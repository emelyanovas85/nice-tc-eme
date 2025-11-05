package at.nice.tc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Random;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatClient chatClient;

    @Autowired
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
