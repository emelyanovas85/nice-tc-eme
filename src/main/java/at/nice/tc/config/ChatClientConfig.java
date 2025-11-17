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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient mainChatClient(ChatMemory chatMemory,
                                     ChatModel chatModel,
                                     @Lazy
                                     MainChatTools mainChatTools,
                                     GoogleTools googleTools,
                                     JiraTools jiraTools) {
        return ChatClient.builder(chatModel)
                .defaultTools(mainChatTools, googleTools, jiraTools)
                .defaultOptions(//OpenAiChatOptions.builder()
//                        .internalToolExecutionEnabled(false)
                        ChatOptions.builder()
                        .temperature(0.8)
                        .topP(0.8)
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public ChatClient agentChatClient(ChatMemory chatMemory, ChatModel chatModel) {
        return ChatClient.builder(chatModel)
//            .defaultTools(jiraTools, googleTools) // промпт и данные получит в готовом виде
                .defaultOptions(//OpenAiChatOptions.builder()
//                        .internalToolExecutionEnabled(false)
                        ChatOptions.builder()
                        .temperature(0.3)
                        .topP(0.3)
                        .build())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
