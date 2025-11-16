package at.nice.tc.service;

import at.nice.tc.events.ChatEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class EventService {

    // Хранилище подписчиков conversationId -> множества Consumer<ChatEvent>
    private final Map<String, Set<Consumer<ChatEvent>>> subscribers = new ConcurrentHashMap<>();

    /**
     * Подписка UI/чата на события по conversationId (aka chatId).
     * @return отписка (Registration pattern)
     */
    public Registration subscribe(String conversationId, Consumer<ChatEvent> handler) {
        subscribers.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(handler);
        log.debug("Подписан слушатель событий из чата {}", conversationId);
        return () -> {
            Set<Consumer<ChatEvent>> set = subscribers.get(conversationId);
            if (set != null) set.remove(handler);
        };
    }

    /**
     * Вызывается любым {@link ChatEvent} и отправляет это событие всем подписчикам
     */
    @EventListener
    public void broadcastEvent(ChatEvent event) {
        String conversationId = event.getConversationId();
        Set<Consumer<ChatEvent>> set = subscribers.get(conversationId);
        if (set == null || set.isEmpty())
            return;
        for (Consumer<ChatEvent> c : set) {
            try {
                c.accept(event);
            } catch (Throwable t) {
                log.error("Ошибка в обработчике событий ChatView для чата {}", conversationId, t);
            }
        }
    }

    /**
     * Коллбэк для отписки
     */
    public interface Registration {
        void unsubscribe();
    }
}
