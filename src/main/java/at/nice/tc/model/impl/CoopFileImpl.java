package at.nice.tc.model.impl;

import at.nice.tc.model.CoopFile;
import at.nice.tc.utils.ThrowableUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

public class CoopFileImpl implements CoopFile {
    private final AtomicReference<String> text = new AtomicReference<>();
    private final String fileName;

    public CoopFileImpl(String fileName) {
        this.fileName = fileName;
        read();
    }

    private synchronized void read() {
        ClassPathResource resource = new ClassPathResource(fileName);
        try {
            if (resource.exists()) {
                String content = Files.readString(resource.getFile().toPath());
                text.set(content);
            } else {
                text.set("");
            }
        } catch (IOException e) {
            ThrowableUtils.reThrow(e);
        }
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public synchronized String getContent() {
        return text.get();
    }

    @Override
    public synchronized void setContent(String content) {
        text.set(content);
        write();
    }

    private synchronized void write() {
        ClassPathResource resource = new ClassPathResource("prompt.md");
        try {
            Path filePath = resource.getFile().toPath();
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, text.get() != null ? text.get() : "", StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            ThrowableUtils.reThrow(e);
        }
    }
}
