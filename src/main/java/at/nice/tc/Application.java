package at.nice.tc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Главный класс приложения
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class Application {
    private static ConfigurableApplicationContext context;
    private static String[] mainArgs;


    public static void main(String[] args) {
        mainArgs = args;
        context = SpringApplication.run(Application.class, args);
    }

    public static void restart() {
        new Thread(() -> {
            context.close();
            context = SpringApplication.run(Application.class, mainArgs);
        }).start();
    }
}
