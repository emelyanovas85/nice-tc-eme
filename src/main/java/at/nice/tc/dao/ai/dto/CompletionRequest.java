package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Запрос для создания текстового дополнения.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompletionRequest(
        @JsonProperty("model") String model,
        @JsonProperty("prompt") String prompt,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("top_p") Double topP,
        @JsonProperty("n") Integer n,
        @JsonProperty("stream") Boolean stream,
        @JsonProperty("stop") Object stop,
        @JsonProperty("presence_penalty") Double presencePenalty,
        @JsonProperty("frequency_penalty") Double frequencyPenalty,
        @JsonProperty("user") String user
) {}
