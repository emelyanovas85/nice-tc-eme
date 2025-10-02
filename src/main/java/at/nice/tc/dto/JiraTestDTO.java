package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class JiraTestDTO {
    public static final JiraTestDTO EMPTY = builder().build();

    private String id;
    private String name;
    private String objective;
    private List<String> steps;
    private String author;
    private String status;
    private String precondition;
    private List<String> labels;
    private String priority;

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id != null ? id : "",
                "name", name != null ? name : "",
                "objective", objective != null ? objective : "",
                "steps", steps != null ? steps : Collections.emptyList(),
                "author", author != null ? author : "",
                "status", status != null ? status : "",
                "precondition", precondition != null ? precondition : "",
                "labels", labels != null ? labels : Collections.emptyList(),
                "priority", priority != null ? priority : ""
        );
    }
}
