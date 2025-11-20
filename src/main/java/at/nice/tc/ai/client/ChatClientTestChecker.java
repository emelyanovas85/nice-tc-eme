package at.nice.tc.ai.client;

import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.AiService;
import at.nice.tc.service.JiraService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.utils.FluxUtils;
import at.nice.tc.utils.JiraUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public record ChatClientTestChecker(AiService aiService,
                                    JiraService jiraService,
                                    MemoryService memoryService) {

    public String checkTestCase(TestTree.Test test, ToolEventPublisher publisher) {
        final String conversationId = publisher.conversationId() + "_" + test.getId();

        publisher.publish(new CheckEvent.CheckStartedEvent(conversationId, test));

        if (memoryService.hasInMemory(conversationId)) {
            List<Message> completedMessages = memoryService.getCompletedMessages(conversationId);
            if (completedMessages.size() > 1) {
                return completedMessages.get(1).getText();
            } else if (memoryService.hasActiveStream(conversationId)) {
                return FluxUtils.blockHotFlux( // дожидается ответа целиком
                        memoryService.subscribe(conversationId),
                        Duration.ofMinutes(5)
                );
            }
        }

        // Если нет результатов и нет генерации — сбрасываем память и запускаем проверку теста заново
        memoryService.clearMessages(conversationId);

        Map<String, Object> testAsMap = jiraService
                .getTest(String.valueOf(test.getId()))
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .join();

        String markdownTest = JiraUtils.toMarkdown(testAsMap);
        String prompt = String.join("\n\n", Prompt.get(), markdownTest);
        return FluxUtils.blockHotFlux(
                aiService.sendAgentMessageStream(prompt, conversationId)
                        .doOnError(e -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, e)))
                        .doOnComplete(() -> publisher.publish(new CheckEvent.CheckFinishedEvent(conversationId, test, null))),
                Duration.ofSeconds(300)
        );
    }
}
