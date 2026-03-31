package at.nice.tc.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Утилита для записи JSON в файл, если файл не существует.
 * Предотвращает случайное перезаписывание существующих данных.
 */
public class JsonLLMResponseFileWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final String DEFAULT_DIRECTORY = "src/main/resources/example/jsonByLLM";

    /**
     * Сохраняет JSON массив в файл, если файл не существует.
     *
     * @param jsonArray JSON строка для сохранения
     * @param filename имя файла (без пути)
     * @return true если файл создан, false если файл уже существует
     * @throws RuntimeException при ошибках ввода-вывода
     */
    public static boolean saveIfNotExists(String jsonArray, String filename) {
        return saveIfNotExists(jsonArray, filename, DEFAULT_DIRECTORY);
    }

    /**
     * Сохраняет JSON массив в файл, если файл не существует.
     *
     * @param jsonArray JSON строка для сохранения
     * @param filename имя файла (без пути)
     * @param directory директория для сохранения
     * @return true если файл создан, false если файл уже существует
     * @throws RuntimeException при ошибках ввода-вывода
     */
    public static boolean saveIfNotExists(String jsonArray, String filename, String directory) {
        try {
            // Создаем директорию если её нет
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Полный путь к файлу
            Path filePath = dirPath.resolve(filename);
            File file = filePath.toFile();

            // Проверяем существует ли файл
            if (file.exists()) {
                return false;
            }

            // Валидируем JSON перед сохранением
            if (!isValidJson(jsonArray)) {
                throw new RuntimeException("Невалидный JSON");
            }

            // Сохраняем файл
            Files.write(filePath, jsonArray.getBytes());
            return true;

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении файла: " + e.getMessage(), e);
        }
    }


    /**
     * Проверяет, существует ли файл.
     *
     * @param filename имя файла
     * @return true если файл существует
     */
    public static boolean exists(String filename) {
        return exists(filename, DEFAULT_DIRECTORY);
    }

    /**
     * Проверяет, существует ли файл.
     *
     * @param filename имя файла
     * @param directory директория
     * @return true если файл существует
     */
    public static boolean exists(String filename, String directory) {
        Path filePath = Paths.get(directory, filename);
        return Files.exists(filePath);
    }

    /**
     * Читает JSON из файла.
     *
     * @param filename имя файла
     * @return содержимое файла как строка
     */
    public static String readFromFile(String filename) {
        return readFromFile(filename, DEFAULT_DIRECTORY);
    }

    /**
     * Читает JSON из файла.
     *
     * @param filename имя файла
     * @param directory директория
     * @return содержимое файла как строка
     */
    public static String readFromFile(String filename, String directory) {
        try {
            Path filePath = Paths.get(directory, filename);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Файл не найден: " + filePath);
            }
            return new String(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файла: " + e.getMessage(), e);
        }
    }

    /**
     * Валидирует JSON строку.
     *
     * @param json JSON строка
     * @return true если JSON валидный
     */
    private static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Получает список всех сохраненных JSON файлов.
     *
     * @return массив имен файлов
     */
    public static String[] listFiles() {
        return listFiles(DEFAULT_DIRECTORY);
    }

    /**
     * Получает список всех сохраненных JSON файлов.
     *
     * @param directory директория
     * @return массив имен файлов
     */
    public static String[] listFiles(String directory) {
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                return new String[0];
            }

            return Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .toArray(String[]::new);

        } catch (IOException e) {
            return new String[0];
        }
    }
}
