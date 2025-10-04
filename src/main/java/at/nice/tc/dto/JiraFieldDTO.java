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

    @JsonIgnore
    public <T> T getValue(JiraTestDTO test) {
        //noinspection unchecked
        return (T) valueExtractor.apply(test);
    }
}
