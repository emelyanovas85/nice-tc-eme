package at.nice.tc.ai.client;

import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.AiService;
import at.nice.tc.service.JiraService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.utils.JiraUtils;
import at.nice.tc.utils.ThrowableUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public record ChatClientTestChecker(AiService aiService,
                                    JiraService jiraService,
                                    MemoryService memoryService) {

    public CompletableFuture<String> checkTestCase(TestTree.Test test, ToolEventPublisher publisher) {
        final String conversationId = publisher.conversationId() + "_" + test.getId();

        if (memoryService.hasInMemory(conversationId)) {
            List<Message> completedMessages = memoryService.getCompletedMessages(conversationId);
            if (completedMessages.size() > 1) {
                return CompletableFuture.completedFuture(completedMessages.get(1).getText());

            } else if (memoryService.hasActiveStream(conversationId)) { // сообщение уже отправлено, просто подписываемся на ответ
                return sendMessage(() -> memoryService.subscribe(conversationId), test, publisher);
            }
        }

        // Если нет результатов и нет генерации — сбрасываем память и запускаем проверку теста заново
        memoryService.clearMessages(conversationId);

        publisher.publish(new CheckEvent.CheckPreparingEvent("чтение теста (1/2)...", conversationId, test));

        final String testId = String.valueOf(test.getId());
        CompletableFuture<Map<String, Object>> testAsMapFuture = jiraService.getTest(testId) // попытка 1
                .thenCompose(resp1 -> JiraUtils.optionalNotHTML(resp1).orElseGet(() -> jiraService.getTest(testId) // попытка 2
                        .thenCompose(resp2 -> JiraUtils.optionalNotHTML(resp2).orElseThrow(() ->
                                new RuntimeException("С двух попыток не удалось получить из jira данные теста %s = %s. Вместо json возвращается HTML".formatted(test, testId))))
                ))
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .exceptionally(t -> {
                    publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, t));
                    return ThrowableUtils.reThrow(t);
                });

        return testAsMapFuture.thenCompose(testAsMap -> {
            publisher.publish(new CheckEvent.CheckPreparingEvent("парсинг теста (2/2)...", conversationId, test));

            String markdownTest = JiraUtils.toMarkdown(testAsMap);
            String prompt = String.join("\n\n", Prompt.get(), markdownTest);

            return sendMessage(() -> aiService.sendAgentMessageStream(prompt, conversationId), test, publisher);
        });
    }


    private CompletableFuture<String> sendMessage(Supplier<Flux<String>> messageStream, TestTree.Test test, ToolEventPublisher publisher) {
        if (messageStream == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Не передан поток сообщений при отправке теста " + test));

        final String conversationId = publisher.conversationId() + "_" + test.getId();
        publisher.publish(new CheckEvent.CheckStartedEvent(conversationId, test));

        return messageStream.get()
                .doOnError(e -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, e)))
                .doOnComplete(() -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null)))
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .toFuture()
                .orTimeout(10, TimeUnit.MINUTES);
    }
}
