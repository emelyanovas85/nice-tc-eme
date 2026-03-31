package at.nice.tc.service;

import at.nice.tc.events.ToolEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 *
 */
@Service
@RequiredArgsConstructor
public class AiToolCallService {
    private final MemoryService memoryService;
    private final Map<Long, ToolEvent> updates = new ConcurrentHashMap<>();


    /**
     * Сигнал вьюхе использовать {@link #updates}
     */
    public void updateTool(String chatId, ToolEvent messageUpdate) {
        final long id = System.currentTimeMillis();
        updates.put(id, messageUpdate);
        memoryService.pushToken(chatId, TOOL_UPDATE.getPlaceholder() + id);
    }

    /**
     * Отправляет <tool> в чат
     */
    public void beginTool(String chatId) {
        memoryService.pushToken(chatId, TOOL_OPEN.getPlaceholder());
    }

    /**
     * Отправляет </tool> в чат
     */
    public void endTool(String chatId) {
        memoryService.pushToken(chatId, TOOL_CLOSE.getPlaceholder());
    }

    public Optional<ToolEvent> getUpdate(long timestamp) {
        return Optional.ofNullable(updates.get(timestamp));
    }

}
