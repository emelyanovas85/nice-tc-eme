package at.nice.tc.service;

import at.nice.tc.events.ToolEvent;
import at.nice.tc.ui.MessageDelimiters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Service
@RequiredArgsConstructor
public class AiToolCallService {
    private final MemoryService memoryService;
    private final Map<Long, ToolEvent> updates = new ConcurrentHashMap<>();

    /**
     *
     */
    public void beginTool(String chatId) {
        memoryService.pushToken(chatId, MessageDelimiters.TOOL_OPEN);
    }

    /**
     * Сигнал вьюхе использовать {@link #updates}
     */
    public void updateTool(String chatId, ToolEvent messageUpdate) {
        final long id = System.currentTimeMillis();
        updates.put(id, messageUpdate);
        memoryService.pushToken(chatId, MessageDelimiters.TOOL_UPDATE + id);
    }

    /**
     *
     */
    public void endTool(String chatId) {
        memoryService.pushToken(chatId, MessageDelimiters.TOOL_CLOSE);
    }

    private Optional<ToolEvent> getUpdate(long timestamp) {
        return Optional.ofNullable(updates.get(timestamp));
    }

}
