package at.nice.tc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.UnaryOperator;

@Slf4j
@Data
public class LocalStorage {

    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper();


    public LocalStorage(Path root) {
        this.root = root;
        root.toFile().mkdirs();
    }

    public <T> UnaryOperator<T> saveAs(String key) {
        return obj -> {
            saveAs(key, obj);
            return obj;
        };
    }

    public void saveAs(String key, Object obj) {
        try {
            String value = mapper.writeValueAsString(obj);
            Files.writeString(root.resolve(key + ".json"), value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("Ошибка при маппинге в строку {}", key, e);
        }
    }

    public <T> T saved(String key) {
        try {
            File file = root.resolve(key + ".json").toFile();
            if (!file.exists())
                return null;
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("Ошибка получения сохраненного значения {}", key, e);
            return null;
        }
    }
}
