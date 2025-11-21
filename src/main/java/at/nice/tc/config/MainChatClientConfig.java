//package at.nice.tc.config;
//
//
//import at.nice.tc.mcp.service.JiraMcpTools;
//import lombok.RequiredArgsConstructor;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.openai.OpenAiChatOptions;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@RequiredArgsConstructor
//public class MainChatClientConfig {
//
//    private final ChatMemory chatMemory;
//    private final ChatModel chatModel;
//    private final JiraMcpTools jiraMcpTools;
//
//    @Bean
//    public ChatClient mainChatClient() {
//        return ChatClient.builder(chatModel)
//                .defaultTools(jiraMcpTools)
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .temperature(0.5)
//                        .topP(0.5)
//                        .frequencyPenalty(0.01)
//                        .presencePenalty(0.01)
//                        .build())
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
//                        .conversationId("default")
//                        .build())
//                .build();
//    }
//}
