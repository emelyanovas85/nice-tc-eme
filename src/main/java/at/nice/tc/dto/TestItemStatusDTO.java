package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestItemStatusDTO {
    private String itemId;
    private String tabName;
    private String name;
    private String status; // CHECKING, COMPLIANT, NON_COMPLIANT
    private String statusColor;
    private boolean highlighted;
    private String aiResponse;
}