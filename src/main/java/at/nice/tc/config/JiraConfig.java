package at.nice.tc.config;

import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import jiraClient.JiraClientSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;


@DependsOn("systemPropertyLoader")
@Configuration
@Lazy
public class JiraConfig {

    @Bean
    @Lazy
    public JiraClient getJiraClient() {
        return JiraClientSingleton.getJiraClient();
    }

    @Bean
    @Lazy
    public JiraTestCaseAPI getJiraTestCaseAPI() {
        return JiraTestCaseAPI.getDefault();
    }

    @Bean
    @Lazy
    public JiraTestRunAPI getJiraTestRunAPI() {
        return JiraTestRunAPI.getDefault();
    }
}
