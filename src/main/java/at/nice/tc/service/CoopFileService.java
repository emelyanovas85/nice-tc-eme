package at.nice.tc.service;

import at.nice.tc.model.CoopFile;
import at.nice.tc.model.impl.CoopFileImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для работы с промптом требований к ТК
 */
@Service
public class CoopFileService {

    @Autowired
    private final CoopFile promptFile = new CoopFileImpl("prompt.md");
    @Autowired
    private final CoopFile testFieldsFile = new CoopFileImpl("testcase_required_fields.txt");


    public CompletableFuture<String> getContentPrompt() {
        return getContent(promptFile);
    }

    public void updateContentPrompt(String content, UUID user) {
        updateContent(promptFile, content, user);
    }

    public CompletableFuture<String> getContentTestFields() {
        return getContent(testFieldsFile);
    }

    public void updateContentTestFields(String content, UUID user) {
        updateContent(testFieldsFile, content, user);
    }




    protected CompletableFuture<String> getContent(CoopFile file) {
        return CompletableFuture.supplyAsync(file::getContent);
    }

    protected void updateContent(CoopFile file, String newContent, UUID user) {
        CompletableFuture.runAsync(() -> {
            String oldContent = file.getContent();
            file.setContent(newContent);
            LISTENERS.forEach(l -> l.onContentUpdated(file, oldContent, newContent, user));
        });
    }


    public interface Listener {
        void onContentUpdated(CoopFile file, String oldValue, String newValue, UUID userBy);
    }



    public static final Set<Listener> LISTENERS = new HashSet<>(1);

    public static void addListener(Listener l) {
        LISTENERS.add(l);
    }

}
