package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Запрос для ранжирования документов (для всех моделей).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RerankRequest(
        @JsonProperty("model") String model,
        @JsonProperty("query") String query,
        @JsonProperty("documents") List<String> documents,
        @JsonProperty("top_k") Integer topK,
        @JsonProperty("truncate") Integer truncate
) {}
