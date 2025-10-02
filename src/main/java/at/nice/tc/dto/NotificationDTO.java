package at.nice.tc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private String type;
    private String message;
    private LocalDateTime timestamp;
    private Object data;
}