package at.nice.tc.ai.tools;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.utils.ThrowableUtils;
import at.nice.tc.utils.ToolUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class MainChatTools {

    private final TestCheckersAggregator aggregatorChecker;
    private final ToolEventPublisher.Factory publisherFactory;

    @Tool(name = "checkTestCaseByRequirements",
            description = "Проверяет тест-кейс по требованиям. " +
                    "Получает требования из Google Docs и запускает проверку для основного и всех вложенных тест-кейсов. " +
                    "Возвращает результаты проверки по тест-кейсу и вложенным в него тест-кейсам.")
    public String checkTestCaseByRequirements(
            @ToolParam(description = "Ключ или ID основного тест-кейса для проверки")
            String keyTestCase,
            ToolContext toolContext) {
        String chatId = ToolUtils.conversationId(toolContext);

        ToolEventPublisher publisher = publisherFactory.forConversation(chatId);
        publisher.eventPublisher().beginTool(chatId);
        try {
//        String requirements = googleTools.getTestCaseRequirements("a");
            List<String> results = new CopyOnWriteArrayList<>();
            aggregatorChecker.startChecks(keyTestCase, publisher)
                    .thenApply(futures -> futures
                            .stream()
                            .map(future -> future
                                    .exceptionally(ex -> "Не удалось проверить тест " + keyTestCase +
                                            ". Ошибка: " + ThrowableUtils.asString(ex))
                                    .thenAccept(results::add)
                            )
                            .toList()
                    )
                    .thenCompose(futures -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])))
                    .join();
            return String.join("", results);
        } finally {
            publisher.eventPublisher().endTool(chatId);
        }
    }
}
