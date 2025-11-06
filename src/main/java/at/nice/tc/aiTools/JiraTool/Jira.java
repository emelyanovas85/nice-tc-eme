package at.nice.tc.aiTools.JiraTool;

import dto.testCase.VersionDTO;

import java.util.List;

public interface Jira {

    /**
     * ВСЕ данные со вкладок "Подробнее", "Шаги", "Вложения".
     * @param id ключ (VPEPVV-T800) или id версии (123456)
     */
    String getFullTest(String id);

    /**
     * Вкладки "Подробнее", "Шаги", "Вложения".
     * @param id ключ (VPEPVV-T800) или id версии (123456)
     * @param fields см availableTestProperties.txt
     */
    String getTest(String id, List<String> fields);

    /**
     * Вкладка "Выполнение"
     * @param versionId id конкретной версии ТК
     * @param fields см availableTestExecutionProperties.txt
     */
    String getTestExecutions(int versionId, List<String> fields);

    String getLastUpdate(String id);

    boolean isAvailable();

    String getAllVersions(String testKey);
}
