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

    private static final Map<String, String> FIELD_DESCRIPTIONS = IssueFieldsMapping.load();

    /**
     * Известные секционные заголовки Jira Wiki Markup, которые авторы пишут как
     * {@code *Шаги*}, {@code *Ожидаемый результат*} и т.д. прямо в строке описания
     * (без предшествующего переноса).  При конвертации они становятся {@code ### Заголовок}.
     */
    private static final List<String> SECTION_HEADERS = List.of(
            "Шаги",
            "Ожидаемый результат",
            "Фактический результат",
            "Дополнительная информация",
            "Precondition",
            "Предусловие"
    );

    /**
     * Конвертирует сырой JSON задачи Jira Issue в Markdown-строку со всеми полями.
     */
    @SuppressWarnings("unchecked")
    public static String toMarkdown(String issueJson) {
        Map<String, Object> root = parseRoot(issueJson);
        String key = str(root.get("key"));
        Map<String, Object> fields = (Map<String, Object>) root.getOrDefault("fields", Collections.emptyMap());

        StringBuilder md = new StringBuilder();
        md.append("# ").append(key).append(" — ").append(str(fields.get("summary"))).append("\n\n");

        md.append("## Детали задачи\n\n");
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("issuetype", "Тип"), nestedName(fields.get("issuetype")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("status", "Статус"), nestedName(fields.get("status")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("priority", "Приоритет"), nestedName(fields.get("priority")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("resolution", "Решение"), nestedName(fields.get("resolution")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("assignee", "Исполнитель"), nestedDisplayName(fields.get("assignee")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("reporter", "Автор задачи"), nestedDisplayName(fields.get("reporter")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("creator", "Создатель задачи"), nestedDisplayName(fields.get("creator")));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("project", "Проект"), nestedName(fields.get("project")));

        Object labelsObj = fields.get("labels");
        if (labelsObj instanceof List<?> labels && !labels.isEmpty()) {
            md.append("- **").append(FIELD_DESCRIPTIONS.getOrDefault("labels", "Метки")).append("**: ")
                    .append(String.join(", ", (List<String>) labels)).append("\n");
        }
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("versions", "Затронутые версии"),
                (List<Map<String, Object>>) fields.get("versions"));
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("fixVersions", "Исправить в версиях"),
                (List<Map<String, Object>>) fields.get("fixVersions"));
        appendVersionList(md, FIELD_DESCRIPTIONS.getOrDefault("components", "Компоненты"),
                (List<Map<String, Object>>) fields.get("components"));

        md.append("\n## Даты\n\n");
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("created", "Дата создания"), formatDate(str(fields.get("created"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("updated", "Дата последнего обновления"), formatDate(str(fields.get("updated"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("resolutiondate", "Дата решения"), formatDate(str(fields.get("resolutiondate"))));
        appendField(md, FIELD_DESCRIPTIONS.getOrDefault("duedate", "Срок выполнения"), formatDate(str(fields.get("duedate"))));

        md.append("\n## Дополнительные поля\n\n");
        for (Map.Entry<String, String> entry : FIELD_DESCRIPTIONS.entrySet()) {
            String fieldKey = entry.getKey();
            if (!fieldKey.startsWith("customfield_") || !fields.containsKey(fieldKey)) continue;
            String readable = extractCustomValue(fields.get(fieldKey));
            if (readable != null && !readable.isBlank()) {
                appendField(md, entry.getValue(), readable);
            }
        }

        appendDescription(md, fields);

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

        Object attachObj = fields.get("attachment");
        if (attachObj instanceof List<?> attachments && !attachments.isEmpty()) {
            md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("attachment", "Вложения")).append("\n\n");
            for (Object a : attachments) {
                Map<String, Object> att = (Map<String, Object>) a;
                md.append("- ").append(str(att.get("filename")))
                        .append(" (id=").append(str(att.get("id"))).append(")\n");
            }
        }

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

    /**
     * Краткая версия: только заголовок (ключ — summary) и раздел «Описание».
     */
    @SuppressWarnings("unchecked")
    public static String toMarkdownShort(String issueJson) {
        Map<String, Object> root = parseRoot(issueJson);
        String key = str(root.get("key"));
        Map<String, Object> fields = (Map<String, Object>) root.getOrDefault("fields", Collections.emptyMap());

        StringBuilder md = new StringBuilder();
        md.append("# ").append(key).append(" — ").append(str(fields.get("summary"))).append("\n\n");
        appendDescription(md, fields);
        return md.toString();
    }

    // ---- private helpers ----

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseRoot(String issueJson) {
        try {
            return JiraUtils.MAPPER.readValue(issueJson, LinkedHashMap.class);
        } catch (Exception e) {
            return ThrowableUtils.reThrow(e);
        }
    }

    private static void appendDescription(StringBuilder md, Map<String, Object> fields) {
        Object descObj = fields.get("description");
        if (descObj != null) {
            md.append("\n## ").append(FIELD_DESCRIPTIONS.getOrDefault("description", "Описание")).append("\n\n");
            md.append(jiraWikiToMarkdown(str(descObj))).append("\n");
        }
    }

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

    @SuppressWarnings("unchecked")
    private static String extractCustomValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map<?, ?> map) {
            Object val = ((Map<String, Object>) map).get("value");
            if (val != null) return str(val);
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
     * Конвертер Jira Wiki Markup → Markdown.
     *
     * <h3>Порядок замен критичен — менять с осторожностью.</h3>
     *
     * <p><b>Шаг 0 — нормализация.</b><br>
     * 0a. Если {@code \n} пришли двойно экранированными ({@code \\n}) — разворачиваем.<br>
     * 0b. Вставляем {@code \n\n} перед известными секционными заголовками вида
     *     {@code *Шаги*}, {@code *Ожидаемый результат*} и т.д., когда они встречаются
     *     в середине строки (Jira хранит description без переносов строк между секциями).<br>
     * 0c. Вставляем {@code \n} перед каждым элементом списка {@code # } и {@code * },
     *     идущим в одну строку без переноса.<br>
     * 0d. У каждой строки обрезаем ведущие пробелы если строка начинается с маркера
     *     списка ({@code # ...}, {@code * ...}) или заголовка {@code h1.}–{@code h6.}.
     *     Используем явную проверку через {@code startsWith} вместо regex, чтобы гарантированно
     *     убирать пробел перед {@code #} даже при наличии нескольких пробелов.
     *
     * <p><b>Шаг 0.5 — Jira-заголовки {@code h1.}–{@code h6.} → Markdown {@code ##}…{@code ######}.</b>
     *
     * <p><b>Шаг 1 — Jira-нумерованные списки {@code #} / {@code ##} / {@code ###} → {@code 1.}.</b><br>
     * Маркеры идут до inline-замен, иначе {@code #} останется в строке и Markdown
     * рендерер считает его заголовком H1.
     *
     * <p><b>Шаг 2 — ненумерованные списки {@code * } → {@code - }.</b>
     *
     * <p><b>Шаг 3 — секционные bold-заголовки на отдельной строке → {@code ### Заголовок}.</b><br>
     * Строка вида {@code *Шаги*} (весь контент — bold) становится Markdown-заголовком {@code ### Шаги}.
     * Выполняется до общего inline-bold, иначе они превратятся в {@code **Шаги**}.
     *
     * <p><b>Шаг 4 — inline-разметка.</b><br>
     * Bold, italic, {color}, изображения, ссылки.
     */
    static String jiraWikiToMarkdown(String text) {
        if (text == null) return "";

        // --- Шаг 0a: разворачиваем double-escaped \\n ---
        text = text.replace("\\n", "\n").replace("\\r", "");

        // --- Шаг 0b: вставляем переносы перед секционными заголовками ---
        // Паттерн: пробел + *Заголовок* + (пробел или конец строки)
        for (String header : SECTION_HEADERS) {
            text = text.replaceAll(
                    " \\*(" + header + ")\\*(?= |$)",
                    "\n\n*$1*\n"
            );
        }

        // --- Шаг 0c: вставляем \n перед элементами списков идущих в одну строку ---
        // "... текст # пункт" → "... текст\n# пункт"
        text = text.replaceAll("(?<=[^\n]) # ", "\n# ");
        // "... ; * пункт" → "... ;\n* пункт"  (только перед кириллицей/латиницей/кавычками)
        text = text.replaceAll(" \\* (?=[\\p{Lu}\\p{L}«\"—\\-])", "\n* ");

        // --- Шаг 0d: убираем ведущие пробелы у строк-маркеров ---
        // Используем startsWith вместо regex — надёжно работает с " # текст", "  ## текст" и т.д.
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String stripped = line.stripLeading();
            boolean isMarker = stripped.startsWith("#")
                    || stripped.startsWith("* ")
                    || stripped.startsWith("** ")
                    || stripped.startsWith("*** ")
                    || stripped.matches("^h[1-6]\\..*");
            sb.append(isMarker ? stripped : line.stripTrailing());
            if (i < lines.length - 1) sb.append('\n');
        }
        text = sb.toString();

        // --- Шаг 0.5: Jira-заголовки h1.–h6. → Markdown ## … ###### ---
        text = text
                .replaceAll("(?m)^h1\\.\\s*", "## ")
                .replaceAll("(?m)^h2\\.\\s*", "### ")
                .replaceAll("(?m)^h3\\.\\s*", "#### ")
                .replaceAll("(?m)^h4\\.\\s*", "##### ")
                .replaceAll("(?m)^h5\\.\\s*", "###### ")
                .replaceAll("(?m)^h6\\.\\s*", "###### ");

        // --- Шаг 1: Jira-нумерованные списки (#, ##, ###) → Markdown ---
        text = text
                .replaceAll("(?m)^###\\s*", "      1. ")
                .replaceAll("(?m)^##\\s*",  "   1. ")
                .replaceAll("(?m)^#\\s*",   "1. ");

        // --- Шаг 2: Jira-ненумерованные списки (* текст) → Markdown ---
        text = text
                .replaceAll("(?m)^\\*\\*\\* ", "      - ")
                .replaceAll("(?m)^\\*\\* ",    "   - ")
                .replaceAll("(?m)^\\* ",       "- ");

        // --- Шаг 3: секционные заголовки *Текст* на отдельной строке → ### Текст ---
        // Строка целиком состоит из *Текст* (без других символов вокруг)
        text = text.replaceAll("(?m)^\\*([^*\n]+)\\*\\s*$", "### $1");

        // --- Шаг 4: inline-разметка ---
        text = text
                .replaceAll("\\*([^*\n]+)\\*",             "**$1**")
                .replaceAll("(?<![*_])_([^_\n]+)_(?![*_])", "*$1*")
                .replaceAll("\\{color:[^}]+}(.*?)\\{color}", "$1")
                .replaceAll("!([^|!\n]+)\\|thumbnail!",     "![вложение]($1)")
                .replaceAll("\\[([^|\\]]+)\\|([^\\]]+)\\]", "[$1]($2)")
                .replaceAll("(?m)^----+\\s*$",              "---");

        return text;
    }

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
