package at.nice.tc.dao.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Формат структурированного ответа для всех моделей.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseFormat(
        @JsonProperty("type") String type,
        @JsonProperty("json_schema") Object jsonSchema
) {
    public static ResponseFormat text() {
        return new ResponseFormat("text", null);
    }

    public static ResponseFormat jsonObject() {
        return new ResponseFormat("json_object", null);
    }

    public static ResponseFormat jsonSchema(Object schema) {
        return new ResponseFormat("json_schema", schema);
    }
}
