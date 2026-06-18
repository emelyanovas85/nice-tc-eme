package at.nice.tc.ai.tools.jiraTool;

import java.util.List;

public interface Jira {

    /**
     * ВСЕ данные со вкладок "Подробнее", "Шаги", "Вложения".
     *
     * @param id ключ (VPEPVV-T800) или id версии (123456)
     */
    String getFullTest(String id);

    /**
     * Вкладки "Подробнее", "Шаги", "Вложения".
     *
     * @param id     ключ (VPEPVV-T800) или id версии (123456)
     * @param fields см availableTestProperties.txt
     */
    String getTest(String id, List<String> fields);

    /**
     * Служит для получения обрезанного тест-кейса
     *
     * @param id ключ (VPEPVV-T800) или id версии (123456)
     */
    String getTestCase(String id);

    /**
     * Вкладка "Выполнение"
     *
     * @param versionId id конкретной версии ТК
     * @param fields    см availableTestExecutionProperties.txt
     */
    String getTestExecutions(int versionId, List<String> fields);

    String getLastUpdate(String id);

    boolean isAvailable();

    String getAllVersions(String testKey);

    /**
     * Получает данные задачи Jira Issue (не тест-кейс) по ключу.
     *
     * @param issueKey ключ задачи, например VPEPVV-1123
     * @param fields   список полей для запроса; если null — запросить все поля
     * @return сырая JSON-строка ответа
     */
    String getIssue(String issueKey, List<String> fields);
}
