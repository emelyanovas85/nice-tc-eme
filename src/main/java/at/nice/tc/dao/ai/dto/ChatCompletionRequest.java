package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Запрос для создания чат-ответа (Qwen3 32B/30B модели).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        @JsonProperty("messages") List<ChatMessage> messages,
        @JsonProperty("model") String model,
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
        @JsonProperty("top_p") Double topP,
        @JsonProperty("top_k") Integer topK,
        @JsonProperty("min_p") Double minP,
        @JsonProperty("frequency_penalty") Double frequencyPenalty,
        @JsonProperty("presence_penalty") Double presencePenalty,
        @JsonProperty("repetition_penalty") Double repetitionPenalty,
        @JsonProperty("stop") Object stop,
        @JsonProperty("stream") Boolean stream,
        @JsonProperty("n") Integer n,
        @JsonProperty("seed") Long seed,
        @JsonProperty("logprobs") Boolean logprobs,
        @JsonProperty("top_logprobs") Integer topLogprobs,
        @JsonProperty("user") String user,
        @JsonProperty("tools") List<Tool> tools,
        @JsonProperty("tool_choice") Object toolChoice,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {}
