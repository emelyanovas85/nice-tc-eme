package at.nice.tc.dao;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import dto.testCase.VersionDTO;
import impl.TestCase;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Objects;

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


    @AllArgsConstructor
    class JiraImpl implements Jira {
        private JiraTestCaseAPI testCaseAPI;
        private JiraTestRunAPI testRunAPI;
        private JiraClient jiraClient;

        @Override
        public boolean isAvailable() {
            return jiraClient.isAvailable();
        }

        @Override
        public JiraTestDTO readTestFromJira(String id) {
            TestCase testCase = testCaseAPI.getTestCase(id);
            return JiraTestDTO
                    .builder()
                    .id(testCase.getId())
                    .name(testCase.getName())
                    .objective("objective")
                    .steps(testCase.getSteps().stream().map(Objects::toString).toList()) //FIXME: Step это просто строки, но из JiraClient ДТО с полями
                    .author("author")
                    .status("ststus")
                    .precondition("precondition")
                    .labels(List.of("labels"))
                    .priority(testCase.getPriority())
                    .build();
        }

        @Override
        public List<JiraTestVersionDTO> getAllVersions(String testKey) {
            List<VersionDTO> allVersionsTestCaseById = testCaseAPI.getAllVersionsTestCaseById(testKey);
            return List.of(JiraTestVersionDTO
                    .builder()
                    .build());
        }

        @Override
        public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
            return List.of(JiraTestVersionDTO.builder().build());
        }
    }
}
