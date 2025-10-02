package at.nice.tc.service;

import at.nice.tc.model.Test;
import at.nice.tc.model.Check;
import at.nice.tc.model.TestMetadata;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Сервис для работы с Jira API
 * Заглушка - реальную реализацию предоставит разработчик
 */
@Service
public class Example_JiraService {

    /**
     * Получить список версий теста
     * @param testId ID теста (например T123)
     * @return Список версий (например ["1.0", "1.1", "2.0"])
     */
    public List<String> getTestVersions(String testId) {
        if (testId.startsWith("T")) {
            return Arrays.asList("1.0", "1.1", "2.0");
        }
        throw new RuntimeException("Test not found: " + testId);
    }

    /**
     * Получить список ID версий тестов из прогона
     * @param runId ID прогона (например C456)
     * @return Список ID версий (например ["12345", "12346"])
     */
    public List<String> getRunTestVersions(String runId) {
        if (runId.startsWith("C")) {
            return Arrays.asList("12345", "12346", "12347");
        }
        throw new RuntimeException("Run not found: " + runId);
    }

    /**
     * Получить данные версии теста
     * @param versionId ID версии (например 12345)
     * @return Объект Test с полными данными
     */
    public Test getTestVersion(String versionId) {
        Test test = new Test();
        test.setId(versionId);
        test.setName("Тестовый сценарий " + versionId);
        test.setVersion("1.0");

        // Создаем тестовые проверки
        List<Check> checks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Check check = new Check();
            check.setId(String.valueOf(i) + ".0");
            check.setName("Проверка " + i + ".0");
            check.setDescription("Описание проверки " + i);
            check.setDefaultPrompt("Проверь, что {requirement} соответствует {expected_result}");
            checks.add(check);
        }
        test.setChecks(checks);

        // Создаем тестовые плейсхолдеры
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("requirement", "Функция должна работать корректно");
        placeholders.put("expected_result", "Возвращается положительный результат");
        placeholders.put("test_data", Arrays.asList("value1", "value2", "value3"));
        placeholders.put("empty_placeholder", null); // Плейсхолдер без значения

        Map<String, Object> complexData = new HashMap<>();
        complexData.put("type", "integration_test");
        complexData.put("timeout", 30);
        complexData.put("retries", 3);
        placeholders.put("config", complexData);

        test.setPlaceholders(placeholders);

        // Метаданные
        TestMetadata metadata = new TestMetadata();
        metadata.setCreated(LocalDateTime.now().minusDays(7));
        metadata.setLastModified(LocalDateTime.now().minusHours(2));
        metadata.setAuthor("test.user@company.com");
        metadata.setJiraUrl("https://jira.company.com/browse/TEST-" + versionId);
        metadata.setTestKey("TEST-" + versionId);
        test.setMetadata(metadata);

        return test;
    }

    /**
     * Проверить соединение с Jira
     */
    public boolean checkConnection() {
        return true;
    }

    /**
     * Получить URL Jira для теста
     */
    public String getJiraUrl(String testKey) {
        return "https://jira.company.com/browse/" + testKey;
    }
}
