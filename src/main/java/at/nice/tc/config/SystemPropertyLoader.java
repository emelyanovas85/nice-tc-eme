
package at.nice.tc.config;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class SystemPropertyLoader {

    @Value("${jira.username:}")
    private String jiraUsername;

    @Value("${jira.password:}")
    private String jiraPassword;

    @PostConstruct
    public void setSystemProperties() {
        if (jiraUsername != null && !jiraUsername.isBlank()) {
            System.setProperty("jira.username", jiraUsername);
        }
        if (jiraPassword != null && !jiraPassword.isBlank()) {
            System.setProperty("jira.password", jiraPassword);
        }
    }
}
