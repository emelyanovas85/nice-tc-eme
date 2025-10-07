package at.nice.tc;

import at.nice.tc.dao.Jira;
import at.nice.tc.service.JiraService;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClientSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный класс приложения (БЕЗ Thymeleaf)
 * Статические ресурсы отдаются из /static/
 */
@SpringBootApplication
public class Application {

    @Bean
    public JiraService jiraService() {
        return new JiraService(new ConcurrentHashMap<>(), jira());
    }

    @Bean
    public Jira jira() {
        return new Jira.Mocking(() -> new Jira.JiraImpl(JiraTestCaseAPI.getDefault(), JiraTestRunAPI.getDefault(), JiraClientSingleton.getJiraClient()));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
