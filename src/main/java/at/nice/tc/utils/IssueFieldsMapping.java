package at.nice.tc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Загружает маппинг полей Jira Issue из issue_fields_description.json.
 * <p>
 * Ключ   — имя поля в JSON-ответе Jira (например {@code "customfield_10700"}).
 * Значение — человекочитаемое название поля (например {@code "Тип дефекта"}).
 */
public class IssueFieldsMapping {

    private IssueFieldsMapping() {
    }

    public static Map<String, String> load() {
        ClassPathResource resource = new ClassPathResource("issue_fields_description.json");
        try {
            return JiraUtils.MAPPER.readValue(
                    resource.getInputStream(),
                    new TypeReference<LinkedHashMap<String, String>>() {
                    }
            );
        } catch (IOException e) {
            return ThrowableUtils.reThrow(e);
        }
    }
}
