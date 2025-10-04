package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TestDetailsDTO {
    private String testId;
    private String version;
    private JiraTestDTO testData;
    private List<TestItemStatusDTO> itemStatuses;
    private Map<String, Object> executionRuns;
    private List<String> attachments;
    private List<Map<String, Object>> changeHistory;
}