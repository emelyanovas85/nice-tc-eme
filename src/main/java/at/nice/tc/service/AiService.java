package at.nice.tc.service;

import at.nice.tc.controller.AiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 */
public interface AiService {

    /**
     * Пакетная обработка промптов
     */
    void processBatch(String testId, List<AiController.CheckPrompt> checks);

    /**
     * Отправка сообщения в чат
     */
    void sendChatMessage(String testId, String checkId, String message, Map<String, Object> placeholders);

    /**
     * Остановка обработки
     */
    void stopProcessing(String testId, String checkId);

    /**
     * Проверка доступности AI сервиса
     */
    public boolean isAvailable();




    @Service
    class Mock implements AiService {

        @Autowired
        private SseService sseService;

        // Хранилище активных процессов
        private final Map<String, CompletableFuture<Void>> activeProcesses = new ConcurrentHashMap<>();

        @Override
        public void processBatch(String testId, List<AiController.CheckPrompt> checks) {
            checks.forEach(check -> {
                String processKey = testId + "-" + check.getId();

                sseService.sendProcessingStart(testId, check.getId());

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        processCheckPrompt(testId, check.getId(), check.getPrompt());
                    } catch (Exception e) {
                        sseService.sendChatMessage(testId, check.getId(),
                                "Ошибка обработки промпта: " + e.getMessage(), "comment");
                        sseService.sendStatusUpdate(testId, check.getId(), "failed");
                    } finally {
                        sseService.sendProcessingEnd(testId, check.getId());
                        activeProcesses.remove(processKey);
                    }
                });

                activeProcesses.put(processKey, future);
            });
        }

        @Override
        public void sendChatMessage(String testId, String checkId, String message, Map<String, Object> placeholders) {
            String processKey = testId + "-" + checkId;

            sseService.sendProcessingStart(testId, checkId);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processChatMessage(testId, checkId, message, placeholders);
                } catch (Exception e) {
                    sseService.sendChatMessage(testId, checkId,
                            "Ошибка обработки сообщения: " + e.getMessage(), "comment");
                } finally {
                    sseService.sendProcessingEnd(testId, checkId);
                    activeProcesses.remove(processKey);
                }
            });

            activeProcesses.put(processKey, future);
        }

        public void stopProcessing(String testId, String checkId) {
            String processKey = testId + "-" + checkId;
            CompletableFuture<Void> future = activeProcesses.get(processKey);

            if (future != null) {
                future.cancel(true);
                activeProcesses.remove(processKey);
                sseService.sendProcessingEnd(testId, checkId);
                sseService.sendChatMessage(testId, checkId, "Обработка остановлена пользователем", "comment");
            }
        }

        public boolean isAvailable() {
            return true;
        }

        /**
         * Обработка промпта проверки (заглушка)
         */
        private void processCheckPrompt(String testId, String checkId, String prompt) {
            try {
                Thread.sleep(2000 + (long) (Math.random() * 3000)); // 2-5 секунд

                String[] responses = {
                        "Требование соответствует ожидаемому результату. Тест пройден успешно.",
                        "Обнаружено несоответствие в реализации. Требуется доработка.",
                        "Функциональность работает корректно, соответствует спецификации.",
                        "Найдены незначительные отклонения, но основная логика корректна.",
                        "Тестируемая функция полностью соответствует требованиям."
                };

                String response = responses[(int) (Math.random() * responses.length)];
                String status = response.toLowerCase().contains("соответствует") ? "passed" : "failed";

                sseService.sendChatMessage(testId, checkId, response, "system");
                sseService.sendStatusUpdate(testId, checkId, status);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Обработка прервана");
            }
        }

        /**
         * Обработка сообщения чата (заглушка)
         */
        private void processChatMessage(String testId, String checkId, String message, Map<String, Object> placeholders) {
            try {
                Thread.sleep(1000 + (long) (Math.random() * 2000)); // 1-3 секунды

                String processedMessage = replacePlaceholders(message, placeholders);
                String response = generateAiResponse(processedMessage);

                String messageType = response.toLowerCase().contains("соответствует") ? "match" : "system";
                sseService.sendChatMessage(testId, checkId, response, messageType);

                if (messageType.equals("match")) {
                    sseService.sendStatusUpdate(testId, checkId, "passed");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Обработка прервана");
            }
        }

        private String replacePlaceholders(String text, Map<String, Object> placeholders) {
            String result = text;
            if (placeholders != null) {
                for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                    String placeholder = "{" + entry.getKey() + "}";
                    String value = entry.getValue() != null ? entry.getValue().toString() : "[ПУСТО]";
                    result = result.replace(placeholder, value);
                }
            }
            return result;
        }

        private String generateAiResponse(String message) {
            if (message.toLowerCase().contains("проверь") || message.toLowerCase().contains("тест")) {
                String[] testResponses = {
                        "Проведена проверка. Результат соответствует ожидаемому.",
                        "После анализа можно сделать вывод, что функция работает некорректно.",
                        "Тестирование показало полное соответствие требованиям.",
                        "Обнаружены проблемы в реализации, требуется исправление.",
                        "Функциональность соответствует спецификации и готова к использованию."
                };
                return testResponses[(int) (Math.random() * testResponses.length)];
            }

            String[] generalResponses = {
                    "Понял ваш запрос. Анализирую информацию...",
                    "Рассмотрел предоставленные данные. Могу предоставить следующие выводы...",
                    "На основе анализа могу сказать, что...",
                    "Изучил вопрос. Вот мои рекомендации...",
                    "Провел анализ. Результаты показывают..."
            };

            return generalResponses[(int) (Math.random() * generalResponses.length)];
        }
    }
}
