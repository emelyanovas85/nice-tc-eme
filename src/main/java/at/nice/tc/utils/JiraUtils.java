package at.nice.tc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class JiraUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();


    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("<span[^>]*class=\\\\\"atwho-inserted\\\\\"[^>]*>(\\{[^}]+})</span>\\s*");
    private static final Pattern FORMATTING_TAGS_PATTERN =
            Pattern.compile("</?(em|strong)>");

    /**
     * Заменяет переменные в виде длинных html на короткий формат {название переменной}
     */
    public static String simplifyHtmlVariables(String json) {
        if (json == null)
            return null;
        json = VARIABLE_PATTERN.matcher(json).replaceAll("$1");
        json = FORMATTING_TAGS_PATTERN.matcher(json).replaceAll("");
        return json;
    }

    public static Map<String, Object> parseTreeMapJson(String json) {
        try {
            //noinspection unchecked
            Map<String, Object> test = MAPPER.readValue(json, LinkedHashMap.class);
            shiftField_stepByStepScript(test); // в запросе этого поля нет, а в ответе есть
            return test;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, Object>> getInnerTestCaseFields(Map<String, Object> test) {
        Object testScriptObj = test.get("testScript");
        if (!(testScriptObj instanceof Map<?, ?>)) return Collections.emptyList();
        //noinspection unchecked
        LinkedHashMap<String, Object> testScript = (LinkedHashMap<String, Object>) testScriptObj;

        Object stepsObj = testScript.get("steps");
        if (!(stepsObj instanceof List<?>)) return Collections.emptyList();
        //noinspection unchecked
        List<LinkedHashMap<String, Object>> steps = (List<LinkedHashMap<String, Object>>) stepsObj;

        //noinspection unchecked
        return steps.stream()
                .filter(step -> step.containsKey("testCase"))
                .map(step -> (Map<String, Object>) step.get("testCase"))
                .collect(Collectors.toList());
    }

    public static Set<String> getNestedTestIds(Map<String, Object> test) {
        return getInnerTestCaseFields(test).stream()
                .map(innerTest -> innerTest.get("id"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    public static Map<String, Object> insertNestedTests(Map<String, Object> test, Map<String, Map<String, Object>> nestedId$nestedTest) {
        getInnerTestCaseFields(test).forEach(shortInnerTest -> {
            Object innerId = String.valueOf(shortInnerTest.get("id"));
            Map<String, Object> fullInnerTest = nestedId$nestedTest.get(innerId);
            shortInnerTest.putAll(fullInnerTest);
        });
        nestedId$nestedTest.forEach((id, test1) -> {
            getInnerTestCaseFields(test1).forEach(shortInnerTest -> {
                Object innerId = String.valueOf(shortInnerTest.get("id"));
                Map<String, Object> fullInnerTest = nestedId$nestedTest.get(innerId);
                shortInnerTest.putAll(fullInnerTest);
            });
        });
        return test;
    }

    /**
     * Перемещает значение поля stepByStepScript в родительский объект.
     *
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
        if (!(testScriptObj instanceof Map<?, ?>)) return test;
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

    public static String toString(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
