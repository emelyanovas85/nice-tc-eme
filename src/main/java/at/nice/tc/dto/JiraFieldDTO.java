package at.nice.tc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Function;

@Data
@AllArgsConstructor
public class JiraFieldDTO {
    private String id;
    private String name;
    @JsonIgnore
    private Function<JiraTestDTO, Object> valueExtractor;

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public <T> T getValue(JiraTestDTO test) {
        try {
            return (T) valueExtractor.apply(test);
        } catch (Throwable t) {
            return (T) t;
        }
    }
}
