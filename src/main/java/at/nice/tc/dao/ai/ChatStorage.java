package at.nice.tc.dao.ai;

import at.nice.tc.dao.ai.dto.ChatMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Getter
@RequiredArgsConstructor
public class ChatStorage {
    private final ConcurrentHashMap<String, Queue<ChatMessage>> storage;

    public Queue<ChatMessage> getChatById(@NotNull String chatId) {
        return storage.get(chatId);
    }

    public boolean putMessage(@NotNull String chatId, @NotNull ChatMessage message) {
        return storage.containsKey(chatId) && storage.get(chatId).add(message);
    }

    public ChatMessage getLastMessage(@NotNull String chatId) {
        return storage.get(chatId).peek();
    }
}
