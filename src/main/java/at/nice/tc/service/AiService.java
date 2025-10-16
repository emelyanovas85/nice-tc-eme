package at.nice.tc.service;

import at.nice.tc.controller.AiController;
import at.nice.tc.dao.ai.AI;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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

    private final ChatClient chatClient;
    private final SseService sseService;
//    private final ChatMemory chatMemory;

    //        private final SseService sseService = null; // TODO:  убрать null и сделать бин
    private final AI ai = null;  // TODO:  убрать null и сделать бин

//     Хранилище активных процессов
//    private final Map<String, CompletableFuture<Void>> activeProcesses = new ConcurrentHashMap<>();


    public void sendMessageStream(String message, String testId, String checkId/*String conversationId, */) {
        chatClient.prompt()
//               .user(pus -> pus.text(message).param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .stream()
                .content()
                .subscribe(content-> sseService.sendChatMessage(testId, checkId, content, "partial"));
    }

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
