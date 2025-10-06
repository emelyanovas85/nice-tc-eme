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
import java.util.HashMap;
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
    private final ConcurrentHashMap<String, CompletableFuture<JiraTestDTO>> tests;
    private final Jira jira;


    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(jira::isAvailable);
    }

    public CompletableFuture<JiraTestDTO> readTestAsync(String id) {
        return tests.computeIfAbsent(id, AsyncUtils.asCompletableFuture(jira::readTestFromJira));
    }

    public CompletableFuture<List<JiraTestVersionDTO>> searchVersionsAsync(String testId) {
        return CompletableFuture.supplyAsync(() -> jira.searchVersions(testId));
    }

    public CompletableFuture<List<JiraTestVersionDTO>> readRunAsUsedTestVersionsAsync(String runId) {
        return CompletableFuture.supplyAsync(() -> jira.readRunAsUsedTestVersions(runId));
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

    public CompletableFuture<Object> getFieldValue(String testId, String fieldId) {
        return tests.get(testId).thenApplyAsync(CACHED_FIELDS.get(fieldId)::getValue);
    }
}
