package at.nice.tc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Метаданные теста
 */
public class TestMetadata {

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("lastModified")
    private LocalDateTime lastModified;

    @JsonProperty("author")
    private String author;

    @JsonProperty("jiraUrl")
    private String jiraUrl;

    @JsonProperty("testKey")
    private String testKey;

    public TestMetadata() {}

    // Getters and Setters
    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public String getTestKey() {
        return testKey;
    }

    public void setTestKey(String testKey) {
        this.testKey = testKey;
    }
}
