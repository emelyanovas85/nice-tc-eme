package at.nice.tc.config;


import at.nice.tc.ai.tools.MainChatTools;
import at.nice.tc.ai.tools.googleTool.GoogleTools;
import at.nice.tc.ai.tools.jiraTool.JiraTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MainChatClientConfig {

    private final GoogleTools googleTools;
    private final MainChatTools mainChatTools;
    private final ChatMemory chatMemory;
    private final ChatModel chatModel;
    private final JiraTools jiraTools;

    @Bean
    public ChatClient mainChatClient() {
        return ChatClient.builder(chatModel)
                .defaultTools(mainChatTools, googleTools, jiraTools)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.8)
                        .topP(0.8)
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
