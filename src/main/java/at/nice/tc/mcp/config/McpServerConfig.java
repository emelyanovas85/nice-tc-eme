
package at.nice.tc.mcp.config;

import at.nice.tc.mcp.service.JiraMcpTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация MCP сервера для Spring AI 1.1.0.
 * 
 * Автоматически регистрирует все методы JiraMcpTools с аннотацией @McpTool
 * как доступные инструменты для MCP клиентов.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class McpServerConfig {

    private final JiraMcpTools jiraMcpTools;

    /**
     * Регистрация JiraMcpTools компонента.
     * 
     * Spring AI автоматически обнаружит все методы с @McpTool аннотацией
     * в этом компоненте и зарегистрирует их как доступные инструменты.
     */
    @Bean
    public JiraMcpTools jiraMcpToolsRegistry() {
        log.info("Registering Jira MCP Tools");
        return jiraMcpTools;
    }
}
