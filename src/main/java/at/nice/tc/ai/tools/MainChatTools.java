package at.nice.tc.ai.tools;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.utils.ThrowableUtils;
import at.nice.tc.utils.ToolUtils;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static at.nice.tc.utils.JiraUtils.MAPPER;
import static java.util.stream.Collectors.groupingBy;


@Component
@Slf4j
@RequiredArgsConstructor
public class MainChatTools {

    private final TestCheckersAggregator aggregatorChecker;
    private final ToolEventPublisher.Factory publisherFactory;


    /// проверь тест VPEPVV-T2706
    /// проверь тест VPEPVV-T800
    /// проверь тест EHDRUONIA-T9

    @Tool(name = "checkTestCaseByRequirements",
            description = "Получение необходимых для данных в формате json c результатами проверки верхнеуровнего" +
                    " (основного) тест-кейса и вложенных в него тест-кейсов.")
    public String checkTestCaseByRequirements(
            @ToolParam(description = "Ключ или ID верхнеуровнего (основного) тест-кейса для проверки")
            String keyTestCase,
            ToolContext toolContext) {
        String chatId = ToolUtils.getConversationId(toolContext);

        ToolEventPublisher publisher = publisherFactory.forConversation(chatId);
        publisher.eventPublisher().beginTool(chatId);


        try {
            List<String> results = new CopyOnWriteArrayList<>();
            aggregatorChecker.startChecks(keyTestCase, publisher)
                    .thenApply(futures -> futures
                            .stream()
                            .map(future -> future
                                    .exceptionally(ex -> "Не удалось проверить тест " + keyTestCase +
                                            ". Ошибка: " + ThrowableUtils.asString(ex))
                                    .thenAccept(results::add)
                            )
                            .toList()
                    )
                    .thenCompose(futures -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])))
                    .join();

            List<Map<String, Object>> merged = new ArrayList<>();





            String jsonArray = "[" + String.join(",", results) + "]";

//            List<Map<String, Object>> map = null;

            try {
                for (String result : results) {
                    if (result == null || result.isBlank()) continue;
                    Map<String, Object> map = MAPPER.readValue(result, new TypeReference<>() {});
                    merged.add(map);
                }
            } catch (JsonProcessingException e) {
//                logBrokenJsonContext(merged, e);
            }

            List<Map<String, Object>> jsonTable2 = jsonTable2(keyTestCase, merged);
            List<Map<String, Object>> jsonTable3 = jsonTable3(merged);


            int size = jsonTable3.size();
            int sum = jsonTable3.stream()
                    .map(b -> (Integer) b.get("value"))
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            return jsonArray +
                    "\n\njson для формирования таблицы 2:\n\n" + jsonTable2 +
                    "\n\njson для формирования таблицы 3:\n\n" + jsonTable3 +
                    "\n\nДанные для блока 'Сводка результатов':\n\n" +
                    "\nКоличество требований: " + size +
                    "\nКоличество ✅: " + sum +
                    "\nКоличество ❌: " + (size - sum);

        } finally {
            publisher.eventPublisher().endTool(chatId);
        }
    }
    private static void logBrokenJsonContext(String json, JsonProcessingException e) {
        JsonLocation loc = e.getLocation();
        int offset = (int) loc.getCharOffset();   // позиция символа 'В'
        int radius = 120;

        int from = Math.max(0, offset - radius);
        int to = Math.min(json.length(), offset + radius);

        String context = json.substring(from, to);
        int caretPos = offset - from;

        StringBuilder marker = new StringBuilder();
        for (int i = 0; i < caretPos; i++) marker.append(' ');
        marker.append('^');

        log.error(
                "JSON parse error at line {}, column {} (char {}): {}\n" +
                        "Context ({}..{} of {}):\n{}\n{}\n",
                loc.getLineNr(), loc.getColumnNr(), offset, e.getOriginalMessage(),
                from, to, json.length(),
                context,
                marker
        );
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> jsonTable2(String keyTestCase, List<Map<String, Object>> map) {
        return map.stream()
                .filter(s -> s.get("key").equals(keyTestCase))
                .flatMap(s -> ((List<Map<String, Object>>) s.getOrDefault("requirements", List.of()))
                        .stream()
                        .filter(m -> m.containsKey("comments"))
                        .filter(m -> {
                            List<?> comments = (List<?>) m.get("comments");
                            return comments.stream()
                                    .anyMatch(item -> item instanceof Map && ((Map<?, ?>) item).containsKey("step_number"));
                        }))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> jsonTable3(List<Map<String, Object>> map) {
        List<Map<String, Object>> allItems = map.stream()
                .flatMap(item -> {
                    List<Map<String, Object>> items = new ArrayList<>();
                    if (item.containsKey("requirements")) {
                        items.addAll((List<Map<String, Object>>) item.get("requirements"));
                    }
                    if (item.containsKey("results")) {
                        items.addAll((List<Map<String, Object>>) item.get("results"));
                    }
                    return items.stream();
                })
                .toList();

        Map<String, List<Map<String, Object>>> groupedByReqNumber = allItems.stream()
                .collect(groupingBy(item -> (String) item.get("req_number")));

        List<Map<String, Object>> transformedResults = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByReqNumber.entrySet()) {
            String reqNumber = entry.getKey();
            List<Map<String, Object>> itemsWithSameReq = entry.getValue();

            Integer minValue = itemsWithSameReq.stream()
                    .map(b -> (Integer) b.get("value"))
                    .filter(Objects::nonNull)
                    .min(Integer::compareTo)
                    .orElseThrow(() -> new RuntimeException("Отсутствуют значения 'value' в json:\n\n" + entry));


            List<Map<String, String>> allComments = new ArrayList<>();
            for (Map<String, Object> item : itemsWithSameReq) {
                Object commentsObj = item.get("comments");
                if (commentsObj instanceof List) {
                    List<?> comments = (List<?>) commentsObj;

                    for (Object comment : comments) {
                        Map<String, String> commentMap = new HashMap<>();

                        if (comment instanceof String) {
                            commentMap.put("comments", (String) comment);
                            allComments.add(commentMap);
                        } else if (comment instanceof Map<?, ?> commentWithStep) {
                            if (commentWithStep.containsKey("comment")) {
                                commentMap.put("comments", (String) commentWithStep.get("comment"));
                                allComments.add(commentMap);
                            }
                        }
                    }
                }
            }

            Map<String, Object> resultItem = new LinkedHashMap<>();
            resultItem.put("req_number", reqNumber);
            resultItem.put("value", minValue);
            resultItem.put("comments", allComments);

            transformedResults.add(resultItem);
        }

        transformedResults.sort(Comparator.comparing(item -> (String) item.get("req_number"), Comparator.nullsLast(Comparator.naturalOrder())));
        return transformedResults;
    }
}
