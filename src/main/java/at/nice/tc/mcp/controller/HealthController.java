
package at.nice.tc.mcp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints для MCP сервера.
 * 
 * Используется для проверки статуса сервера и интеграции с mcpo.
 */
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HealthController {

    /**
     * Базовый health check endpoint.
     * 
     * Используется mcpo для проверки доступности сервера.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("Health check endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("server", "jira-mcp-server");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return response;
    }

    /**
     * Info endpoint для получения информации о сервере.
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        log.debug("Info endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Jira MCP Server");
        response.put("version", "1.0.0");
        response.put("description", "MCP Server providing Jira test management tools");
        response.put("protocol", "Streamable HTTP");
        response.put("transport", "HTTP");
        response.put("mcp-endpoint", "/mcp");
        response.put("capabilities", new String[]{"tools", "logging", "notifications"});
        
        return response;
    }

    /**
     * Root endpoint для проверки базовой доступности.
     */
    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "message", "Jira MCP Server is running",
            "endpoints", new String[]{
                "/health - Health check",
                "/info - Server information",
                "/mcp - MCP endpoint for Streamable HTTP",
                "/actuator/health - Spring Boot health",
                "/actuator/metrics - Metrics"
            }
        );
    }
}
