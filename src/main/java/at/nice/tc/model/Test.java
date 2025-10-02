package at.nice.tc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Модель теста
 */
public class Test {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("checks")
    private List<Check> checks;

    @JsonProperty("placeholders")
    private Map<String, Object> placeholders;

    @JsonProperty("metadata")
    private TestMetadata metadata;

    public Test() {}

    public Test(String id, String name, String version) {
        this.id = id;
        this.name = name;
        this.version = version;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Check> getChecks() {
        return checks;
    }

    public void setChecks(List<Check> checks) {
        this.checks = checks;
    }

    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Map<String, Object> placeholders) {
        this.placeholders = placeholders;
    }

    public TestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(TestMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "Test{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", checksCount=" + (checks != null ? checks.size() : 0) +
                '}';
    }
}
