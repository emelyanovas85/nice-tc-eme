package at.nice.tc.dao.ai_temp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Запрос для детокенизации токенов.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetokenizeRequest(
        @JsonProperty("model") String model,
        @JsonProperty("tokens") List<Integer> tokens,
        @JsonProperty("skip_special_tokens") Boolean skipSpecialTokens
) {}
