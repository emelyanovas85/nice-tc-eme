package at.nice.tc.ai.tools;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.utils.ThrowableUtils;
import at.nice.tc.utils.ToolUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MainChatTools {

    private final TestCheckersAggregator aggregatorChecker;
    private final ToolEventPublisher.Factory publisherFactory;


    /// проверь тест VPEPVV-T2706
    /// проверь тест VPEPVV-T800
    /// проверь тест EHDRUONIA-T9

    @Tool(name = "checkTestCaseByRequirements",
            description = "Получение необходимых для данных в формате json c результатами проверки верхнеуровнего" +
                    " (основного) тест-кейса и вложенных в него тест-кейсов. Необходим для выполнения ")
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

            String jsonArray = "[" + String.join(",", results).trim().replaceAll("\\n\\s+", "") + "]";

            List<Map<String, Object>> map;

            try {
                map = MAPPER.readValue(jsonArray, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            List<Map<String, Object>> jsonTable2 = jsonTable2(keyTestCase, map);
            List<Map<String, Object>> jsonTable3 = jsonTable3(map);


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
    //таб 2
//    public static void main(String[] args) {
//        InputStream inputStream = MainChatTools.class.getResourceAsStream("/example/jsonByLLM/VPEPVV_T800_11_new.json");
//        String maps = null;
//        try {
//            maps = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        final ObjectMapper MAPPER = new ObjectMapper();
//        List<Map<String, Object>> map;
//
//        try {
//            map = MAPPER.readValue(maps, new TypeReference<>() {});
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        List<Map<String, Object>> requirementsWithStepComments = map.stream()
//                .filter(s -> "VPEPVV-T800".equals(s.get("key")))
//                .flatMap(s -> ((List<Map<String, Object>>) s.getOrDefault("requirements", List.of()))
//                        .stream()
//                        .filter(m -> m.containsKey("comments"))
//                        .filter(m -> {
//                            List<?> comments = (List<?>) m.get("comments");
//                            return /*!comments.isEmpty() &&*/
//                                    comments.stream().anyMatch(item ->
//                                            item instanceof Map && ((Map<?, ?>) item).containsKey("step_number"));
//                        }))
//                .toList();
//
//        System.out.println(requirementsWithStepComments);
//    }
    //таб 3
//    public static void main(String[] args) {
//        // Чтение файла
//        InputStream inputStream = MainChatTools.class.getResourceAsStream("/example/jsonByLLM/VPEPVV_T2706_3_new.json");
//        String maps = null;
//        try {
//            maps = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        final ObjectMapper MAPPER = new ObjectMapper();
//        List<Map<String, Object>> map;
//
//        try {
//            map = MAPPER.readValue(maps, new TypeReference<>() {});
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Собираем все требования и результаты
//        List<Map<String, Object>> transformedResults = jsonTable3(map);
//
//
//        System.out.println(transformedResults);
//    }
