package at.nice.tc.controller;

import at.nice.tc.service.AiService;
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

    private AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
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
    public static class BatchProcessRequest {
        private List<CheckPrompt> checks;

        public List<CheckPrompt> getChecks() {
            return checks;
        }

        public void setChecks(List<CheckPrompt> checks) {
            this.checks = checks;
        }
    }

    public static class CheckPrompt {
        private String id;
        private String prompt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }

    public static class ChatRequest {
        private String testId;
        private String checkId;
        private String message;
        private Map<String, Object> placeholders;

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getCheckId() {
            return checkId;
        }

        public void setCheckId(String checkId) {
            this.checkId = checkId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getPlaceholders() {
            return placeholders;
        }

        public void setPlaceholders(Map<String, Object> placeholders) {
            this.placeholders = placeholders;
        }
    }

    public static class StopRequest {
        private String testId;
        private String checkId;

        public String getTestId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public String getCheckId() {
            return checkId;
        }

        public void setCheckId(String checkId) {
            this.checkId = checkId;
        }
    }
}
