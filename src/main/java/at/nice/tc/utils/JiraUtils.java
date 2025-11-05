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

    public static Map<String, Object> parseTestJson(String json) {
        try {
            Map<String, Object> test = MAPPER.readValue(json, LinkedHashMap.class);
            shiftField_stepByStepScript(test); // в запросе этого поля нет, а в ответе есть
            return test;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Перемещает значение поля stepByStepScript в родительский объект.
     * @param fields карта полей, в которой производится перемещение; не должна быть {@code null}
     * @throws IllegalArgumentException {@code fields} равны {@code null}
     */
    public static void shiftField_stepByStepScript(Map<String, Object> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Fields map must not be null");
        }
        fields.keySet().removeIf(k -> k.contains(".")); // удаляем выпрямленные поля

        Object testScript = fields.get("testScript");
        if (!(testScript instanceof Map))
            return;

        @SuppressWarnings("unchecked")
        Map<String, Object> testScriptMap = (Map<String, Object>) testScript;

        Object stepByStepScript = testScriptMap.remove("stepByStepScript");
        if (!(stepByStepScript instanceof Map))
            return;

        @SuppressWarnings("unchecked")
        Map<String, Object> stepByStepScriptMap = (Map<String, Object>) stepByStepScript;

        testScriptMap.putAll(stepByStepScriptMap);
    }


    public static Map<String, Object> sortSteps(Map<String, Object> test) {
        Object testScriptObj = test.get("testScript");
        if (!(testScriptObj instanceof Map<?,?>)) return test;
        //noinspection unchecked
        LinkedHashMap<String, Object> testScript = (LinkedHashMap<String, Object>) testScriptObj;

        Object stepsObj = testScript.get("steps");
        if (!(stepsObj instanceof List<?>)) return test;
        //noinspection unchecked
        List<LinkedHashMap<String, Object>> steps = (List<LinkedHashMap<String, Object>>) stepsObj;
        List<?> sortedSteps = steps.stream()
                .peek(step -> step.put("index", ((int) step.get("index")) + 1))
                .sorted(Comparator.comparingInt(step -> (int) step.get("index")))
                .collect(Collectors.toList());
        testScript.put("steps", sortedSteps);
        return test;
    }

    public static String toString(Map<String, Object> treeMap) {
        try {
            return MAPPER.writeValueAsString(treeMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
