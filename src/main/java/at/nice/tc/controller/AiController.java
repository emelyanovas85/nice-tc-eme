package at.nice.tc.controller;

import at.nice.tc.config.Prompt;
import at.nice.tc.service.AiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST контроллер для работы с AI сервисом
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping(value = "/chat/stream")
    public Flux<String> sendMessageAsStream(@RequestBody TestMessage request) {
        return aiService.sendMessageStream(request.getMessage());
    }

    @GetMapping("/check/{testCaseId}")
    public ResponseEntity<String> checkTestCase(@PathVariable String testCaseId) {
        // Сконструировать промпт (или использовать Tool), чтобы AI сам запросил требования и данные
        String prompt = Prompt.checkRequirements(testCaseId);

        String aiResult = aiService.sendPrompt(prompt);
        return ResponseEntity.ok(aiResult);
    }

    @Setter
    @Getter
    public static class TestMessage {
        private String message;
    }
}
