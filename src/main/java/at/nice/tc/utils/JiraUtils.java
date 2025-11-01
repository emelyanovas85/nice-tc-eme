package at.nice.tc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class JiraUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonTreeMap parseTestJson(String json) {
        try {
            return MAPPER.readValue(json, JsonTreeMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public static JsonTreeMap sortSteps(JsonTreeMap test) {
        List<LinkedHashMap<String,Object>> steps = test.getAutocast("testScript.stepByStepScript.steps");
        List<?> sortedSteps = steps.stream()
                .peek(step -> step.put("index", ((int) step.get("index")) + 1))
                .sorted(Comparator.comparingInt(step -> (int) step.get("index")))
                .collect(Collectors.toList());
        test.put("testScript.stepByStepScript.steps", sortedSteps);
        return test;
    }
}
