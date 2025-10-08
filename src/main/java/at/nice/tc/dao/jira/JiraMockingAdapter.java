package at.nice.tc.dao.jira;

import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import at.nice.tc.utils.LocalStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class JiraMockingAdapter implements Jira {
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


    public static final LocalStorage LOCAL_STORAGE = new LocalStorage(Paths.get("mocks"));

    private static String key(String methodName, Object arg) {
        return methodName + "~~" + arg;
    }


    @Override
    public JiraTestDTO readTestFromJira(String id) {
        String key = key("readTestFromJira", id);
        return jira().map(jira -> jira.readTestFromJira(id))
                .map(LOCAL_STORAGE.saveAs(key))
                .orElseGet(() -> LOCAL_STORAGE.getSaved(key).as(JiraTestDTO.class));
    }

    @Override
    public List<JiraTestVersionDTO> getAllVersions(String testKey) {
        String key = key("getAllVersions", testKey);
        return jira().map(jira -> jira.getAllVersions(testKey))
                .map(LOCAL_STORAGE.saveAs(key))
                .orElseGet(() -> LOCAL_STORAGE.getSaved(key).as(new TypeReference<>() {}));
    }

    @Override
    public List<JiraTestVersionDTO> readRunAsUsedTestVersions(String runId) {
        String key = key("readRunAsUsedTestVersions", runId);
        return jira().map(jira -> jira.readRunAsUsedTestVersions(runId))
                .map(LOCAL_STORAGE.saveAs(key))
                .orElseGet(() -> LOCAL_STORAGE.getSaved(key).as(new TypeReference<>() {}));
    }
}