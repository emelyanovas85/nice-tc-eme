package at.nice.tc.service;

import at.nice.tc.controller.AiController;
import at.nice.tc.dao.AI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 */
@Service
@RequiredArgsConstructor
public class AiService {

        private final SseService sseService = null; // TODO:  убрать null и сделать бин
        private final AI ai = null;  // TODO:  убрать null и сделать бин

        // Хранилище активных процессов
        private final Map<String, CompletableFuture<Void>> activeProcesses = new ConcurrentHashMap<>();


    public void processBatch(String testId, List<AiController.CheckPrompt> checks) {
        ai.processBatch(testId, checks);
    }

    public void sendChatMessage(String testId, String checkId, String message, Map<String, Object> placeholders) {
        ai.sendChatMessage(testId, checkId, message, placeholders);
    }

    public void stopProcessing(String testId, String checkId) {
        ai.stopProcessing(testId, checkId);
    }

    public boolean isAvailable() {
        return ai.isAvailable();
    }
}
