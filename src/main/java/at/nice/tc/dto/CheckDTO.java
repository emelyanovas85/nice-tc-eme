package at.nice.tc.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Проверка
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckDTO {

    private String id; // уникальный идентификатор для обращения UI -> backend
    private String name; // одно слово для отображения в UI
    private String description; // несколько слов для отображения при наведении
    private String prompt; // текст промпта для ИИ

    public enum Type { TEST, RUN }

    private Type type; // для тестов и прогонов разные проверки

    @JsonCreator
    public static CheckDTO fromJson(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("prompt") String prompt,
            @JsonProperty("type") String type) {
        return new CheckDTO(
                id,
                name,
                description,
                prompt,
                Type.valueOf(type.toUpperCase()) // Преобразование строки в перечисление
        );
    }
}
