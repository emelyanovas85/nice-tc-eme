package at.nice.tc.dao.jira;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Slf4j
@RequiredArgsConstructor
public class JiraMocking implements Jira {
    private final Supplier<Jira> origin;
    private Jira jiraCache;

    public Optional<Jira> jira() {
        if (jiraCache != null && jiraCache.isAvailable())
            return Optional.of(jiraCache);
        Jira jira;
        try {
            jiraCache = jira = origin.get();
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
            return MAPPER.readValue(file, new TypeReference<>() {
            });
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