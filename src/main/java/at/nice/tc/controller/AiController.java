package at.nice.tc.controller;

import at.nice.tc.service.AiService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для работы с AI сервисом
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }


    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String sendMessage(@RequestBody StopRequest request) {
        // Вызываем сервис для отправки сообщения и получения ответа
        return aiService.sendMessage(conversationId, message);
    }




















    /**
     * Пакетная обработка промптов для теста
     * @param testId ID теста
     * @param request Запрос с промптами для проверок
     */
    @PostMapping("/batch/{testId}")
    public ResponseEntity<String> processBatch(
            @PathVariable String testId,
            @RequestBody BatchProcessRequest request) {
        try {
            aiService.processBatch(testId, request.getChecks());
            return ResponseEntity.ok("Пакетная обработка запущена");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка запуска обработки");
        }
    }

    /**
     * Отправка сообщения в чат
     * @param request Запрос с сообщением
     */
    @PostMapping("/chat")
    public ResponseEntity<String> sendChatMessage(@RequestBody ChatRequest request) {
        try {
            aiService.sendChatMessage(
                request.getTestId(),
                request.getCheckId(),
                request.getMessage(),
                request.getPlaceholders()
            );
            return ResponseEntity.ok("Сообщение отправлено");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка отправки сообщения");
        }
    }

    /**
     * Остановка обработки
     * @param request Запрос на остановку
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stopProcessing(@RequestBody StopRequest request) {
        try {
            aiService.stopProcessing(request.getTestId(), request.getCheckId());
            return ResponseEntity.ok("Обработка остановлена");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка остановки обработки");
        }
    }

    /**
     * Получить статус AI сервиса
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        try {
            boolean isAvailable = aiService.isAvailable();
            if (isAvailable) {
                return ResponseEntity.ok("available");
            } else {
                return ResponseEntity.ok("unavailable");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }

    // Внутренние классы для запросов
    @Setter
    @Getter
    public static class BatchProcessRequest {
        private List<CheckPrompt> checks;

    }

    @Setter
    @Getter
    public static class CheckPrompt {
        private String id;
        private String prompt;

    }

    @Setter
    @Getter
    public static class ChatRequest {
        private String testId;
        private String checkId;
        private String message;
        private Map<String, Object> placeholders;

    }

    @Setter
    @Getter
    public static class StopRequest {
        private String testId;
        private String checkId;

    }
}
