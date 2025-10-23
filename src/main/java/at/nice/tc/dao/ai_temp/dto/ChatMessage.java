package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Базовое сообщение в диалоге для всех AI моделей.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") Object content,
        @JsonProperty("name") String name,
        @JsonProperty("tool_call_id") String toolCallId
) {
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }
}
