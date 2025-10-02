package at.nice.tc.dao;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface Jira {

    /**
     * Данные теста
     * @param id вида '12345'
     */
    JiraTestDTO readTestFromJira(String id);

    /**
     * Идентификаторы версий теста
     * @param testId вида 'T777'
     */
    List<JiraTestVersionDTO> searchVersions(String testId);

    /**
     * Идентификаторы версий тестов, использованных в прогоне
     * @param runId вида 'C777'
     */
    List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId);


    @Component
    @Repository
    class Impl implements Jira {
// TODO:        private JiraClient client;

        @Override
        public JiraTestDTO readTestFromJira(String id) {
            // TODO: use client
            return JiraTestDTO.builder().build();
        }

        @Override
        public List<JiraTestVersionDTO> searchVersions(String testId) {
            // TODO: use client
            return List.of(JiraTestVersionDTO.builder().build());
        }

        @Override
        public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
            // TODO: use client
            return List.of(JiraTestVersionDTO.builder().build());
        }
    }
}
