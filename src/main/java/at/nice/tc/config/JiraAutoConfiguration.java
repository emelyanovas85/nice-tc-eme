package at.nice.tc.config;

import at.nice.tc.ai.tools.jiraTool.Jira;
import at.nice.tc.service.JiraService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot AutoConfiguration для использования nice-tc-eme как библиотеки.
 * <p>
 * Принимающий проект не требует никакой конфигурации кроме:
 * <pre>
 * jira.username=...
 * jira.password=...
 * </pre>
 * в своём application.properties.
 */
@AutoConfiguration
@Import({JiraConfig.class, SystemPropertyLoader.class})
public class JiraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JiraService jiraService(Jira jira) {
        return new JiraService(jira);
    }
}
