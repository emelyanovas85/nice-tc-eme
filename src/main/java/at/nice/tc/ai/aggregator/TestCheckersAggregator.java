package at.nice.tc.ai.aggregator;

import at.nice.tc.ai.client.ChatClientTestChecker;
import at.nice.tc.ai.dto.jira.DTOTestWithNested;
import at.nice.tc.events.ToolEvent;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.Attachment;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class TestCheckersAggregator {

    private final ChatClientTestChecker chatClientTestChecker;
    private final JiraService jiraService;
    private final ObjectMapper objectMapper;

    private final List<String> jiraFields = List.of(
            "id",
            "key",
            "majorVersion",
            "testScript.steps.index",
            "testScript.steps.testCase.id"//,
//            "testScript.steps.testCase.key",
//            "testScript.steps.testCase.majorVersion"
    );

    /**
     * Запускает проверку теста с учетом вложенных по ключу
     */
    public CompletableFuture<List<CompletableFuture<String>>> checkTestCase(String keyTestCase, ToolEventPublisher publisher) {
        Map<String, CompletableFuture<TestTree.Test>> cache = new ConcurrentHashMap<>();
        return collectNestedTests(keyTestCase, publisher, cache)
                .thenApply(testTree -> {
                    publisher.publish(conversationId -> new CheckEvent.AgentBuiltTestTreeEvent(conversationId, testTree));
                    return testTree.getDescendants();
                })
                .thenApply(testsList -> testsList.stream()
                        .map(test -> chatClientTestChecker.checkTestCase(test, publisher))
                        .collect(toList())
                );
    }

    /**
     * Рекурсивно проходит по тестам и строит дерево тестов
     * Использует кеш для предотвращения повторной загрузки одного и того же теста
     */
    private CompletableFuture<TestTree.Test> collectNestedTests(String testId, ToolEventPublisher publisher, Map<String, CompletableFuture<TestTree.Test>> cache) {
        // Проверяем кеш перед загрузкой
        CompletableFuture<TestTree.Test> cachedFuture = cache.get(testId);
        if (cachedFuture != null) {
            return cachedFuture;
        }

        // Создаем новую задачу для загрузки теста и сразу добавляем в кеш,
        // чтобы предотвратить повторные запросы, если тот же тест запрашивается параллельно
        CompletableFuture<TestTree.Test> future = createFutureForTest(testId, publisher, cache);
        cache.put(testId, future);
        
        return future;
    }

    private CompletableFuture<TestTree.Test> createFutureForTest(String testId, ToolEventPublisher publisher, Map<String, CompletableFuture<TestTree.Test>> cache) {
        publisher.publish(cId -> new ToolEvent("Получение тестов, вложенных в тест " + testId));
        return jiraService.getTest(testId, jiraFields)
                .thenCompose(json -> {
                    DTOTestWithNested dto = parseJsonToDTOTestsList(json);
                    TestTree.Test test = new TestTree.Test(dto.id(), dto.key(), dto.majorVersion());

                    publisher.publish(cId ->
                            new ToolEvent("Получение тестов, вложенных в тест " + test.getId() + ", завершено",
                                    new Attachment.Text(test + ".json", json)
                            )
                    );

                    List<CompletableFuture<Void>> futures = dto.testScript().stepByStepScript().steps()
                            .stream()
                            .filter(Objects::nonNull)
                            .map(DTOTestWithNested.StepDTO::testCase)
                            .filter(Objects::nonNull)
                            .map(DTOTestWithNested.TestCaseDTO::id)
                            .map(String::valueOf)
                            .map(id -> collectNestedTests(id, publisher, cache).thenAccept(test::addTest))
                            .toList();

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(done -> test);
                });
    }

    private DTOTestWithNested parseJsonToDTOTestsList(String json) {
        try {
            return objectMapper.readValue(json, DTOTestWithNested.class);
        } catch (Exception e) {
            return ThrowableUtils.reThrow(e);
        }
    }
}
