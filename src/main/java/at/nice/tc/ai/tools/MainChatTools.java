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

import static at.nice.tc.ui.MessageDelimiters.THINK_CLOSE;
import static at.nice.tc.ui.MessageDelimiters.THINK_OPEN;


@Component
@RequiredArgsConstructor
public class MainChatTools {

    private final TestCheckersAggregator aggregatorChecker;
    private final ToolEventPublisher.Factory publisherFactory;

    @Tool(name = "checkTestCaseByRequirements",
            description = "Получение необходимых для данных в формате json c результатами проверки верхнеуровнего" +
                    " (основного) тест-кейса и вложенных в него тест-кейсов. Необходим для выполнения ")
    public String checkTestCaseByRequirements(
            @ToolParam(description = "Ключ или ID верхнеуровнего (основного) тест-кейса для проверки")
            String keyTestCase,
            ToolContext toolContext) {
        String chatId = ToolUtils.conversationId(toolContext);

        ToolEventPublisher publisher = publisherFactory.forConversation(chatId);
        publisher.eventPublisher().beginTool(chatId);
        try {
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

            String allWithoutThinking = String.join("", results)
                    .replaceAll("(?s)" + THINK_OPEN.getPlaceholder() + ".*?" + THINK_CLOSE.getPlaceholder(), "");

            return allWithoutThinking.trim();

        } finally {
            publisher.eventPublisher().endTool(chatId);
        }
    }
}
