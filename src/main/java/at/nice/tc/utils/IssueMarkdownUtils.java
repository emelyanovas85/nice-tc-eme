package at.nice.tc.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Конвертирует JSON-ответ Jira REST API ({@code /rest/api/latest/issue/{key}})
 * в читаемый Markdown-текст.
 * <p>
 * Маппинг ключ → человекочитаемое название берётся из {@link IssueFieldsMapping}.
 */
public abstract class IssueMarkdownUtils {

    /**
     * Маппинг: ключ поля JSON → человекочитаемое описание.
     * Загружается из issue_fields_description.json.
     */
    private static final Map<String, String> FIELD_DESCRIPTIONS = IssueFieldsMapping.load();

    /**
     * Конвертирует сырой JSON задачи Jira Issue в Markdown-строку.
     *
     * @param issueJson JSON-строка от {@code GET /rest/api/latest/issue/{key}}
     * @return текст в формате Markdown
     */
    @SuppressWarnings("unchecked")
    public static String toMarkdown(String issueJson) {
        Map<String, Object> root;
        try {
            root = JiraUtils.MAPPER.readValue(issueJson, LinkedHashMap.class);
        } catch (Exception e) {
            return ThrowableUtils.reThrow(e);
        }

        String key = str(root.get("key"));
        Map<String, Object> fields = (Map<String, Object>) root.getOrDefault("fields", Collections.emptyMap());

        StringBuilder md = new StringBuilder();
        md.append("# ").append(key).append(" — ").append(str(fields.get("summary"))).append("\n\n");

        // --- Детали задачи ---
        md.append("## Детали задачи\n\n");
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("issuetype", "Тип"), nestedName(fields.get("issuetype")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("status", "Статус"), nestedName(fields.get("status")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("priority", "Приоритет"), nestedName(fields.get("priority")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("resolution", "Решение"), nestedName(fields.get("resolution")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("assignee", "Исполнитель"), nestedDisplayName(fields.get("assignee")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("reporter", "Автор задачи"), nestedDisplayName(fields.get("reporter")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("creator", "Создатель задачи"), nestedDisplayName(fields.get("creator")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("project", "Проект"), nestedName(fields.get("project")));

        // Метки
        Object labelsObj = fields.get("labels");
        if (labelsObj instanceof List<?> labels && !labels.isEmpty()) {
            md.append("- **").append(FIELD_DESCRIPTIONS.getOrDefault("labels", "Метки")).append("**: ")
                    .append(String.join(", ", (List<String>) labels)).append("\n");
        }

        // Версии
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("versions", "Затронутые версии"),
                (List<Map<String, Object>>) fields.get("versions"));
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("fixVersions", "Исправить в версиях"),
                (List<Map<String, Object>>) fields.get("fixVersions"));

        // Компоненты
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("components", "Компоненты"),
                (List<Map<String, Object>>) fields.get("components"));

        // Даты
        md.append("\n## Даты\n\n");
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("created", "Дата создания"), formatDate(str(fields.get("created"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("updated", "Дата последнего обновления"), formatDate(str(fields.get("updated"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("resolutiondate", "Дата решения"), formatDate(str(fields.get("resolutiondate"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("duedate", "Срок выполнения"), formatDate(str(fields.get("duedate"))));

        // --- Кастомные поля (только непустые) ---
        md.append("\n## Дополнительные поля\n\n");
        for (Map.Entry<String, String> entry : FIELD_DESCRIPTIONS.entrySet()) {
            String fieldKey = entry.getKey();
            if (!fieldKey.startsWith("customfield_") || !fields.containsKey(fieldKey)) continue;
            String readable = extractCustomValue(fields.get(fieldKey));
            if (readable != null && !readable.isBlank()) {
                appendField(md, entry.getValue(), readable);
            }
        }

        // --- Описание ---
        Object descObj = fields.get("description");
        if (descObj != null) {
            md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("description", "Описание")).append("\n\n");
            md.append(jiraWikiToMarkdown(str(descObj))).append("\n");
        }

        // --- Связанные задачи ---
        Object issueLinksObj = fields.get("issuelinks");
        if (issueLinksObj instanceof List<?> issueLinks && !issueLinks.isEmpty()) {
            md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("issuelinks", "Связанные задачи")).append("\n\n");
            for (Object il : issueLinks) {
                Map<String, Object> link = (Map<String, Object>) il;
                Map<String, Object> linkType = (Map<String, Object>) link.get("type");
                String typeName = linkType != null ? str(linkType.get("outward")) : "";
                Map<String, Object> outward = (Map<String, Object>) link.get("outwardIssue");
                Map<String, Object> inward = (Map<String, Object>) link.get("inwardIssue");
                Map<String, Object> linkedIssue = outward != null ? outward : inward;
                if (linkedIssue != null) {
                    String linkedKey = str(linkedIssue.get("key"));
                    String linkedSummary = nestedName((Map<String, Object>) linkedIssue.get("fields"));
                    md.append("- ").append(typeName).append(" **").append(linkedKey).append("**");
                    if (linkedSummary != null) md.append(" — ").append(linkedSummary);
                    md.append("\n");
                }
            }
        }

        // --- Вложения ---
        Object attachObj = fields.get("attachment");
        if (attachObj instanceof List<?> attachments && !attachments.isEmpty()) {
            md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("attachment", "Вложения")).append("\n\n");
            for (Object a : attachments) {
                Map<String, Object> att = (Map<String, Object>) a;
                md.append("- ").append(str(att.get("filename")))
                        .append(" (id=").append(str(att.get("id"))).append(")\n");
            }
        }

        // --- Комментарии ---
        Object commentObj = fields.get("comment");
        if (commentObj instanceof Map<?, ?> commentMap) {
            Object commentsList = commentMap.get("comments");
            if (commentsList instanceof List<?> comments && !comments.isEmpty()) {
                md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("comment", "Комментарии")).append("\n\n");
                for (Object c : comments) {
                    Map<String, Object> comment = (Map<String, Object>) c;
                    String author = nestedDisplayName(comment.get("author"));
                    String created = formatDate(str(comment.get("created")));
                    String body = jiraWikiToMarkdown(str(comment.get("body")));
                    md.append("---\n");
                    md.append("**").append(author).append("** (").append(created).append(")\n\n");
                    md.append(body).append("\n\n");
                }
            }
        }

        return md.toString();
    }

    // ---- helpers ----

    private static void appendField(StringBuilder md, String label, String value) {
        if (value != null && !value.isBlank() && !"null".equals(value)) {
            md.append("- **").append(label).append("**: ").append(value).append("\n");
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendVersionList(StringBuilder md, String label, List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return;
        String joined = list.stream()
                .map(v -> str(v.get("name")))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        if (!joined.isBlank()) appendField(md, label, joined);
    }

    @SuppressWarnings("unchecked")
    private static String nestedName(Object obj) {
        if (obj instanceof Map<?, ?> map) return str(((Map<String, Object>) map).get("name"));
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String nestedDisplayName(Object obj) {
        if (obj instanceof Map<?, ?> map) return str(((Map<String, Object>) map).get("displayName"));
        return null;
    }

    /**
     * Универсальное извлечение текстового значения из кастомного поля:
     * - одиночный объект  {"value": "..."}  → значение
     * - массив объектов   [{"value": "..."}] → значения через запятую
     * - строка/число/bool → toString
     */
    @SuppressWarnings("unchecked")
    private static String extractCustomValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map<?, ?> map) {
            Object val = ((Map<String, Object>) map).get("value");
            if (val != null) return str(val);
            // displayName — для полей-пользователей
            Object dn = ((Map<String, Object>) map).get("displayName");
            if (dn != null) return str(dn);
            return null;
        }
        if (obj instanceof List<?> list) {
            if (list.isEmpty()) return null;
            return list.stream()
                    .map(item -> {
                        if (item instanceof Map<?, ?> m) {
                            Object val = ((Map<String, Object>) m).get("value");
                            return val != null ? str(val) : str(((Map<String, Object>) m).get("displayName"));
                        }
                        return str(item);
                    })
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
        }
        return str(obj);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    /**
     * Минимальный конвертер Jira Wiki Markup → Markdown.
     */
    private static String jiraWikiToMarkdown(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\\*([^*\\n]+)\\*", "**$1**")           // bold
                .replaceAll("_([^_\\n]+)_", "*$1*")                   // italic
                .replaceAll("\\{color:[^}]+}(.*?)\\{color}", "$1")    // color tags
                .replaceAll("!([^|!\\n]+)\\|thumbnail!", "![вложение]($1)") // изображения
                .replaceAll("(?m)^# ", "1. ");                         // numbered list
    }

    /**
     * Форматирует ISO-дату из Jira (2026-06-16T14:00:08.000+0300) → 16.06.2026 14:00.
     */
    private static String formatDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return iso.substring(8, 10) + "." + iso.substring(5, 7) + "." + iso.substring(0, 4)
                    + " " + iso.substring(11, 16);
        } catch (Exception e) {
            return iso;
        }
    }
}
