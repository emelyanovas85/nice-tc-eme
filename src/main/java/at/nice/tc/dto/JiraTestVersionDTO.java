package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JiraTestVersionDTO {
    private String testId;
    private String id;
    private String version;
    // TODO: задать правильные поля
}
