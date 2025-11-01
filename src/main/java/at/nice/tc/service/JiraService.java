package at.nice.tc.service;

import at.nice.tc.aiTools.JiraTool.Jira;
import dto.testCase.VersionDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class JiraService {
    private final Jira jira;

    public JiraService(@Lazy Jira jira) {
        this.jira = jira;
    }

    public CompletableFuture<String> getFullTest(String id) {
        return CompletableFuture.supplyAsync(() -> jira.getFullTest(id));
    }

    public CompletableFuture<String> getLastUpdate(String id) {
        return CompletableFuture.supplyAsync(() -> jira.getLastUpdate(id));
    }

    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(jira::isAvailable);
    }

    public CompletableFuture<List<VersionDTO>> getAllVersionsAsync(String testKey) {
        return CompletableFuture.supplyAsync(() -> jira.getAllVersions(testKey));
    }
}
