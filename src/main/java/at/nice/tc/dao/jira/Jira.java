package at.nice.tc.dao.jira;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import java.util.List;

public interface Jira {

    /**
     * @return {true} - jira доступна
     */
    boolean isAvailable();

    /**
     * Данные теста
     * @param id вида '12345'
     */
    JiraTestDTO readTestFromJira(String id);

    /**
     * Идентификаторы версий теста
     * @param testKey вида 'T777'
     */
    List<JiraTestVersionDTO> getAllVersions(String testKey);

    /**
     * Идентификаторы версий тестов, использованных в прогоне
     * @param runId вида 'C777'
     */
    List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId);


}
