package at.nice.tc.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Component
public class SpringUtils {

    public void logClasspathRoots() {
        try {
            Enumeration<URL> roots = getClass().getClassLoader().getResources("");
            log.info("Класс-пассы в системе:");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                log.info(" - {}", url);
            }
        } catch (IOException e) {
            log.error("Ошибка при чтении classpath", e);
        }
    }

    public InputStream tryLoadResource(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath); // <-- здесь
        if (is == null) {
            log.error("Не найден ресурс {} через getClass().getClassLoader()", resourcePath);
        } else {
            log.info("Ресурс {} успешно найден через getClass().getClassLoader()", resourcePath);
        }
        return is;
    }

    public List<String> readResourceLines(String resourcePath) {
        log.info("Версия 5________________________________________");

        logClasspathRoots();

        try (InputStream is = tryLoadResource(resourcePath)) {
            if (is == null) {
                throw new IOException("Ресурс не найден: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .filter(line -> !line.trim().isEmpty() && !line.startsWith("//"))
                        .toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
