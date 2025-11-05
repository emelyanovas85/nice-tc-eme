package at.nice.tc.aiTools;

import at.nice.tc.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AITools {
    private final AiService aiService;

    @Tool(name = "checkRequirements", description = "Служит для проверки списка требований к одному тест-кейсу. " +
            "Возвращает строку с результатами проверки тест-кейса по требованиям")
    public String checkRequirements(
            @ToolParam(description = "id тест-кейса")
            String testCaseId,
            @ToolParam(description = "список требований для проверки тест-кейса на соответствия")
            List<String> requirements) {

        List<CompletableFuture<String>> futures = requirements.stream()
                .map(it -> "Проверь тест-кейс '%s' на соответствие требованию: ".formatted(testCaseId) + it)
                .map(message -> CompletableFuture.supplyAsync(() -> aiService.sendIncognitoMessage(message)))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.joining("\n")))
                .join();
    }
}
