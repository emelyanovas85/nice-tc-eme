package at.nice.tc.dao.ai;

import at.nice.tc.controller.AiController;
import at.nice.tc.dao.ai.client.ChatApiClient;
import at.nice.tc.utils.LocalStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Repository;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AiRepo implements AI {
    private final ChatApiClient chatApiClient;
    public static final LocalStorage LOCAL_STORAGE = new LocalStorage(Paths.get("ai"));

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void processBatch(String testId, List<AiController.CheckPrompt> checks) {

    }

    @Override
    public void sendChatMessage(String testId, String checkId, String message, Map<String, Object> placeholders) {

    }

    @Override
    public void stopProcessing(String testId, String checkId) {

    }
}
