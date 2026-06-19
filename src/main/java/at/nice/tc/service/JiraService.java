package at.nice.tc.service;

import at.nice.tc.ai.tools.jiraTool.Jira;
import at.nice.tc.ai.tools.jiraTool.JiraImpl;
import at.nice.tc.utils.IssueMarkdownUtils;
import at.nice.tc.utils.JiraUtils;
import at.nice.tc.utils.ThrowableUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class JiraService {
    private final Jira jira;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JiraService(@Lazy Jira jira) {
        this.jira = jira;
    }

    public CompletableFuture<String> getTest(String id) {
        return CompletableFuture.supplyAsync(() -> jira.getTest(id, getRequiredTestProperties()));
    }

    public CompletableFuture<String> getTestWithNestedMarkdown(String id) {
        return getTest(id)
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .thenApplyAsync(test -> {
                    Set<String> nestedTestIds = JiraUtils.getNestedTestIds(test);
                    Map<String, Map<String, Object>> nestedId$nestedTest = new LinkedHashMap<>();
                    while (!nestedTestIds.isEmpty()) {
                        nestedTestIds.removeIf(nestedId$nestedTest.keySet()::contains);
                        Map<String, CompletableFuture<Map<String, Object>>> nestedTests = nestedTestIds.stream()
                                .collect(Collectors.toMap(nestedId -> nestedId, nestedId -> getTest(nestedId)
                                        .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                                        .thenApplyAsync(JiraUtils::parseTreeMapJson)
                                        .thenApplyAsync(JiraUtils::sortSteps)));
                        CompletableFuture.allOf(nestedTests.values().toArray(new CompletableFuture[0])).join();

                        nestedTests.forEach((k, futureV) -> nestedId$nestedTest.put(k, futureV.join()));

                        nestedTestIds.clear();
                        nestedTests.values().stream().map(CompletableFuture::join)
                                .map(JiraUtils::getNestedTestIds)
                                .forEach(nestedTestIds::addAll);
                    }

                    //region добавление вкладки Выполнение
                    List<Map<String, Object>> tests = new ArrayList<>();
                    tests.add(test);
                    tests.addAll(nestedId$nestedTest.values());

                    List<CompletableFuture<?>> getExecutionTasks = new ArrayList<>();

                    tests.forEach(t -> getExecutionTasks.add(
                            getTestExecutions((int) t.get("id"), getRequiredExecutionsProperties())
                                    .thenApply(JiraUtils::simplifyHtmlVariables)
                                    .thenApply(JiraUtils::parseTreeMapJson)
                                    .thenApply(testResults -> test.put("testResults", testResults))
                    ));
                    CompletableFuture.allOf(getExecutionTasks.toArray(new CompletableFuture[0])).join();
                    //endregion

                    return JiraUtils.toMarkdown(test, nestedId$nestedTest);
                });
    }

    public CompletableFuture<String> getTest(String id, List<String> fields) {
        return CompletableFuture.supplyAsync(() -> jira.getTest(id, fields));
    }

    public CompletableFuture<String> getLastUpdate(String id) {
        return CompletableFuture.supplyAsync(() -> jira.getLastUpdate(id));
    }

    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(jira::isAvailable);
    }

    public CompletableFuture<String> getAllVersionsAsync(String testKey) {
        return CompletableFuture.supplyAsync(() -> jira.getAllVersions(testKey));
    }

    /**
     * Получает задачу Jira Issue по ключу и конвертирует её в Markdown (все поля).
     * Запрашивает только поля, описанные в issue_fields_description.json.
     *
     * @param issueKey ключ задачи, например VPEPVV-1123
     * @return Markdown-строка с подробным описанием задачи
     */
    public CompletableFuture<String> getIssueMarkdown(String issueKey) {
        return CompletableFuture.supplyAsync(() -> jira.getIssue(issueKey, issueFields()))
                .thenApply(IssueMarkdownUtils::toMarkdown);
    }

    /**
     * Получает задачу Jira Issue по ключу и конвертирует её в краткий Markdown:
     * только заголовок (ключ — summary) и раздел «Описание».
     *
     * @param issueKey ключ задачи, например VPEPVV-1123
     * @return Markdown-строка с заголовком и описанием задачи
     */
    public CompletableFuture<String> getIssueMarkdownShort(String issueKey) {
        // Для краткой версии достаточно только summary + description
        List<String> fields = List.of("summary", "description");
        return CompletableFuture.supplyAsync(() -> jira.getIssue(issueKey, fields))
                .thenApply(IssueMarkdownUtils::toMarkdownShort);
    }

    /**
     * Возвращает уникальные id issues (int), связанных с тестом через issueLinks,
     * отфильтрованных по следующим критериям:
     * <ul>
     *   <li>тип задачи: {@code issuetype.name == "Дефект"}</li>
     *   <li>статус НЕ равен {@code "закрыт (fixed)"} и НЕ равен {@code "отменен (canceled)"}</li>
     * </ul>
     *
     * @param testIdOrKey числовой id версии теста (только цифры) или ключ теста (содержит нецифровые символы)
     * @return CompletableFuture со списком уникальных числовых id issues-дефектов
     */
    public CompletableFuture<List<Integer>> getDefectIssueIds(String testIdOrKey) {
        // Если передан ключ (содержит нецифровые символы) — сначала получаем числовой id через getTest
        CompletableFuture<Integer> versionIdFuture;
        if (testIdOrKey.trim().matches("\\d+")) {
            versionIdFuture = CompletableFuture.completedFuture(Integer.parseInt(testIdOrKey.trim()));
        } else {
            versionIdFuture = getTest(testIdOrKey, List.of("id"))
                    .thenApplyAsync(json -> {
                        try {
                            Map<String, Object> testMap = objectMapper.readValue(json, new TypeReference<>() {});
                            return ((Number) testMap.get("id")).intValue();
                        } catch (Exception e) {
                            return ThrowableUtils.reThrow(e);
                        }
                    });
        }

        return versionIdFuture
                .thenComposeAsync(versionId ->
                        getTestExecutions(versionId, List.of("issueLinks"))
                                .thenApplyAsync(json -> {
                                    try {
                                        Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {});
                                        List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
                                        if (data == null) return Collections.<Integer>emptyList();

                                        // Собираем уникальные issueId из всех issueLinks
                                        return data.stream()
                                                .map(entry -> (List<Map<String, Object>>) entry.get("issueLinks"))
                                                .filter(Objects::nonNull)
                                                .flatMap(Collection::stream)
                                                .map(link -> link.get("issueId"))
                                                .filter(Objects::nonNull)
                                                .map(id -> Integer.parseInt(id.toString()))
                                                .distinct()
                                                .collect(Collectors.toList());
                                    } catch (Exception e) {
                                        return ThrowableUtils.reThrow(e);
                                    }
                                })
                )
                .thenComposeAsync(issueIds -> {
                    // Асинхронно запрашиваем каждый issue и фильтруем
                    List<CompletableFuture<Optional<Integer>>> futures = issueIds.stream()
                            .map(issueId -> CompletableFuture.supplyAsync(() -> jira.getIssue(String.valueOf(issueId), null))
                                    .thenApplyAsync(json -> {
                                        try {
                                            Map<String, Object> issue = objectMapper.readValue(json, new TypeReference<>() {});
                                            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                                            if (fields == null) return Optional.<Integer>empty();

                                            // Проверяем тип: issuetype.name == "Дефект"
                                            Map<String, Object> issueType = (Map<String, Object>) fields.get("issuetype");
                                            if (issueType == null) return Optional.<Integer>empty();
                                            String typeName = String.valueOf(issueType.getOrDefault("name", ""));
                                            if (!"дефект".equals(typeName.toLowerCase())) return Optional.<Integer>empty();

                                            // Проверяем статус: исключаем "закрыт (fixed)" и "отменен (canceled)"
                                            Map<String, Object> status = (Map<String, Object>) fields.get("status");
                                            if (status != null) {
                                                String statusName = String.valueOf(status.getOrDefault("name", "")).toLowerCase();
                                                if (statusName.equals("закрыт (fixed)") || statusName.equals("отменен (canceled)")) {
                                                    return Optional.<Integer>empty();
                                                }
                                            }

                                            return Optional.of(issueId);
                                        } catch (Exception e) {
                                            return ThrowableUtils.reThrow(e);
                                        }
                                    })
                            )
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApplyAsync(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList()));
                });
    }

    @Cacheable
    public List<String> getRequiredTestProperties() {
        return readLines("testcase_required_fields.txt");
    }

    @Cacheable
    public List<String> getRequiredExecutionsProperties() {
        return readLines("executions_required_fields.txt");
    }

    public List<String> getAvailableTestProperties() throws IOException {
        return readLines("testcase_fields_description.json");
    }

    public List<String> getAvailableTestExecutionProperties() throws IOException {
        return readLines("availableTestExecutionProperties.txt");
    }

    public CompletableFuture<String> getTestExecutions(int versionId, List<String> fields) {
        return CompletableFuture.supplyAsync(() -> jira.getTestExecutions(versionId, fields));
    }

    /**
     * Список полей issue для полного запроса — все поля из issue_fields_description.json.
     */
    private static List<String> issueFields() {
        return List.of(
                "summary", "description", "issuetype", "status", "priority",
                "resolution", "assignee", "reporter", "creator", "project",
                "labels", "versions", "fixVersions", "components",
                "comment", "issuelinks", "attachment",
                "created", "updated", "resolutiondate", "duedate",
                "customfield_10700", "customfield_10909", "customfield_10912",
                "customfield_10913", "customfield_11808", "customfield_12300",
                "customfield_12304", "customfield_12607", "customfield_12608",
                "customfield_12614", "customfield_14700", "customfield_15702",
                "customfield_20705", "customfield_21703", "customfield_10006",
                "customfield_12602", "customfield_13803", "customfield_13804",
                "customfield_13805", "customfield_11600"
        );
    }

    /**
     * Читает файл из classpath через InputStream — работает как в файловой системе,
     * так и внутри JAR-архива (в отличие от resource.getFile()).
     */
    private List<String> readLines(String resourceName) {
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.startsWith("/"))
                    .toList();
        } catch (IOException e) {
            return ThrowableUtils.reThrow(e);
        }
    }
}
