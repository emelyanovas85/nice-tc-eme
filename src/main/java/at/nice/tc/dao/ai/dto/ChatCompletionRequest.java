package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        @JsonProperty("messages") List<ChatMessage> messages,
        @JsonProperty("model") String model,
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("max_completion_tokens") Integer maxCompletionTokens,
        @JsonProperty("stream") Boolean stream,
        @JsonProperty("stream_options") Object streamOptions,
        @JsonProperty("seed") Long seed,
        @JsonProperty("stop") Object stop,
        @JsonProperty("response_format") ResponseFormat responseFormat,
        @JsonProperty("user") String user
) {
    public static ChatCompletionRequest forTesting(List<ChatMessage> messages) {
        return new ChatCompletionRequest(
                messages,
                "qwen3-32b-awq",     // модель
                0.1,                 // детерминированная генерация
                1024,                // лимит токенов
                false,                // streaming для stopProcessing
                null,                // включить usage в SSE
                12345L,              // воспроизводимость
                List.of("###", "\n\n"), // остановка по маркерам
                ResponseFormat.jsonObject(), // или "json_object"
                "test-user"          // для логов
        );
    }
}


