package at.nice.tc.dao.jira;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.testCase.VersionDTO;
import impl.TestCase;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
