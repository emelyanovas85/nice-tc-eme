package at.nice.tc.aiTools;

import at.nice.tc.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AITools {
    @Lazy
    private final AiService aiService;

    @Tool(name = "checkRequirements", description =
            """
            Служит для проверки списка требований к одному тест-кейсу.
            Возвращает строку с результатами проверки тест-кейса по требованиям.
            В списке требований должны содержатся полные требования по каждому пункту.
            Служит только для проверки всего списка требований. Нельзя проверять требования по одному.
            """)
    public String checkRequirements(
            @ToolParam(description = "id тест-кейса")
            String testCaseId,
            @ToolParam(description = "Список требований для проверки тест-кейса на соответствия")
            List<String> requirements) {

        List<CompletableFuture<String>> futures = requirements.stream()
                .map(it -> "Проверь тест-кейс '%s' на соответствие требованию, выбери из тест-кейса, только необходимые поля: ".formatted(testCaseId) + it)
                .map(message -> CompletableFuture.supplyAsync(() -> aiService.sendIncognitoMessage(message)))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining("\n")))
                .join();
    }
}
