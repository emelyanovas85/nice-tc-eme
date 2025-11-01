package at.nice.tc.service;

import at.nice.tc.aiTools.JiraTool.Jira;
import dto.testCase.VersionDTO;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class JiraService {
    private final Jira jira;

    public JiraService(@Lazy Jira jira) {
        this.jira = jira;
    }

    public CompletableFuture<String> getTest(String id, List<String> fields) {
        return CompletableFuture.supplyAsync(() -> jira.getTest(id, fields));
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

    public List<String> getZephyrScaleTestProperties() throws IOException {
        // Читаем файл (построчный текст)
        ClassPathResource resource = new ClassPathResource("zephyrScaleTestProperties.txt");
        return Files.readAllLines(resource.getFile().toPath())
                .stream()
                .filter(line -> !line.trim().isEmpty())
                .toList();
    }
}
