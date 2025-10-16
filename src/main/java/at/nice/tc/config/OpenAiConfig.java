package at.nice.tc.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${ai.service.url}")
    private String baseUrl;

    @Value("${ai.service.api-key}")
    private String apiKey;

    @Value("${ai.service.model}")
    private String model;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .build())
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatMemory chatMemory, ChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}