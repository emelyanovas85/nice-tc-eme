package at.nice.tc.dto;

import at.nice.tc.utils.ThrowableUtils;
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

    /**
     * Возвращает значение поля из теста или ошибку,
     * возникшую в процессе извлечения значения
     * @param <T> позволяет присваивать куда угодно
     */
    @SuppressWarnings("unchecked")
    @JsonIgnore
    public <T> T getValue(JiraTestDTO test) {
        try {
            return (T) valueExtractor.apply(test);
        } catch (Throwable t) {
            return (T) ThrowableUtils.asString(t);
        }
    }
}
