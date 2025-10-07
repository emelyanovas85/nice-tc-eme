package at.nice.tc.dao;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

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
    List<JiraTestVersionDTO> searchVersions(String testKey);

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
        public boolean isAvailable() {
            return false; // FIXME: use client
        }

        @Override
        public JiraTestDTO readTestFromJira(String id) {
            // TODO: use client
            return JiraTestDTO.builder().build();
        }

        @Override
        public List<JiraTestVersionDTO> searchVersions(String testKey) {
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
