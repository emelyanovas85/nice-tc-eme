package at.nice.tc.ai.tools;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.utils.ScoreChecker;
import at.nice.tc.utils.ThrowableUtils;
import at.nice.tc.utils.ToolUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static at.nice.tc.ui.MessageDelimiters.THINK_CLOSE;
import static at.nice.tc.ui.MessageDelimiters.THINK_OPEN;
import static java.util.Objects.isNull;


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

            String jsonArray = "[" + allWithoutThinking.trim() + "]";

            jsonArray = jsonArray
                    .replaceAll("\\n\\s+", "")
                    .replaceAll("}\\s*\\{", "},{");

            // Исправляем проблему с отсутствием поля "results"
            jsonArray = jsonArray.replaceAll(
                    "\"key\":\\s*\"([^\"]+)\"\\s*,\\s*\\[",
                    "\"key\": \"$1\", \"results\": ["
            );

            final ObjectMapper MAPPER = new ObjectMapper();
            ScoreChecker scoreChecker = new ScoreChecker();
            final String fileName = keyTestCase + ".json";
            Resource referenceJSONByLLM = scoreChecker.findReferenceJSONByLLM(fileName);
            if (isNull(referenceJSONByLLM)) scoreChecker.writeJsonToResources(fileName, jsonArray);
            List<Map<String, Object>> expected = scoreChecker.parseJsonToListOfMaps(referenceJSONByLLM);
            List<Map<String, Object>> actual;
            try {
                actual = MAPPER.readValue(jsonArray, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            scoreChecker.validateScores(actual, expected, keyTestCase);

            return jsonArray;

        } finally {
            publisher.eventPublisher().endTool(chatId);
        }

        /// проверь тест VPEPVV-T800
    }
}
