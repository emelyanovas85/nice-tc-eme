package at.nice.tc.utils;

import at.nice.tc.Application;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.vaadin.uitest.parser.Parser.objectMapper;

/**
 * Проверяет согласованность оценок (value) в JSON массивах.
 * Сравнивает с эталонным образцом и отслеживает изменения.
 *
 * @author ScoreChecker
 * @version 2.0
 */
@Service
public class ScoreChecker {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static ResourcePatternResolver resolver;

    @Autowired
    public void setResolver(ResourcePatternResolver resolver) {
        ScoreChecker.resolver = resolver;
    }
    private static final String FOLDER_PATH = "example/jsonByLLM";

    /**
     * Ищет файл по имени в указанной папке
     * @param fileName имя файла
     * @return File объект если найден, null если не найден
     */
    public Resource findReferenceJSONByLLM(String fileName) {

        try {
            Resource[] resources = resolver.getResources("classpath*:" + FOLDER_PATH + "/**/" + fileName);
            return resources.length > 0 ? resources[0] : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Парсит Resource в List<Map<String, Object>>
     * @param resource найденный ресурс
     * @return список карт или пустой список при ошибке
     */
    @Nullable
    public List<Map<String, Object>> parseJsonToListOfMaps(Resource resource) {
        if (resource == null || !resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Записывает String в JSON файл в resources папку (аналог findReferenceJSONByLLM)
     * @param fileName имя файла (data.json)
     * @param jsonString содержимое JSON
     * @return Resource созданного файла
     */
    public void writeJsonToResources(String fileName, String jsonString) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path resourcesPath = Paths.get(projectRoot, "src", "main", "resources", FOLDER_PATH);
            Files.createDirectories(resourcesPath);
            Path filePath = resourcesPath.resolve(fileName);
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(jsonString);
            }

            Application.restart();//todo заменить на что-то другое (пока для отладки промптов)


        } catch (Exception e) {
            throw new RuntimeException("Ошибка записи в resources/" + FOLDER_PATH + "/" + fileName, e);
        }
    }

    /**
     * Сравнивает только value во ВСЕХ вложенных Map.
     * Структура одинаковая = сравнивает по позициям.
     * Кидает ошибку с путем к расхождению.
     */
    /**
     * ✅ ВАЛИДАЦИЯ (исправленная версия)
     */
    public void validateScores (List<Map<String, Object>> actual, List<Map<String, Object>> expected, String testKey) {
        if (actual.size() != expected.size()) {
            throw new RuntimeException(testKey + ": количество секций не совпадает: "
                    + actual.size() + " vs " + expected.size());
        }

        for (int sec = 0; sec < actual.size(); sec++) {
            Map<String, Object> actSec = actual.get(sec);
            Map<String, Object> expSec = expected.get(sec);
            compareMaps(actSec, expSec, testKey, "секция[" + (sec + 1) + "]");
        }
    }

    private void compareMaps(Map<String, Object> actual, Map<String, Object> expected,
                             String testKey, String path) {
        for (String key : expected.keySet()) {
            Object expVal = expected.get(key);
            Object actVal = actual.get(key);

            if (expVal instanceof List<?> expList && actVal instanceof List<?> actList) {
                if (expList.size() != actList.size()) {
                    throw new RuntimeException(testKey + ": " + path + "." + key +
                            " размер списка: " + actList.size() + " vs " + expList.size());
                }

                for (int i = 0; i < expList.size(); i++) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> actMap = (Map<String, Object>) actList.get(i);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> expMap = (Map<String, Object>) expList.get(i);

                    compareMaps(actMap, expMap, testKey, path + "." + key + "[" + i + "]");
                }
            } else if ("value".equals(key)) {
                int actValue = ((Number) actVal).intValue();
                int expValue = ((Number) expVal).intValue();

                if (actValue != expValue) {
                    throw new RuntimeException(String.format(
                            "ТК %s: %s.value ожидается %d, получено %d",
                            testKey, path, expValue, actValue));
                }
            }
        }
    }
}