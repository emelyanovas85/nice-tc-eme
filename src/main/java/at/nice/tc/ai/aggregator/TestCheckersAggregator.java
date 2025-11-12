package at.nice.tc.ai.aggregator;

import at.nice.tc.ai.client.ChatClientTestChecker;
import at.nice.tc.ai.dto.jira.DTOTestWithNested;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
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
            "testScript.steps.testCase.key",
            "testScript.steps.testCase.id",
            "testScript.steps.index",
            "testScript.steps.testCase.majorVersion");

    /**
     * Запускает проверку теста с учетом вложенных по ключу
     */
    public CompletableFuture<List<CompletableFuture<String>>> checkTestCase(String keyTestCase) {
        return this.testChain(keyTestCase)
                .thenApply(testsList ->
                        testsList.stream()
                                .map(DTOTestWithNested::id)
                                .map(testKey -> supplyAsync(() -> chatClientTestChecker.checkTestCase(testKey)))
                                .collect(toList()));
    }

    /**
     * Собирает цепочку тестов рекурсивно по ключу/ID,
     */
    public CompletableFuture<List<DTOTestWithNested>> testChain(String keyOrId) {
        Set<Integer> allIds = ConcurrentHashMap.newKeySet();
        Set<Integer> processedIds = ConcurrentHashMap.newKeySet();

        return collectNestedTests(keyOrId, allIds, processedIds)
                .thenCompose(ignored -> sequence(
                        allIds.stream()
                                .map(id -> jiraService.getTest(String.valueOf(id), jiraFields)
                                        .thenApply(this::parseJsonToDTOTestsList))
                                .collect(toList())
                ));
    }

    /**
     * Рекурсивно собирает ID вложенных тестов в allIds с учетом уже обработанных.
     */
    private CompletableFuture<Void> collectNestedTests(String keyTestCase, Set<Integer> allIds, Set<Integer> processedIds) {
        return jiraService.getTest(keyTestCase, jiraFields)
                .thenCompose(json -> {
                    DTOTestWithNested dto = parseJsonToDTOTestsList(json);
                    Integer currentId = dto.id();

                    chatClientTestChecker.addIdKeyMapping(currentId, keyTestCase);

                    if (processedIds.contains(currentId)) return completedFuture(null);

                    processedIds.add(currentId);

                    List<CompletableFuture<Void>> futures = dto.testScript().stepByStepScript().steps()
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(step -> nonNull(step.testCase()))
                            .map(step -> collectNestedTests(step.testCase().key(), allIds, processedIds))
                            .toList();

                    allIds.add(currentId);

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                });
    }

    /**
     * Задаёт вопрос чат-клиенту по ключу теста.
     */
    public CompletableFuture<String> askQuestionByKey(String keyTestCase, String question) {
        return supplyAsync(() -> chatClientTestChecker.askQuestionByKey(keyTestCase, question));
    }

    private DTOTestWithNested parseJsonToDTOTestsList(String json) {
        try {
            return objectMapper.readValue(json, DTOTestWithNested.class);
        } catch (Exception e) {
            return ThrowableUtils.reThrow(e);
        }
    }

    /**
     * Асинхронно превращает список CompletableFuture<T> в CompletableFuture<List<T>> без блокировок.
     * Это делается последовательным объединением результатов через thenCompose.
     */
    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<List<T>> result = completedFuture(List.of());

        for (CompletableFuture<T> future : futures) {
            result = result.thenCombine(future, (list, elem) -> {
                // Создаем новый список с добавленным элементом, чтобы избежать мутаций
                List<T> newList = List.copyOf(list);
                return new ArrayList<T>(newList) {{
                    add(elem);
                }};
            });
        }

        return result;
    }
}
