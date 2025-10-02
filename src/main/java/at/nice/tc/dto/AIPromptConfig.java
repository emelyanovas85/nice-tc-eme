package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIPromptConfig {
    private String itemId;
    private String tabName;
    private String name;
    private String prompt;
    private List<String> dataFields;
}