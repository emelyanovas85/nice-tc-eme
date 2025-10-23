package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;

    public String sendMessage(String message/*String conversationId, */) {
        return chatClient.prompt()
//               .user(pus -> pus.text(message).param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
    }
}

//    public void sendMessageAndStreamToSse(String message) {
//        Flux<String> contentFlux = sendMessageStream(message);
//
//        contentFlux.subscribe(
//                contentPart -> sseService.sendChatMessage("", "", "", contentPart),
//                error -> sseService.sendSystemNotification("Ошибка при получении ответа ИИ: " + error.getMessage(), "error"),
//                () -> System.out.println("Поток ответа от ИИ завершен."));
//    }

