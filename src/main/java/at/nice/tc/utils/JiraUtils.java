package at.nice.tc.utils;

import at.nice.tc.model.TestTree;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class JiraUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * Бегло проверяет, что передан не HTML
     */
    public static Optional<CompletableFuture<String>> optionalNotHTML(String response) {
        if (response == null || response.startsWith("<!DOCTYPE html>") || response.startsWith("<html ")) { // вернулся HTML вместо json
            return Optional.empty();
        }
        return Optional.of(CompletableFuture.completedFuture(response));
    }


    private static final Pattern SPANS_PATTERN = Pattern.compile("</?span[^>]*>");
    private static final Pattern SPACE_AFTER_PATTERN = Pattern.compile("[\\s\u00A0\u2060]+(\"[,}])");
    private static final Pattern FORMATTING_TAGS_PATTERN = Pattern.compile("</?(em|strong)>");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("[\\s\u00A0]{2,}");
    private static final Pattern UNPRINTABLE_CHARS_PATTERN = Pattern.compile("\u2060");



    /**
     * Заменяет переменные в виде длинных html на короткий формат {название переменной}
     */
    public static String simplifyHtmlVariables(String json) {
        if (json == null)
            return null;
        json = SPANS_PATTERN.matcher(json).replaceAll("");
        json = SPACE_AFTER_PATTERN.matcher(json).replaceAll("$1");
        json = FORMATTING_TAGS_PATTERN.matcher(json).replaceAll("");
        json = MULTIPLE_SPACES_PATTERN.matcher(json).replaceAll(" ");
        json = UNPRINTABLE_CHARS_PATTERN.matcher(json).replaceAll("");
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

    public static String toMarkdown(Map<String, Object> test,
                                    Map<String, Map<String, Object>> nestedId$nestedTest) {
        if (test == null)
            return "null";

        MarkdownMap<String, Object> markdown = new MarkdownMap<>(test);

        markdown.appendRow("# Основной тест-кейс")
                .append(toMarkdown(test));

        if (!nestedId$nestedTest.isEmpty()) {
            markdown.appendRow("\n\n\n# Вложенные тест-кейсы")
                    .append(
                            nestedId$nestedTest.values().stream()
                                    .map(JiraUtils::toMarkdown)
                                    .collect(Collectors.joining("\n\n"))
                    );
        }

        try {
            Files.writeString(Paths.get(test.get("key") + ".md"), markdown.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//        Files.writeString(Paths.get(test.get("key") + ".json"), toString(insertNestedTests(test, nestedId$nestedTest)), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//            for (Map<String, Object> nestedTest : nestedId$nestedTest.values()) {
//                Files.writeString(Paths.get(nestedTest.get("key") + ".json"), MAPPER.writeValueAsString(test), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//            }
        } catch (IOException e) {
            ThrowableUtils.reThrow(e);
        }

        return markdown.toString();
    }

    public static String toMarkdown(Map<String, Object> test) {
        if (test == null)
            return "null";

        MarkdownMap<String, Object> markdown = new MarkdownMap<>(test);

        markdown.appendRow("\n## Тест-кейс ", "id")
                .appendRow("\n### Подробнее")
                .appendRow("#### Ключ\n- ", "key")
                .appendRow("#### Версия\n- ", "majorVersion")
                .appendRow("#### Наименование\n- ", "name")
                .appendRow("#### Задача тест-кейса\n- ", "objective")
                .appendRow("#### Предварительные действия\n", "precondition");

        markdown.optionalListMap("parameters").ifPresent(parameters -> {
            if (parameters.isEmpty())
                return;
            markdown.appendRow("\n### Входные параметры")
                    .appendRow("|name|default value|")
                    .appendRow("|-|-|");
            parameters.sort(Comparator.comparingInt(params -> (int) params.get("index")));
            parameters.forEach(param ->
                    markdown.append("|").append(param.get("name")).append("|").append(param.get("defaultValue")).appendRow("|")
            );
        });

        markdown.optionalListMap("testData").ifPresent(testData -> {
            if (testData.isEmpty())
                return;
            markdown.appendRow("\n### Наборы тестовых данных");
            Map<Integer, String> headersMap = testData.stream().reduce(new HashMap<>(), (map, row) -> {
                row.remove("id"); // ломает структуру
                row.keySet().forEach(header -> {
                    int index = (int) new OptionalMap<>(row).optionalMap(header).get().get("index");
                    map.put(index, header);
                });
                return map;
            }, (m1, m2) -> m1);
            List<String> headers = headersMap.keySet().stream().sorted().map(headersMap::get).toList();
            markdown.append("|№").append("|").append(String.join("|", headers)).appendRow("|");

            // отделитель заголовков
            markdown.append("|-"); // для колонки №
            headers.forEach(h -> markdown.append("|-"));
            markdown.appendRow("|");

            for (int i = 0; i < testData.size(); i++) {
                Map<String, Object> row = testData.get(i);
                markdown.append("|").append(i + 1);
                headers.forEach(header -> {
                    //noinspection unchecked
                    Map<String, Object> cell = (Map<String, Object>) row.get(header);
                    Object value = cell == null ? "" : cell.get("value");
                    markdown.append("|").append(value);
                });
                markdown.appendRow("|");
            }
        });

        if (!markdown.toString().trim().endsWith("|")) // не добавлена таблица параметров
            markdown.appendRow("\n### Параметры").appendRow("нет параметров");

        markdown.appendRow("\n### Шаги");
        markdown.optionalMap("testScript").ifPresent(tScript -> {
            //noinspection unchecked
            List<Map<String, Object>> steps = (List<Map<String, Object>>) tScript.get("steps");

            markdown.appendRow("|№|наименование|вложения|тестовые данные|ожидаемый результат|")
                    .appendRow("|-|-|-|-|-|");
            steps.forEach(step0 -> {
                OptionalMap<String, Object> step = new OptionalMap<>(step0);

                markdown.append("|").append(step.get("index"))
                        .append("|").append(step.optional("description").orElseGet(
                                () -> step.optionalMap("testCase").map(t -> "Выполнить тест id = " + t.get("id")).orElse("")
                        ))
                        .append("|").append(
                                step.optionalListMap("attachments")
                                        .map(attas -> attas.stream().map(a -> String.format("%s(%s)", a.get("id"), a.get("name"))).collect(Collectors.joining(",")))
                                        .orElse(""))

                        .append("|").append(step.optional("testData").orElseGet(() -> // если вложенный тест, то параметры, с которыми нужно вызвать
                                step.optionalListMap("stepParameters").map(stepParameters -> {
                                            if (stepParameters.isEmpty())
                                                return "";
                                            for (var stepParamMap : stepParameters) { // добавление индекса и названия параметра в stepParamMap
                                                new OptionalMap<>(stepParamMap).optionalMap("testCaseParameter").ifPresent(stepParamMap::putAll);
                                            }
                                            stepParameters.sort(Comparator.comparingInt(stepParamMap -> (int) stepParamMap.getOrDefault("index", -1)));
                                            var paramsJson = new LinkedHashMap<>();
                                            stepParameters.forEach(stepParamMap -> paramsJson.put(stepParamMap.get("name"), stepParamMap.get("value")));
                                            paramsJson.remove(null); // параметры "значение по умолчанию" падают в null
                                            try {
                                                return simplifyHtmlVariables(MAPPER.writeValueAsString(paramsJson));
                                            } catch (JsonProcessingException e) {
                                                return ThrowableUtils.reThrow(e);
                                            }
                                        })
                                        .orElse("")
                        ))

                        .append("|").append(step.optional("expectedResult").orElse(""))
                        .appendRow("|");
            });
        });

        markdown.appendRow("\n### Вложения")
                .append("- ")
                .append(
                        markdown.optionalListMap("attachments")
                                .map(attas -> attas.stream()
                                        .map(a -> String.format("%s(%s)", a.get("id"), a.get("name")))
                                        .collect(Collectors.joining("\n- ")))
                                .orElse("нет вложений")
                );

        markdown.appendRow("\n\n### Выполнения");
        markdown.optionalMap("testResults").ifPresentOrElse(tResults -> {
            new OptionalMap<>(tResults).optionalListMap("data").ifPresent(data -> {
                if (data.isEmpty()) {
                    markdown.appendRow("- нет выполнений");
                    return;
                }
                markdown.appendRow("|Дата|Статус|Исполнитель|Прогон|Ключ|automated|")
                        .appendRow("|-|-|-|-|-|-|");
                data.forEach(execution -> {
                    Object date = execution.get("executionDate");
                    //noinspection unchecked
                    Object status = ((Map<String, Object>) execution.get("testResultStatus")).get("name");
                    Object userKey = execution.get("userKey");
                    //noinspection unchecked
                    Object runKey = ((Map<String, Object>) execution.get("testRun")).get("key");
                    Object key = execution.get("key");
                    Object automated = execution.get("automated");
                    markdown.append("|").append(date)
                            .append("|").append(status)
                            .append("|").append(userKey)
                            .append("|").append(runKey)
                            .append("|").append(key)
                            .append("|").append(automated)
                            .appendRow("|");
                });
            });
        }, () -> markdown.appendRow("- нет выполнений"));

        return markdown.toString();
    }


    public static class OptionalMap<K, V> extends LinkedHashMap<K, V> {

        public OptionalMap(Map<? extends K, ? extends V> m) {
            super(m);
        }

        public Optional<V> optional(K k) {
            return Optional.ofNullable(get(k));
        }

        public Optional<Map<String, Object>> optionalMap(K k) {
            //noinspection unchecked
            return Optional.ofNullable(get(k)).filter(v -> v instanceof Map<?, ?>).map(Map.class::cast);
        }

        public Optional<List<Map<String, Object>>> optionalListMap(K k) {
            //noinspection unchecked
            return Optional.ofNullable(get(k)).filter(v -> v instanceof List<?>).map(List.class::cast);
        }
    }


    public static class MarkdownMap<K, V> extends OptionalMap<K, V> {
        StringBuilder markdown = new StringBuilder();

        public MarkdownMap(Map<? extends K, ? extends V> m) {
            super(m);
        }

        public MarkdownMap<K, V> appendRow(String prefix, Object key) {
            Optional.ofNullable(get(key)).ifPresent(value -> append(prefix).appendRow(value));
            return this;
        }

        public MarkdownMap<K, V> appendRow(Object text) {
            return append(text).append("\n");
        }

        public MarkdownMap<K, V> append(Object text) {
            if (text != null)
                markdown.append(text);
            return this;
        }

        @Override
        public String toString() {
            return markdown.toString();
        }
    }


    public static String toMarkdownTree(TestTree tree, Function<TestTree.Test, String> stringifier) {
        return tree.getDescendants().stream()
                .map(test -> " ".repeat(test.getDepth() * 2) + "- " + stringifier.apply(test))
                .collect(Collectors.joining("\n"));
    }
}
