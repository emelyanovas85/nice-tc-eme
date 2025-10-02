package at.nice.tc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Базовый тест для проверки загрузки Spring контекста (БЕЗ THYMELEAF)
 */
@SpringBootTest
class ApplicationTests {

    @Test
    void contextLoads() {
        // Тест проверяет, что Spring контекст загружается без Thymeleaf
    }
}
