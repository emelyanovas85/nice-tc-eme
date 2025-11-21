package at.nice.tc.service;

import at.nice.tc.ai.tools.jiraTool.Jira;
import at.nice.tc.utils.JiraUtils;
import at.nice.tc.utils.SpringUtils;
import at.nice.tc.utils.ThrowableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Service
public class JiraService {
    private final Jira jira;
    private final SpringUtils springUtils;

    public JiraService(@Lazy Jira jira, SpringUtils springUtils) {
        this.jira = jira;
        this.springUtils = springUtils;
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

    public List<String> getRequiredTestProperties() {
        return springUtils.readResourceLines("testcase_required_fields.txt");
    }

    public List<String> getRequiredExecutionsProperties() {
        return springUtils.readResourceLines("executions_required_fields.txt");
    }

    public List<String> getAvailableTestProperties() {
        return springUtils.readResourceLines("testcase_fields_description.json");
    }

    public List<String> getAvailableTestExecutionProperties() {
        return springUtils.readResourceLines("availableTestExecutionProperties.txt");
    }

    public CompletableFuture<String> getTestExecutions(int versionId, List<String> fields) {
        return CompletableFuture.supplyAsync(() -> jira.getTestExecutions(versionId, fields));
    }
}
