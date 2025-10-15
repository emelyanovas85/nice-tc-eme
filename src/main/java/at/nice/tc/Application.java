package at.nice.tc;

import at.nice.tc.dao.jira.Jira;
import at.nice.tc.dao.jira.JiraMockingAdapter;
import at.nice.tc.dao.jira.JiraRepo;
import at.nice.tc.service.JiraService;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClientSingleton;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный класс приложения
 */
@SpringBootApplication
public class Application {

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("")
                        .apiKey("")
                        .build())
                .build())
                .defaultAdvisors(t -> t.advisors(InMemoryChatMemoryRepository::new));
    }

    @Bean
    public JiraService jiraService() {
        return new JiraService(new ConcurrentHashMap<>(), jira());
    }

    @Bean
    public Jira jira() {
        return new JiraMockingAdapter(() -> new JiraRepo(JiraTestCaseAPI.getDefault(), JiraTestRunAPI.getDefault(), JiraClientSingleton.getJiraClient()));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
