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


    public ResultBuilder getSaved(String key) {
        File file = root.resolve(key + ".json").toFile();
        return this.new ResultBuilder(key, file);
    }

    @Data
    public class ResultBuilder {
        private final String key;
        private final File file;

        public <T> T as(Class<T> cls) {
            try {
                if (!file.exists())
                    return null;
                return mapper.readValue(file, cls);
            } catch (IOException e) {
                log.warn("Ошибка получения сохраненного значения {}", key, e);
                return null;
            }
        }

        public <T> T as(TypeReference<T> target) {
            try {
                if (!file.exists())
                    return null;
                return mapper.readValue(file, target);
            } catch (IOException e) {
                log.warn("Ошибка получения сохраненного значения {}", key, e);
                return null;
            }
        }
    }
}
