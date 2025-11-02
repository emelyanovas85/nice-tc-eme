package at.nice.tc.config;

import at.nice.tc.aiTools.JiraTool.JiraTools;
import at.nice.tc.aiTools.googleTool.GoogleTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableCaching
public class OpenAiConfig {

    private final JiraTools jiraTools;
    private final GoogleTools googleTools;

    @Value("${ai.service.url}")
    private String baseUrl;

    @Value("${ai.service.api-key}")
    private String apiKey;

    @Value("${ai.service.model}")
    private String model;

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "jiraAllSteps",
                "jiraATestFromJira",
                "jiraAllVersions"
        );
    }

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
                .defaultTools(jiraTools, googleTools)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1)
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}