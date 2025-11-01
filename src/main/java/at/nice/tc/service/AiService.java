package at.nice.tc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
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
@RequiredArgsConstructor
public class AiService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatClient chatClient;

    public Flux<String> sendMessageStream(String message) {
        return chatClient.prompt("Отвечай по-русски")
                .user(message)
//                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, new Random().nextInt()))
                .stream()
                .content();
    }

    public String sendPrompt(String prompt) {
        String response = chatClient.prompt(prompt)
                .call()
                .content();

        return extractJson(response);
    }

    private String extractJson(String response) {
        // Удаляем блок <think>...</think>
        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();

        // Извлекаем JSON между { и }
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String json = response.substring(jsonStart, jsonEnd + 1);

            // Валидируем JSON
            try {
                objectMapper.readTree(json);
                return json;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON from AI response", e);
            }
        }

        throw new RuntimeException("No JSON found in response: " + response);
    }

}
