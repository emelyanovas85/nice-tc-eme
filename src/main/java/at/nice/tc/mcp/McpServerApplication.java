package at.nice.tc.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot приложение для запуска MCP сервера JiraTools.
 * <p>
 * Сервер предоставляет инструменты для работы с Jira тестовыми кейсами
 * через Model Context Protocol (MCP).
 * <p>
 * Доступные транспорты:
 * - Streamable HTTP (порт 8090): HTTP endpoint для mcpo proxy
 * - SSE (Server-Sent Events): Веб-сокеты для прямого подключения
 * <p>
 * Подключение через mcpo к Open WebUI:
 * mcpo --port 8000 --streamable-http http://localhost:8090/mcp
 *
 * @author AI Assistant
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {
        "at.nice.tc.mcp",
        "at.nice.tc.service",
        "at.nice.tc.ai.tools.jiraTool",
        "at.nice.tc.config",
        "at.nice.tc.service",
        "at.nice.tc.ai.tools.googleTool",
        "at.nice.tc.ai.tools",
        "at.nice.tc.ai.aggregator",
        "at.nice.tc.ai.client"

})
public class McpServerApplication {

    public static void main(String[] args) {
        log.info("Starting JiraMcpServer application...");

        SpringApplication app = new SpringApplication(McpServerApplication.class);

        // Логирование стартовой информации в production
        // app.setAdditionalProfiles("prod");

        app.run(args);

        log.info("JiraMcpServer is ready to accept connections");
        log.info("Available endpoint: http://localhost:8090/mcp");
        log.info("Connect via mcpo: mcpo --port 8000 --streamable-http http://localhost:8090/mcp");
    }
}
