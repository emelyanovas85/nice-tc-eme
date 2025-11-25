package at.nice.tc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    /**
     * Executor для параллельной проверки тестов.
     * Использует пул потоков с достаточным количеством потоков для параллельной обработки всех тестов
     */
    @Bean(name = "testCheckExecutor")
    public Executor testCheckExecutor(@Value("${test.check.executor.threads:50}") int threadCount) {
        return Executors.newFixedThreadPool(threadCount, r -> {
            Thread thread = new Thread(r, "test-check-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }
}

