package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Запрос для оценки сходства между двумя текстами (для всех моделей).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreRequest(
        @JsonProperty("model") String model,
        @JsonProperty("text_1") String text1,
        @JsonProperty("text_2") String text2,
        @JsonProperty("truncate") Integer truncate
) {}
