package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {
    private final ChatMemory chatMemory;

    /**
     * Хранит {@link Sinks.Many} с replay для каждого conversationId.
     * <p>
     * Sinks.many().replay().all() позволяет:
     * <ul>
     *   <li>Множественным подписчикам получать одни и те же данные</li>
     *   <li>Новым подписчикам получать всю историю токенов</li>
     *   <li>Переподключаться после перезагрузки страницы</li>
     * </ul>
     */
    private final Map<String, Sinks.Many<String>> replaySinks = new ConcurrentHashMap<>();

    /**
     * Получает или создает Sink для указанного conversationId.
     * <p>
     * Все подписчики на этот conversationId будут получать одни и те же токены.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @return Sink для публикации токенов
     */
    public Sinks.Many<String> getOrCreateSink(String conversationId) {
        return replaySinks.computeIfAbsent(conversationId, id -> {
            log.debug("Создан новый Sink для conversationId: {}", id);
            return Sinks.many().replay().all();
        });
    }

    /**
     * Подписывается на поток токенов для указанного conversationId.
     * <p>
     * Новые подписчики получат всю историю токенов + новые в реальном времени.
     * Используется для подключения UI компонентов к активному потоку ответа.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @return Flux с историей и новыми токенами
     */
    public Flux<String> subscribe(String conversationId) {
        Sinks.Many<String> sink = getOrCreateSink(conversationId);
        log.debug("Подписка на поток для conversationId: {}", conversationId);
        return sink.asFlux();
    }

    /**
     * Отправляет токен во все активные подписки для указанного conversationId.
     * <p>
     * Используется {@link AiService} для публикации токенов,
     * полученных от AI модели, во все подключенные UI компоненты.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @param token токен для отправки
     */
    public void pushToken(String conversationId, String token) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitNext(token);
            log.trace("Токен отправлен для conversationId: {}", conversationId);
        } else {
            log.warn("Sink не найден для conversationId: {}", conversationId);
        }
    }

    /**
     * Завершает поток для указанного conversationId.
     * <p>
     * После завершения новые подписчики получат всю историю,
     * но новые токены не будут приниматься.
     * 
     * @param conversationId уникальный идентификатор разговора
     */
    public void completeStream(String conversationId) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
            replaySinks.remove(conversationId);
            log.debug("Поток завершен для conversationId: {}", conversationId);
        }
    }

    /**
     * Завершает поток с ошибкой для указанного conversationId.
     * <p>
     * Ошибка будет передана всем текущим и будущим подписчикам.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @param error ошибка, которую нужно передать
     */
    public void errorStream(String conversationId, Throwable error) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        if (sink != null) {
            sink.tryEmitError(error);
            log.debug("Поток завершен с ошибкой для conversationId: {}", conversationId, error);
        }
    }

    /**
     * Проверяет, есть ли активный поток для указанного conversationId.
     * <p>
     * Sink считается активным, если он существует в кэше и не завершен.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @return true, если поток активен
     */
    public boolean hasActiveStream(String conversationId) {
        Sinks.Many<String> sink = replaySinks.get(conversationId);
        return sink != null;
    }

    /**
     * Возвращает сообщения из истории долгосрочной памяти (ChatMemory).
     * <p>
     * История сохраняется Spring AI и может быть использована
     * для восстановления контекста разговора после перезапуска приложения.
     * 
     * @param conversationId уникальный идентификатор разговора
     * @return список сообщений или пустой список
     */
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
     * Проверяет, есть ли сохраненные данные для conversationId.
     * <p>
     * Данные считаются существующими, если:
     * <ul>
     *   <li>Есть активный поток (Sink) для conversationId</li>
     *   <li>Есть завершенные сообщения в ChatMemory</li>
     * </ul>
     * 
     * @param conversationId уникальный идентификатор разговора
     * @return true, если есть данные
     */
    public boolean hasInMemory(String conversationId) {
        boolean hasStream = replaySinks.containsKey(conversationId);
        boolean hasMessages = !getCompletedMessages(conversationId).isEmpty();
        return hasStream || hasMessages;
    }

    /**
     * Очищает историю сообщений для указанного conversationId в ChatMemory.
     * <p>
     * Не влияет на активный поток токенов (Sink).
     * Используется для сброса контекста перед новой проверкой.
     * 
     * @param conversationId уникальный идентификатор разговора
     */
    public void clearMessages(String conversationId) {
        try {
            chatMemory.clear(conversationId);
            log.debug("История сообщений очищена для conversationId: {}", conversationId);
        } catch (Exception e) {
            log.error("Ошибка при очистке сообщений для conversationId: {}", conversationId, e);
        }
    }

    /**
     * Полностью удаляет все данные для conversationId:
     * <ul>
     *   <li>Завершает активный поток (Sink)</li>
     *   <li>Удаляет Sink из кэша</li>
     *   <li>Очищает историю сообщений в ChatMemory</li>
     * </ul>
     * Используйте для полной очистки разговора.
     * 
     * @param conversationId уникальный идентификатор разговора
     */
    public void removeConversation(String conversationId) {
        Sinks.Many<String> sink = replaySinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        clearMessages(conversationId);
        log.debug("Данные полностью удалены для conversationId: {}", conversationId);
    }
}
