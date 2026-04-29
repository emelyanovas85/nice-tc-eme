package at.nice.tc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class MarkdownSaveService {

    @Value("${app.ready-dir:src/main/resources/example/ready}")
    private String readyDir;

    /**
     * Сохраняет markdown-контент в файл {testCaseId}.md в директории app.ready-dir
     *
     * @param testCaseId     ID тест-кейса (например, VPEPVV-T2834) — используется как имя файла
     * @param markdownContent текст ответа ассистента в формате Markdown
     * @return путь к сохранённому файлу
     */
    public Path save(String testCaseId, String markdownContent) throws IOException {
        Path dir = Paths.get(readyDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(testCaseId + ".md");
        Files.writeString(file, markdownContent);
        log.info("Сохранён файл ответа: {}", file.toAbsolutePath());
        return file;
    }
}
