package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JiraTestVersionDTO {
    private String testKey;    // key "XXXXX-T777"
    private String id;        // id "12345"
    private String version;   // majorVersion "2"
}
