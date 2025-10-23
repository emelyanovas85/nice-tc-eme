package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Базовый запрос для создания embeddings (для всех моделей).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmbeddingRequest(
        @JsonProperty("model") String model,
        @JsonProperty("input") Object input,
        @JsonProperty("encoding_format") String encodingFormat,
        @JsonProperty("dimensions") Integer dimensions,
        @JsonProperty("user") String user
) {}
