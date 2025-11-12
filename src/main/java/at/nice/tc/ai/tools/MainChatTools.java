package at.nice.tc.ai.tools;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.ai.tools.googleTool.GoogleTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class MainChatTools {

    private final TestCheckersAggregator aggregatorChecker;
    private final GoogleTools googleTools;

    @Tool(name = "checkTestCaseByRequirements",
            description = "Проверяет тест-кейс по требованиям. " +
                    "Получает требования из Google Docs и запускает проверку для основного и всех вложенных тест-кейсов. " +
                    "Возвращает результаты проверки по тест-кейсу и вложенным в него тест-кейсам.")
    public String checkTestCaseByRequirements(
            @ToolParam(description = "Ключ или ID основного тест-кейса для проверки")
            String keyTestCase) {

//        String requirements = googleTools.getTestCaseRequirements("a");
        return String.join("",
                aggregatorChecker.checkTestCase(keyTestCase)
                        .thenCompose(futures -> CompletableFuture
                                .allOf(futures.toArray(new CompletableFuture[0]))
                                .thenApply(v -> futures
                                        .stream()
                                        .map(future -> future.exceptionally(ex ->
                                                "Не удалось проверить тест" + keyTestCase + "Ошибка: " + ex.getMessage()))
                                        .map(CompletableFuture::join)
                                        .collect(toList())))
                        .join());
    }


    @Tool(name = "askAboutTestCase",
            description = "Задает дополнительный вопрос о тест-кейсе, который был проверен ранее. " +
                    "Вопрос передается в соответствующий чат-клиент, который имеет контекст проверки.")
    public String askAboutTestCase(
            @ToolParam(description = "Ключ или ID тест-кейса")
            String keyTestCase,
            @ToolParam(description = "Дополнительный вопрос о тест-кейсе")
            String question) {

        return aggregatorChecker.askQuestionByKey(keyTestCase, question).join();
    }
}
