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
    public JiraClient getJiraClient() {
        return JiraClientSingleton.getJiraClient();
    }

    @Bean
    public JiraTestCaseAPI getJiraTestCaseAPI() {
        return JiraTestCaseAPI.getDefault();
    }

    @Bean
    public JiraTestRunAPI getJiraTestRunAPI() {
        return JiraTestRunAPI.getDefault();
    }
}
