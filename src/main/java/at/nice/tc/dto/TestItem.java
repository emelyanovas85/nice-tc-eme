package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestItem {
    private String testId;
    private String itemId;
    private String tabName;
    private String name;
    private ItemStatus status;

    public enum ItemStatus {
        CHECKING,
        COMPLIANT,
        NON_COMPLIANT
    }
}