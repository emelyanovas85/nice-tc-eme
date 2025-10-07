package at.nice.tc.dao;

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
            //PERUFR-T5?fields=
            // id,
            // projectId,
            // archived,
            // key,
            // name,
            // objective,
            // majorVersion,
            // latestVersion,
            // precondition,
            // folder(id,fullName),
            // status,
            // priority,
            // estimatedTime,
            // averageTime,
            // componentId,
            // owner,
            // labels,
            // customFieldValues,
            // testScript(id,text,steps(index,description,text,expectedResult,testData,attachments,customFieldValues,id,stepParameters(id,testCaseParameterId,value),testCase(id,key,name,archived,majorVersion,latestVersion,parameters(id,name,defaultValue,index)))),
            // testData,
            // parameters(id,name,defaultValue,index),
            // paramType
//            String testCase = testCaseAPI.requestToJira(id, "id","name", "objective", "" );
            final TestCase testCase = testCaseAPI.getTestCase(id);
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
            return allVersionsTestCaseById.stream()
                    .map(v -> JiraTestVersionDTO.builder()
                            .testKey(testKey)
                            .id(String.valueOf(v.getId()))
                            .version(String.valueOf(v.getMajorVersion()))
                            .build())
                    .collect(Collectors.toList());
        }

        @Override
        public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
            return List.of(JiraTestVersionDTO.builder().build());
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    class Mocking implements Jira {
        private final Supplier<Jira> origin;
        private Jira jiraCache;

        public Optional<Jira> jira() {
            if (jiraCache != null && jiraCache.isAvailable())
                return Optional.of(jiraCache);
            Jira jira;
            try {
                jira = jiraCache = origin.get();
            } catch (Throwable e) {
                jira = null;
            }
            return Optional.ofNullable(jira);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        public static final Path ROOT = Paths.get("mocks");
        private static final ObjectMapper MAPPER = new ObjectMapper();


        static {
            ROOT.toFile().mkdirs();
        }

        private static <T> UnaryOperator<T> saveAs(String key) {
            return obj -> {
                try {
                    String value = MAPPER.writeValueAsString(obj);
                    Files.writeString(ROOT.resolve(key + ".json"), value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    log.warn("Ошибка при маппинге в строку {}", key, e);
                }
                return obj;
            };
        }

        private static <T> T saved(String key) {
            try {
                File file = ROOT.resolve(key + ".json").toFile();
                if (!file.exists())
                    return null;
                return MAPPER.readValue(file, new TypeReference<>() {});
            } catch (IOException e) {
                log.warn("Ошибка получения сохраненного значения {}", key, e);
                return null;
            }
        }

        private static String key(String methodName, Object arg) {
            return methodName + "~~" + arg;
        }


        @Override
        public JiraTestDTO readTestFromJira(String id) {
            String key = key("readTestFromJira", id);
            return jira().map(jira -> jira.readTestFromJira(id))
                    .map(saveAs(key))
                    .orElseGet(() -> saved(key));
        }

        @Override
        public List<JiraTestVersionDTO> getAllVersions(String testKey) {
            String key = key("getAllVersions", testKey);
            return jira().map(jira -> jira.getAllVersions(testKey))
                    .map(saveAs(key))
                    .orElseGet(() -> saved(key));
        }

        @Override
        public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
            String key = key("readRunAsUsedTestVersions", runId);
            return jira().map(jira -> jira.readRunAsUsedTestVersions(runId))
                    .map(saveAs(key))
                    .orElseGet(() -> saved(key));
        }
    }
}
