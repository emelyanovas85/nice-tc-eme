package at.nice.tc.service;

import at.nice.tc.dao.Jira;
import at.nice.tc.dto.JiraFieldDTO;
import at.nice.tc.dto.JiraTestDTO;
import at.nice.tc.dto.JiraTestVersionDTO;
import at.nice.tc.utils.AsyncUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    protected static final List<JiraFieldDTO> CACHED_FIELDS = List.of(
            new JiraFieldDTO("1", "name", JiraTestDTO::getName)
    );

    public List<JiraFieldDTO> getFields() {
        return CACHED_FIELDS;
    }
}
