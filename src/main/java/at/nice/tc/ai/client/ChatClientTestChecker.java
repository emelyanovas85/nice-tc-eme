package at.nice.tc.ai.client;

import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.AiService;
import at.nice.tc.service.JiraService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.utils.JiraUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static at.nice.tc.utils.JiraUtils.toMarkdown;
import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public record ChatClientTestChecker(AiService aiService,
                                    JiraService jiraService,
                                    MemoryService memoryService) {

    public CompletableFuture<String> checkTestCase(TestTree.Test test, ToolEventPublisher publisher, boolean isMainTest) {
        log.debug("🚀 START checkTestCase: testId={}, name={}", test.getId(), test);
        List<String> conversationIds = generateConversationIds(test, publisher);

        return checkAllStages(test, publisher, conversationIds)
                .thenCompose(cachedResult -> {
//                    if (cachedResult != null) {
//                        log.debug("✅ Найден результат в кэше для {}", test.getId());
//                        return CompletableFuture.completedFuture(cachedResult);
//                    }

                    // Очищаем память и запускаем новые промпты
                    conversationIds.forEach(memoryService::clearMessages);

                    return getTestAsFuture(test, publisher)
                            .thenCompose(testData -> runAllPromptsParallel(conversationIds, testData, test, isMainTest, publisher));
                })
                .exceptionally(t -> {
                    log.error("💥 checkTestCase полностью упал для {}: {}", test.getId(), t.getMessage(), t);
                    return "";
                });
    }


    @NotNull
    private CompletableFuture<Map<String, Object>> getTestAsFuture(TestTree.Test test, ToolEventPublisher publisher) {
        final String testId = String.valueOf(test.getId());
        log.debug("📥 Загружаем тест из Jira: ID={}", testId);

        // ✅ 2 попытки Jira + fallback
        CompletableFuture<String> htmlContentTest = tryJiraTwice(testId, jiraService::getTest);
        CompletableFuture<String> htmlContentExecutionTest = tryJiraTwice(testId, jiraService::getTestExecutions);



        CompletableFuture<Map<String, Object>> futureContentTest = htmlContentTest
                .thenApply(html -> html.isEmpty() ? "" : html)
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .exceptionally(t -> {
                    log.error("💥 getTestAsFuture FAILED для {}: {}", testId, t.getMessage(), t);
                    publisher.publish(new CheckEvent.CheckFinishedEvent(publisher.conversationId(), test, t));
                    return Map.of("name", test.toString(), "error", "Jira unavailable: " + t.getMessage());
                });

        CompletableFuture<Map<String, Object>> futureContentExecutionTest = htmlContentExecutionTest
                .thenApply(html -> html.isEmpty() ? "" : html)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .exceptionally(t -> {
                    log.error("💥 getTestExecutionAsFuture FAILED для {}: {}", testId, t.getMessage(), t);
                    publisher.publish(new CheckEvent.CheckFinishedEvent(publisher.conversationId(), test, t));
                    return Map.of("name", test.toString(), "error", "Jira unavailable: " + t.getMessage());
                });


        return futureContentTest.thenCombine(futureContentExecutionTest, (testMap, execMap) -> {
            Map<String, Object> result = new HashMap<>(testMap);
            result.putAll(execMap);
            return result;
        });
    }

    private <T> CompletableFuture<String> tryJiraTwice(T testId, Function<T, CompletableFuture<String>> jiraMethod) {
        // Первая попытка
        CompletableFuture<String> firstAttempt = jiraMethod.apply(testId)
                .thenApply(resp -> {
                    log.debug("Попытка 1 получения данных из Jira для {}: длина={}", testId, resp != null ? resp.length() : 0);
                    return extractHtml(resp);
                });

        // Вторая попытка если первая провалилась
        return firstAttempt.thenCompose(firstResult ->
                firstResult != null && !firstResult.isEmpty() ?
                        CompletableFuture.completedFuture(firstResult) :
                        jiraMethod.apply(testId)
                                .thenApply(resp -> {
                                    log.debug("Попытка 2 получения данных из Jira для {}: длина={}", testId, resp != null ? resp.length() : 0);
                                    String result = extractHtml(resp);
                                    return result != null ? result : "";
                                })
        );
    }

    private String extractHtml(String response) {
        if (response == null) return null;

        // Проверяем HTML
        if (response.startsWith("<!DOCTYPE html>") || response.startsWith("<html ")) {
            log.warn("Получен HTML вместо JSON");
            return null;
        }

        return response;
    }

    private CompletableFuture<String> checkAllStages(TestTree.Test test, ToolEventPublisher publisher, List<String> convIds) {
        List<CompletableFuture<String>> checks = convIds.stream()
                .map(convId -> checkStage(convId, test, publisher).exceptionally(t -> null))
                .toList();

        if (checks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.anyOf(checks.toArray(new CompletableFuture[0]))
                .thenApply(v -> checks.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    private CompletableFuture<String> checkStage(String convId, TestTree.Test test, ToolEventPublisher publisher) {
        if (!memoryService.hasInMemory(convId)) {
            return CompletableFuture.completedFuture(null);
        }

        List<Message> messages = memoryService.getCompletedMessages(convId);
        if (messages.size() > 1) {
            return CompletableFuture.completedFuture(messages.get(1).getText());
        }

        if (memoryService.hasActiveStream(convId)) {
            return sendMessage(() -> memoryService.subscribe(convId), test, publisher, convId);
        }

        return CompletableFuture.completedFuture(null);
    }


    private CompletableFuture<String> sendMessage(Supplier<Flux<String>> messageStreamSupplier,
                                                  TestTree.Test test, ToolEventPublisher publisher,
                                                  String conversationId) {
        Flux<String> flux = messageStreamSupplier.get();
        if (flux == null) {
            publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null));
            return CompletableFuture.completedFuture("");
        }

        return flux
                .doOnSubscribe(s -> log.debug("▶️ Подписка: {}", conversationId))
                .doOnNext(chunk -> log.debug("📥 Chunk: {} {} (длина={})", chunk, conversationId, chunk.length()))
                .doOnError(e -> {
                    log.error("💥 Flux error для {}: {}", conversationId, e.getMessage());
                    publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, e));
                })
                // ✅ ГАРАНТИРОВАННОЕ событие при ЛЮБОМ завершении
                .doFinally(signalType -> {
                    log.debug("🏁 sendMessage завершён ({}) для {}", signalType, conversationId);
                    publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null));
                })
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .onErrorReturn("")  // Защита от ошибок reduce/map
                .toFuture()
                .orTimeout(10, TimeUnit.MINUTES)  // Быстрее!
                .exceptionally(t -> {
                    log.error("💥 sendMessage exceptionally для {}: {}", conversationId, t.getMessage());
                    publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, t));
                    return "";
                });
    }

    private CompletableFuture<String> runAllPromptsParallel(List<String> conversationIds,
                                                            Map<String, Object> testData,
                                                            TestTree.Test test,
                                                            boolean isMainTest,
                                                            ToolEventPublisher publisher) {
        List<String> prompts = Prompts.REQUIREMENTS;
        List<CompletableFuture<String>> futures = new ArrayList<>(prompts.size());

        for (int i = 0; i < prompts.size(); i++) {
            String convId = conversationIds.get(i);
            publisher.publish(new CheckEvent.CheckPromptStartedEvent(convId, test, i));

            String fullPrompt = String.format("%s\n\n### **%s**\n\n%s",
                    prompts.get(i),
                    isMainTest ? "Верхнеуровневый (основной) тест-кейс:" : "Представленный ниже тест-кейс является вложенным, а не верхнеуровневым (основным):",
                    toMarkdown(testData));


            int finalI = i;

            futures.add(sendMessage(
                    () -> {
                        Flux<String> flux = aiService.sendAgentMessageStream(fullPrompt, convId);
                        return flux != null ? flux : Flux.empty();
                    },
                    test, publisher, convId
            ).exceptionally(t -> {
                log.error("💥 Промпт {} для {} упал: {}", finalI, test.getId(), t.getMessage());
                publisher.publish(new CheckEvent.CheckFinishedEvent(convId, test, t));
                return "";
            }));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(10, TimeUnit.MINUTES)
                .handle((v, t) -> {
                    if (t != null) {
                        log.warn("⏰ runAllPromptsParallel TIMEOUT для {}: {}", test.getId(), t.getMessage());
                        return "TIMEOUT: не все промпты завершились";
                    }
                    return futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(joining(","));
                });
    }


    private List<String> generateConversationIds(TestTree.Test test, ToolEventPublisher publisher) {
        return IntStream.range(0, Prompts.REQUIREMENTS.size())
                .mapToObj(i -> publisher.conversationId() + "_" + test.getId() + "_" + i + "_prompt")
                .toList();
    }
}
