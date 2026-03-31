package at.nice.tc.service;

import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Сервис для взаимодействия с AI моделями через Spring AI ChatClient.
 * Управляет отправкой сообщений и интеграцией с MemoryService для поддержки
 * множественных подписок и сохранения истории разговоров.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {
    private final ChatClient mainChatClient;
    private final ChatClient agentChatClient;
    private final MemoryService memoryService;


    /**
     * Отправляет пользовательское сообщение в основной чат.
     * Использует mainChatClient с доступом ко всем инструментам (tools).
     *
     * @param message текст сообщения от пользователя
     * @param chatId  уникальный идентификатор разговора
     * @return Flux токенов ответа AI модели с поддержкой множественных подписок
     */
    public Flux<String> sendMainMessageStream(String message, String chatId) {
        rateLimitGlobally();// ГЛОБАЛЬНАЯ задержка ПЕРЕД каждым запросом
        return sendMessageStream(mainChatClient, message, chatId);
    }

    /**
     * Отправляет агентское сообщение для проверки тест-кейса.
     * Использует agentChatClient без доступа к инструментам, данные передаются готовыми.
     *
     * @param message промпт для агента с данными теста
     * @param chatId  уникальный идентификатор разговора агента
     * @return Flux токенов ответа AI модели с поддержкой множественных подписок
     */
    public Flux<String> sendAgentMessageStream(String message, String chatId) {
        rateLimitGlobally();// ГЛОБАЛЬНАЯ задержка ПЕРЕД каждым запросом
        return sendMessageStream(agentChatClient, message, chatId);
    }

    /**
     * Главная точка интеграции с MemoryService и реактивными потоками.
     * Все токены от AI пушатся в MemoryService через Sinks.Many (replay),
     * что позволяет множеству подписчиков получать одни и те же данные:
     * - Несколько вкладок браузера на одном chatId
     * - Перезагрузка страницы с восстановлением всей истории токенов
     * - Параллельная подписка без дублирования запросов к AI
     *
     * @param chatClient     экземпляр ChatClient (main или agent)
     * @param message        текст сообщения для отправки AI модели
     * @param conversationId уникальный идентификатор чата для сохранения контекста
     * @return Flux токенов ответа, подключенный к MemoryService через replay sink
     */
    public Flux<String> sendMessageStream(ChatClient chatClient, String message, String conversationId) {
        // Получаем или создаем Sink для данного conversationId
        log.debug("String conversationId '{}'", conversationId);
        memoryService.getOrCreateSink(conversationId);

        // Запускаем генерацию ответа AI и пушим все токены в Sink
        chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .subscribe(
                        token -> memoryService.pushToken(conversationId, token),
                        error -> memoryService.errorStream(conversationId, error),
                        () -> memoryService.completeStream(conversationId)
                );

        // Возвращаем Flux для подписки - новые подписчики получат всю историю + новые токены
        return memoryService.subscribe(conversationId);
    }

    /**
     * Не дает выполнить действие (запрос к LLM), если с момента последнего обращения к нему (к RateLimiter)
     * за разрешением не прошло установленно время (время выражено в скорости 0,45 обращений в секунду)
     */
    private static final RateLimiter START_LIMITER = RateLimiter.create(0.45);

    /// каждые 2.2 секунды делаем обращение
    @SneakyThrows
    public static void rateLimitGlobally() {
        START_LIMITER.acquire();// Ждёт 2.2с между вызовами
    }
}
/// проверь тест VPEPVV-T2706

