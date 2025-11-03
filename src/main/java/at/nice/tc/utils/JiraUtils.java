package at.nice.tc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class JiraUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();


    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("<span[^>]*class=\\\"atwho-inserted\\\"[^>]*>(\\{[^}]+\\})</span>\\s*");

    /**
     * Заменяет переменные в виде длинных html на короткий формат {название переменной}
     */
    public static String simplifyHtmlVariables(String json) {
        if (json == null)
            return null;
        return VARIABLE_PATTERN.matcher(json).replaceAll("$1");
    }

    public static JsonTreeMap parseTestJson(String json) {
        try {
            JsonTreeMap test = MAPPER.readValue(json, JsonTreeMap.class).flat();
            shiftField("stepByStepScript", test); // в запросе этого поля нет, а в ответе есть
            return test;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Перемещает значение составного поля (в формате "a.b.c") в родительский объект.
     * <p>
     * Если переданное поле имеет вложенную структуру (содержит точку), метод извлекает его значение
     * из переданной коллекции {@code fields}, удаляет это поле и помещает значение в родительский объект.
     * <p>
     * Пример:
     * <pre>
     *   fields = {
     *       "user.name": {
     *           "text": "Alice",
     *           "alias":"Ali"
     *       },
     *       "user.age": 30
     *   }
     *   shiftField("user.name", fields)
     *   fields = {
     *       "user": {
     *           "text": "Alice",
     *           "alias":"Ali"
     *       },
     *       "user.age": 30
     *
     *   }
     * </pre>
     * <p>
     * Если поле не содержит точек или его значение отсутствует, метод ничего не делает.
     *
     * @param field  путь к полю в виде строки с разделителями-точками (например, "a.b.c")
     * @param fields карта полей, в которой производится перемещение; не должна быть {@code null}
     * @throws IllegalArgumentException если {@code field} или {@code fields} равны {@code null}
     */
    public static void shiftField(String field, JsonTreeMap fields) {
        if (field == null) {
            throw new IllegalArgumentException("Field path must not be null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("Fields map must not be null");
        }

        Object value = fields.remove(field);
        if (value == null) {
            return; // в fields нет ключа field
        }

        String[] levels = field.split("\\.");
        if (levels.length < 2) {
            return; // Поле не является составным
        }

        String parentKey = field.substring(0, field.lastIndexOf("."));
        Object parentValue = fields.get(parentKey);

        if (parentValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parentMap = (Map<String, Object>) parentValue;

            // Если значение — карта, объединяем её с существующим родительским объектом
            // а если значение не карта, то просто удалили вместе с ключом и все
            if (value instanceof Map<?, ?> valueAsMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>) valueAsMap;
                parentMap.putAll(valueMap);
            }
        }
    }


    public static JsonTreeMap sortSteps(JsonTreeMap test) {
        List<LinkedHashMap<String,Object>> steps = test.getAutocast("testScript.steps");
        List<?> sortedSteps = steps.stream()
                .peek(step -> step.put("index", ((int) step.get("index")) + 1))
                .sorted(Comparator.comparingInt(step -> (int) step.get("index")))
                .collect(Collectors.toList());
        test.put("testScript.steps", sortedSteps);
        return test;
    }

    public static String toString(JsonTreeMap treeMap) {
        try {
            return MAPPER.writeValueAsString(treeMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
