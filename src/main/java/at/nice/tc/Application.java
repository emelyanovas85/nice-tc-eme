package at.nice.tc;

import at.nice.tc.dao.jira.Jira;
import at.nice.tc.dao.jira.JiraMocking;
import at.nice.tc.dao.jira.JiraRepo;
import at.nice.tc.service.CheckService;
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
    public CheckService checkService() {
        return new CheckService();
    }

    @Bean
    public Jira jira() {
        return new JiraMocking(() -> new JiraRepo(JiraTestCaseAPI.getDefault(), JiraTestRunAPI.getDefault(), JiraClientSingleton.getJiraClient()));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
