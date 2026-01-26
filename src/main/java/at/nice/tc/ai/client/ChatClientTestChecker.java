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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static at.nice.tc.utils.JiraUtils.toMarkdown;
import static java.util.stream.Collectors.joining;

@Slf4j
@Component
public record ChatClientTestChecker(AiService aiService,
                                    JiraService jiraService,
                                    MemoryService memoryService) {

    private static final String PROMPT_TEMPLATE = "%s\n\n%s";  // prompt + markdownTest


    public CompletableFuture<String> checkTestCase(TestTree.Test test, ToolEventPublisher publisher) {
        log.info("🚀 START checkTestCase: testId={}, name={}", test.getId(), test); // ← ДОБАВЬТЕ!
        List<String> listConversationId = generateConversationIds(test, publisher);

        return checkAllStages(test, publisher, listConversationId)
                .thenCompose(finalResult -> {

                    if (finalResult != null) return CompletableFuture.completedFuture(finalResult);

                    listConversationId.forEach(memoryService::clearMessages);

                    return getTestAsFuture(test, publisher)
                            .thenCompose(testAsMap -> runAllPromptsParallel(listConversationId, testAsMap, test, publisher));
                });
    }


    private CompletableFuture<String> runAllPromptsParallel(
            List<String> conversationIds,
            Map<String, Object> testAsMap,
            TestTree.Test test,
            ToolEventPublisher publisher) {

        final List<String> allPrompts = Prompt.all();
        List<CompletableFuture<String>> promptFutures = new ArrayList<>();

        for (int i = 0; i < allPrompts.size(); i++) {
            String convId = conversationIds.get(i);

            publisher.publish(new CheckEvent.CheckPromptStartedEvent(convId, test, i + 1));

            String fullPrompt = String.format(PROMPT_TEMPLATE, allPrompts.get(i), toMarkdown(testAsMap));

            promptFutures.add(sendMessage(() -> aiService.sendAgentMessageStream(fullPrompt, convId), test, publisher, convId)
                    .orTimeout(10, TimeUnit.MINUTES));
        }

        return CompletableFuture.allOf(promptFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> promptFutures.stream()
                        .map(future -> future.getNow(null))
                        .filter(Objects::nonNull)
                        .collect(joining("\n\n")));
    }


    @NotNull
    private CompletableFuture<Map<String, Object>> getTestAsFuture(TestTree.Test test, ToolEventPublisher publisher) {
        String conversationTestId = publisher.conversationId();

        final String testId = String.valueOf(test.getId());

        return jiraService.getTest(testId)
                .thenCompose(resp1 -> {
                    log.info("Jira 1 попытка для {}: длина={}", testId, resp1 != null ? resp1.length() : 0);
                    return JiraUtils.optionalNotHTML(resp1)
                            .orElseGet(() -> jiraService.getTest(testId)
                                    .thenCompose(resp2 -> {
                                        log.info("Jira 2 попытка для {}: длина={}", testId, resp2 != null ? resp2.length() : 0);
                                        return JiraUtils.optionalNotHTML(resp2)
                                                .orElseThrow(() -> {
                                                    log.error("💥 Jira FAILED для теста {} после 2 попыток!", testId);
                                                    return new RuntimeException("С двух попыток не удалось получить из Jira данные теста %s = %s".formatted(test, testId));
                                                });
                                    }));
                })
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .exceptionally(t -> {
                    log.error("💥 getTestAsFuture FAILED для {}: {}", testId, t.getMessage(), t);
                    publisher.publish(new CheckEvent.CheckFinishedEvent(conversationTestId, test, t));
                    return Map.of("name", test.toString(), "error", "Jira unavailable: " + t.getMessage());
                });
    }


    private CompletableFuture<String> checkAllStages(TestTree.Test test, ToolEventPublisher publisher, List<String> convIds) {
        List<CompletableFuture<String>> checks = convIds.stream().map(checkId -> (checkStage(checkId, test, publisher))).toList();

        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                .thenApply(v -> checks.get(checks.size() - 1).join())
                .thenApply(result -> result != null ? result : null);
    }


    private CompletableFuture<String> checkStage(String convId, TestTree.Test test, ToolEventPublisher publisher) {
        if (memoryService.hasInMemory(convId)) {
            List<Message> msgs = memoryService.getCompletedMessages(convId);
            if (msgs.size() > 1) {
                return CompletableFuture.completedFuture(msgs.get(1).getText());
            } else if (memoryService.hasActiveStream(convId)) {
                return sendMessage(() -> memoryService.subscribe(convId), test, publisher, convId);
            }
        }
        return CompletableFuture.completedFuture(null);
    }


    private CompletableFuture<String> sendMessage(Supplier<Flux<String>> messageStream, TestTree.Test test, ToolEventPublisher publisher, String conversationId) {
        if (messageStream == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Не передан поток сообщений при отправке теста " + test));

        return messageStream.get()
                .doOnError(e -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, e)))
                .doOnComplete(() -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null)))
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .toFuture()
                .orTimeout(10, TimeUnit.MINUTES);
    }


    private List<String> generateConversationIds(TestTree.Test test, ToolEventPublisher publisher) {
        return IntStream.rangeClosed(1, Prompt.all().size())
                .mapToObj(i -> publisher.conversationId() + "_" + test.getId() + "_" + i + "_prompt")
                .toList();
    }
}
