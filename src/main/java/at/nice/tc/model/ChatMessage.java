package at.nice.tc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Модель сообщения чата
 */
public class ChatMessage {

    public enum MessageType {
        USER, SYSTEM, COMMENT, MATCH
    }

    @JsonProperty("type")
    private MessageType type;

    @JsonProperty("content")
    private String content;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("testId")
    private String testId;

    @JsonProperty("checkId")
    private String checkId;

    public ChatMessage() {}

    public ChatMessage(MessageType type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type=" + type +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
