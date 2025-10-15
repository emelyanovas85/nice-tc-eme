package at.nice.tc;

import at.nice.tc.dao.jira.Jira;
import at.nice.tc.dao.jira.JiraMockingAdapter;
import at.nice.tc.dao.jira.JiraRepo;
import at.nice.tc.service.JiraService;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClientSingleton;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный класс приложения
 */
@SpringBootApplication
public class Application {

    @Bean
    public ChatClient chatClient(@Autowired ChatMemory chatMemory) {
        return ChatClient.builder(OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl("https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru")
                        .apiKey("")
                        .build())
                .build())
                .defaultAdvisors(t -> t.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build()))
                .build();
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
