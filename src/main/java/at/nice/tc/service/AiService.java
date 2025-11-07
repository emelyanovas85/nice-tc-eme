package at.nice.tc.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Сервис для работы с AI (Deepseek V3.1)
 * <p>
 * <p>
 * Для проверки:
 * <p>
 * curl https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/chat/completions   -H "Content-Type: application/json"   -d '{
 * "model": "qwen3-32b-awq",
 * "messages": [{"role": "user", "content": "2+2"}],
 * "temperature": 0.0
 * }'
 */
@Service
public class AiService {
    @Autowired
    @Lazy
    private ChatClient chatClient;

    @Autowired
    @Lazy
    @Qualifier("chatClientWithoutMemory")
    private ChatClient chatClientWithoutMemory;

    public Flux<String> sendMessageStream(String message) {
        return chatClient.prompt("Отвечай по-русски")
                .user(message)
//                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, new Random().nextInt()))
                .stream()
                .content();
    }

    public String sendMessage(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    public String sendMessageWithPrompt(String message, String prompt) {
        return chatClient.prompt(prompt)
                .user(message)
                .call()
                .content();
    }

    public String sendIncognitoMessage(String message) {
        return chatClientWithoutMemory.prompt()
                .user(message)
                .call()
                .content();
    }

    public String sendPrompt(String prompt) {
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
