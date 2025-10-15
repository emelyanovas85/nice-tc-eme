package at.nice.tc.dao.ai;

import at.nice.tc.controller.AiController;
import at.nice.tc.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 */
public interface AI {

    /**
     * Проверка доступности AI сервиса
     */
    boolean isAvailable();


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
}
