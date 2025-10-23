package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Определение инструмента (функции) для AI моделей.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Tool(
        @JsonProperty("type") String type,
        @JsonProperty("function") FunctionDefinition function
) {
    public static Tool function(FunctionDefinition function) {
        return new Tool("function", function);
    }
}
