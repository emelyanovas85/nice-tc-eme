package at.nice.tc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

//@ComponentScan(basePackages = {
//        "at.nice.tc.mcp",
//        "at.nice.tc.service",
//        "at.nice.tc.ai.tools.jiraTool",
//        "at.nice.tc.config",
//        "at.nice.tc.service",
//        "at.nice.tc.ai.tools.googleTool",
//        "at.nice.tc.ai.tools",
//        "at.nice.tc.ai.aggregator",
//        "at.nice.tc.ai.client",
//        "at.nice.tc.utils"
//
//})
@Slf4j
@SpringBootApplication
@EnableCaching

public class McpServerApplication {

    public static void main(String[] args) {
        log.info("Starting McpServerApplication application...");

        SpringApplication app = new SpringApplication(McpServerApplication.class);

        // Логирование стартовой информации в production
        // app.setAdditionalProfiles("prod");

        app.run(args);

    }
}
