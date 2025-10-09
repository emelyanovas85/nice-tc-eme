package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Определение функции для tool calls.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FunctionDefinition(
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("parameters") Object parameters
) {
    public static FunctionDefinition of(String name, String description) {
        return new FunctionDefinition(name, description, null);
    }

    public static FunctionDefinition of(String name, String description, Object parameters) {
        return new FunctionDefinition(name, description, parameters);
    }
}
