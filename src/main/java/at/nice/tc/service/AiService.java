package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для взаимодействия с AI моделями через Spring AI ChatClient.
 * Управляет отправкой сообщений и интеграцией с MemoryService для поддержки
 * множественных подписок и сохранения истории разговоров.
 * 
 * Поддерживает ручное выполнение tool calls при internalToolExecutionEnabled = false.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient mainChatClient;
    private final ChatClient agentChatClient;
    private final MemoryService memoryService;
    private final ToolExecutionService toolExecutionService;
    
    // Максимальное количество итераций для защиты от бесконечных циклов
    private static final int MAX_TOOL_CALL_ITERATIONS = 10;

    /**
     * Отправляет пользовательское сообщение в основной чат.
     * Использует mainChatClient с доступом ко всем инструментам (tools).
     * 
     * @param message текст сообщения от пользователя
     * @param chatId уникальный идентификатор разговора
     * @return Flux токенов ответа AI модели с поддержкой множественных подписок
     */
    public Flux<String> sendMainMessageStream(String message, String chatId) {
        return sendMessageStream(mainChatClient, message, chatId);
    }

    /**
     * Отправляет агентское сообщение для проверки тест-кейса.
     * Использует agentChatClient без доступа к инструментам, данные передаются готовыми.
     * 
     * @param message промпт для агента с данными теста
     * @param chatId уникальный идентификатор разговора агента
     * @return Flux токенов ответа AI модели с поддержкой множественных подписок
     */
    public Flux<String> sendAgentMessageStream(String message, String chatId) {
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
     * Поддерживает обработку tool calls и продолжение conversation loop.
     * 
     * @param chatClient экземпляр ChatClient (main или agent)
     * @param message текст сообщения для отправки AI модели
     * @param conversationId уникальный идентификатор чата для сохранения контекста
     * @return Flux токенов ответа, подключенный к MemoryService через replay sink
     */
    public Flux<String> sendMessageStream(ChatClient chatClient, String message, String conversationId) {
        // Получаем или создаем Sink для данного conversationId
        memoryService.getOrCreateSink(conversationId);

        // Запускаем обработку с поддержкой tool calls
        processMessageWithToolCalls(chatClient, message, conversationId, 0);

        // Возвращаем Flux для подписки - новые подписчики получат всю историю + новые токены
        return memoryService.subscribe(conversationId);
    }

    /**
     * Обрабатывает сообщение с поддержкой tool calls и продолжения conversation loop.
     * 
     * @param chatClient экземпляр ChatClient
     * @param message текст сообщения или null для продолжения conversation
     * @param conversationId идентификатор разговора
     * @param iteration текущая итерация (для защиты от бесконечных циклов)
     */
    private void processMessageWithToolCalls(ChatClient chatClient, String message, 
                                            String conversationId, int iteration) {
        if (iteration >= MAX_TOOL_CALL_ITERATIONS) {
            log.warn("Достигнуто максимальное количество итераций tool calls для conversationId: {}", conversationId);
            memoryService.errorStream(conversationId, 
                new RuntimeException("Превышено максимальное количество итераций tool calls (" + MAX_TOOL_CALL_ITERATIONS + ")"));
            return;
        }

        Flux<ChatResponse> responseFlux;
        if (message != null) {
            // Первое сообщение от пользователя
            responseFlux = chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse();
        } else {
            // Продолжение conversation после tool calls - используем пустой промпт
            // ChatMemory автоматически добавит историю разговора
            responseFlux = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse();
        }

        // Собираем все ответы и обрабатываем tool calls после завершения потока
        responseFlux
                .doOnNext(response -> {
                    // Пушим каждый ответ в MemoryService для UI в реальном времени
                    memoryService.pushToken2(conversationId, response);
                })
                .collectList()
                .subscribe(
                    responses -> {
                        // После сбора всех ответов проверяем последний на наличие tool calls
                        if (!responses.isEmpty()) {
                            ChatResponse lastResponse = responses.get(responses.size() - 1);
                            if (hasToolCalls(lastResponse)) {
                                log.debug("Обнаружены tool calls в ответе, итерация: {}", iteration);
                                handleToolCalls(chatClient, lastResponse, conversationId, iteration);
                            } else {
                                // Нет tool calls - завершаем поток
                                log.debug("Tool calls не обнаружены, завершаем поток для conversationId: {}", conversationId);
                                memoryService.completeStream(conversationId);
                            }
                        } else {
                            memoryService.completeStream(conversationId);
                        }
                    },
                    error -> {
                        log.error("Ошибка при получении ответа от AI для conversationId: {}", conversationId, error);
                        memoryService.errorStream(conversationId, error);
                    }
                );
    }

    /**
     * Проверяет, содержит ли ChatResponse tool calls.
     */
    private boolean hasToolCalls(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults().isEmpty()) {
            return false;
        }
        Generation generation = chatResponse.getResult();
        return generation != null && generation.getOutput() != null 
                && generation.getOutput().hasToolCalls();
    }

    /**
     * Обрабатывает tool calls: выполняет их и продолжает conversation loop.
     */
    private void handleToolCalls(ChatClient chatClient, ChatResponse chatResponse, 
                                 String conversationId, int iteration) {
        Generation generation = chatResponse.getResult();
        if (generation == null || generation.getOutput() == null) {
            log.warn("Generation или Output пусты в ChatResponse");
            memoryService.completeStream(conversationId);
            return;
        }

        // Отправляем маркер tool call в UI
        memoryService.pushEventMarker(conversationId, "__TOOL_CALL__:start");

        // Выполняем tool calls
        List<ToolResponseMessage> toolResponses = toolExecutionService.executeToolCalls(
                generation.getOutput(), conversationId);

        // Отправляем результаты tool calls обратно в модель
        if (!toolResponses.isEmpty()) {
            log.debug("Отправка {} результатов tool calls обратно в модель", toolResponses.size());
            
            // Отправляем маркер завершения tool call
            memoryService.pushEventMarker(conversationId, "__TOOL_CALL__:end");

            // Продолжаем conversation с результатами tool calls
            // Используем ChatClient для отправки ToolResponseMessage
            // ChatMemory автоматически добавит историю разговора, включая AssistantMessage с tool calls
            chatClient.prompt()
                    .messages(toolResponses)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(Map.of(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        memoryService.pushToken2(conversationId, response);
                    })
                    .collectList()
                    .subscribe(
                        responses -> {
                            // После получения ответа продолжаем обработку
                            if (!responses.isEmpty()) {
                                ChatResponse lastResponse = responses.get(responses.size() - 1);
                                if (hasToolCalls(lastResponse)) {
                                    // Еще есть tool calls - продолжаем цикл
                                    handleToolCalls(chatClient, lastResponse, conversationId, iteration + 1);
                                } else {
                                    // Нет больше tool calls - завершаем
                                    memoryService.completeStream(conversationId);
                                }
                            } else {
                                memoryService.completeStream(conversationId);
                            }
                        },
                        error -> {
                            log.error("Ошибка при продолжении conversation после tool calls", error);
                            memoryService.errorStream(conversationId, error);
                        }
                    );
        } else {
            log.warn("Нет результатов tool calls для отправки");
            memoryService.completeStream(conversationId);
        }
    }
}
