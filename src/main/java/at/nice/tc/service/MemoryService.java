package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления памятью чатов и реактивными потоками ответов.
 * <p>
 * Использует {@link Sinks.Many} с replay для поддержки множественных подписок
 * на один поток ответа. Позволяет:
 * <ul>
 *   <li>Открывать несколько вкладок браузера на одном chatId</li>
 *   <li>Перезагружать страницу с восстановлением истории токенов</li>
 *   <li>Подписываться на ответ в любой момент генерации</li>
 * </ul>
 * <p>
 * Также управляет {@link ChatMemory} для сохранения истории сообщений
 * в долгосрочной памяти.
 * <p>
 * <b>Raw-история:</b> Spring AI {@link ChatMemory} сохраняет ответ модели БЕЗ тегов
 * {@code <think>...</think>} — они вырезаются как reasoning-артефакты стриминга.
 * Поэтому MemoryService дополнительно хранит сырой текст каждого ответа ассистента
 * (с тегами) в {@link #rawAssistantMessages}, чтобы при F5 блок «Размышления модели»
 * восстанавливался корректно через {@link #getRawAssistantMessages(String)}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {
    private final ChatMemory chatMemory;

    /**
     * Хранит {@link Sinks.Many} с replay для каждого conversationId.
     */
    private final Map<String, Sinks.Many<String>> replaySinks = new ConcurrentHashMap<>();

    /**
     * Хранит сырые тексты ответов ассистента (включая теги &lt;think&gt;) для каждого conversationId.
     * Используется при восстановлении UI после F5, так как ChatMemory хранит текст без тегов.
     */
    private final Map<String, List<String>> rawAssistantMessages = new ConcurrentHashMap<>();

    /**
     * Буфер токенов текущего активного ответа — накапливается до завершения стрима,
     * затем сохраняется в rawAssistantMessages.
     */
    private final Map<String, StringBuilder> activeRawBuffers = new ConcurrentHashMap<>();

    public Sinks.Many<String> getOrCreateSink(String conversationId) {
        return replaySinks.computeIfAbsent(conversationId, id -> {
            log.debug("Создан новый Sink для conversationId: {}", id);
            return Sinks.many().replay().all();
        });
    }

    public Flux<String> subscribe(String conversationId) {
        Sinks.Many<String> sink = getOrCreateSink(conversationId);
        log.debug("Подписка на поток для conversationId: {}", conversationId);
        return sink.asFlux();
    }

    public void pushToken(String conversationId, String token) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitNext(token);
            // Накапливаем raw-буфер для сохранения после завершения
            activeRawBuffers.computeIfAbsent(conversationId, id -> new StringBuilder()).append(token);
            log.trace("Токен отправлен для conversationId: {}", conversationId);
        } else {
            log.warn("Sink не найден для conversationId: {}", conversationId);
        }
    }

    public void completeStream(String conversationId) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
            replaySinks.remove(conversationId);
            log.debug("Поток завершен для conversationId: {}", conversationId);
        }
        // Сохраняем накопленный raw-текст ответа (с <think> тегами)
        StringBuilder rawBuffer = activeRawBuffers.remove(conversationId);
        if (rawBuffer != null && !rawBuffer.isEmpty()) {
            rawAssistantMessages
                    .computeIfAbsent(conversationId, id -> new ArrayList<>())
                    .add(rawBuffer.toString());
            log.debug("Raw-ответ сохранён для conversationId: {} ({} символов)", conversationId, rawBuffer.length());
        }
    }

    public void errorStream(String conversationId, Throwable error) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitError(error);
            log.debug("Поток завершен с ошибкой для conversationId: {}", conversationId, error);
        }
        activeRawBuffers.remove(conversationId);
    }

    public boolean hasActiveStream(String conversationId) {
        return replaySinks.containsKey(conversationId);
    }

    public List<Message> getCompletedMessages(String conversationId) {
        try {
            List<Message> messages = chatMemory.get(conversationId);
            return messages != null ? messages : List.of();
        } catch (Exception e) {
            log.error("Ошибка при получении сообщений для conversationId: {}", conversationId, e);
            return List.of();
        }
    }

    /**
     * Возвращает сохранённые сырые тексты ответов ассистента (с тегами &lt;think&gt;)
     * в порядке поступления. Используется в {@code ChatView.Restorer} для корректного
     * восстановления блока «Размышления модели» после перезагрузки страницы.
     *
     * @param conversationId уникальный идентификатор разговора
     * @return список raw-ответов или пустой список
     */
    public List<String> getRawAssistantMessages(String conversationId) {
        return rawAssistantMessages.getOrDefault(conversationId, List.of());
    }

    public boolean hasInMemory(String conversationId) {
        boolean hasStream = replaySinks.containsKey(conversationId);
        boolean hasMessages = !getCompletedMessages(conversationId).isEmpty();
        return hasStream || hasMessages;
    }

    public void clearMessages(String conversationId) {
        try {
            chatMemory.clear(conversationId);
            log.debug("История сообщений очищена для conversationId: {}", conversationId);
        } catch (Exception e) {
            log.error("Ошибка при очистке сообщений для conversationId: {}", conversationId, e);
        }
    }

    public void removeConversation(String conversationId) {
        Sinks.Many<String> sink = replaySinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        activeRawBuffers.remove(conversationId);
        rawAssistantMessages.remove(conversationId);
        clearMessages(conversationId);
        log.debug("Данные полностью удалены для conversationId: {}", conversationId);
    }
}
