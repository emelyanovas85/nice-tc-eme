package at.nice.tc.ai.client;

import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.AiService;
import at.nice.tc.service.JiraService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.utils.JiraUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
            } else if (memoryService.hasActiveStream(conversationId)) {
                // Преобразуем Flux в CompletableFuture
                return memoryService.subscribe(conversationId)
                        .reduce(new StringBuilder(), StringBuilder::append)
                        .map(StringBuilder::toString)
                        .toFuture()
                        .orTimeout(10, TimeUnit.MINUTES);
            }
        }

        // Если нет результатов и нет генерации — сбрасываем память и запускаем проверку теста заново
        memoryService.clearMessages(conversationId);

        CompletableFuture<Map<String, Object>> testAsMapFuture = jiraService
                .getTest(String.valueOf(test.getId()))
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps);

        publisher.publish(new CheckEvent.CheckPreparingEvent("чтение теста...", conversationId, test));

        return testAsMapFuture.thenCompose(testAsMap -> {
            publisher.publish(new CheckEvent.CheckPreparingEvent("парсинг теста...", conversationId, test));

            String markdownTest = JiraUtils.toMarkdown(testAsMap);
            String prompt = String.join("\n\n", Prompt.get(), markdownTest);

            publisher.publish(new CheckEvent.CheckStartedEvent(conversationId, test));

            return aiService.sendAgentMessageStream(prompt, conversationId)
                    .doOnError(e -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, e)))
                    .doOnComplete(() -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null)))
                    .reduce(new StringBuilder(), StringBuilder::append)
                    .map(StringBuilder::toString)
                    .toFuture()
                    .orTimeout(10, TimeUnit.MINUTES);
        });
    }
}
