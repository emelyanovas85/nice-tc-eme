package at.nice.tc.service;

import at.nice.tc.dao.Jira;
import at.nice.tc.dto.JiraFieldDTO;
import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import at.nice.tc.utils.AsyncUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class JiraService {
    private final ConcurrentHashMap<String, CompletableFuture<JiraTestDTO>> tests = new ConcurrentHashMap<>();
    private final Jira jira;


    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(jira::isAvailable);
    }

    public CompletableFuture<JiraTestDTO> readTestAsync(String id) {
        return tests.computeIfAbsent(id, AsyncUtils.asCompletableFuture(jira::readTestFromJira));
    }

    public CompletableFuture<List<JiraTestVersionDTO>> searchVersionsAsync(String testKey) {
        return CompletableFuture.supplyAsync(() -> jira.searchVersions(testKey));
    }

    public CompletableFuture<List<JiraTestVersionDTO>> readRunAsUsedTestVersionsAsync(String runKey) {
        return CompletableFuture.supplyAsync(() -> jira.readRunAsUsedTestVersions(runKey));
    }

    protected static final Map<String, JiraFieldDTO> CACHED_FIELDS = Stream.of(
            new JiraFieldDTO("1", "name", JiraTestDTO::getName)
    ).collect(Collectors.toUnmodifiableMap(JiraFieldDTO::getId, f -> f));

    public Map<String, JiraFieldDTO> getFieldsMap() {
        return CACHED_FIELDS;
    }

    public List<JiraFieldDTO> getFields() {
        return new ArrayList<>(CACHED_FIELDS.values());
    }

    public CompletableFuture<?> getFieldValue(String testId, String fieldId) {
        return tests.get(testId).thenApplyAsync(CACHED_FIELDS.get(fieldId)::getValue);
    }

    public CompletableFuture<List<?>> getFieldValues(String testId, List<String> fieldIds) {
        CompletableFuture<?>[] futures = fieldIds.stream()
                .map(fieldId -> getFieldValue(testId, fieldId))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures)
                .thenApply(v -> Arrays.stream(futures)
                        .map(CompletableFuture::join) // Безопасно, так как allOf гарантирует завершение всех CompletableFuture
                        .collect(Collectors.toList())
                );
    }
}
