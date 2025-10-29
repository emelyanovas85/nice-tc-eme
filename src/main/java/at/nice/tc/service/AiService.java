package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 * <p>
 * <p>
 *     Для проверки:
 * <p>
 * curl https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/chat/completions   -H "Content-Type: application/json"   -d '{
 * "model": "qwen3-32b-awq",
 * "messages": [{"role": "user", "content": "2+2"}],
 * "temperature": 0.0
 * }'
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;

    public Flux<String> sendMessageStream(String message) {
        return chatClient.prompt()
//               .user(pus -> pus.text(message).param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(
                        "Отвечай по-русски.\n\n" +
                                message)
                .stream()
                .content();
    }
}
