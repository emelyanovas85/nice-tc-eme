package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Запрос для токенизации текста.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenizeRequest(
        @JsonProperty("model") String model,
        @JsonProperty("prompt") String prompt,
        @JsonProperty("messages") List<ChatMessage> messages,
        @JsonProperty("add_special_tokens") Boolean addSpecialTokens
) {}
