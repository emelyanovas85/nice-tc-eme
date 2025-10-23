//package at.nice.tc.dao.ai.client;
//
//import at.nice.tc.dao.ai.dto.*;
//import com.fasterxml.jackson.core.type.TypeReference;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionException;
//
///**
// * 🎯 Специализированный клиент для Qwen3 32B/30B chat моделей.
// *
// * Дополнительные возможности:
// * • /v1/chat/completions - Продвинутые диалоги
// * • /v1/completions - Генерация текста
// * • /tokenize, /detokenize - Работа с токенами
// * • /v1/audio/transcriptions - Транскрипция аудио
// * • Tool calls и function calling
// */
//public class ChatApiClient extends BaseAIApiClient {
//
//    public ChatApiClient(String baseUrl, String apiKey, String modelName) {
//        super(baseUrl, apiKey, modelName);
//    }
//
//    // Convenience constructors for specific Qwen models
//    public static ChatApiClient qwen32B(String baseUrl, String apiKey) {
//        return new ChatApiClient(baseUrl, apiKey, "qwen3-32b-awq");
//    }
//
//    public static ChatApiClient qwen30B(String baseUrl, String apiKey) {
//        return new ChatApiClient(baseUrl, apiKey, "qwen3-30b-awq-4bit");
//    }
//
//    // === CHAT ENDPOINTS ===
//
//    /**
//     * 🔥 ОСНОВНОЕ: Создание чат-ответа.
//     * POST /v1/chat/completions - Диалог с AI моделью.
//     */
//    public Map<String, Object> createChatCompletion(ChatCompletionRequest request)  {
//        String json = toJson(request);
//        String response = POST("/v1/chat/completions", json).body();
//        return parseJson(response, new TypeReference<>() {});
//    }
//
//    /**
//     * Создание обычного completion (продолжение текста).
//     * POST /v1/completions - Генерация текста на основе промта.
//     */
//    public Map<String, Object> createCompletion(CompletionRequest request)  {
//        String json = toJson(request);
//        String response = POST("/v1/completions", json).body();
//        return parseJson(response, new TypeReference<>() {});
//    }
//
//    /**
//     * Токенизация текста или сообщений.
//     * POST /tokenize - Преобразование текста в токены.
//     */
//    public Map<String, Object> tokenize(TokenizeRequest request)  {
//        String json = toJson(request);
//        String response = POST("/tokenize", json).body();
//        return parseJson(response, new TypeReference<>() {});
//    }
//
//    /**
//     * Детокенизация токенов обратно в текст.
//     * POST /detokenize - Преобразование токенов в текст.
//     */
//    public Map<String, Object> detokenize(DetokenizeRequest request)  {
//        String json = toJson(request);
//        String response = POST("/detokenize", json).body();
//        return parseJson(response, new TypeReference<>() {});
//    }
//
//    /**
//     * Простой чат с одним сообщением пользователя.
//     */
//    public Map<String, Object> simpleChat(String userMessage, double temperature, int maxTokens)  {
//        List<ChatMessage> messages = List.of(ChatMessage.user(userMessage));
//        ChatCompletionRequest request = ChatCompletionRequest.forTesting(messages);
//        return createChatCompletion(request);
//    }
//
//    /**
//     * Чат с системным промтом.
//     */
//    public Map<String, Object> systemChat(String systemPrompt, String userMessage, double temperature, int maxTokens)  {
//        List<ChatMessage> messages = List.of(
//            ChatMessage.system(systemPrompt),
//            ChatMessage.user(userMessage)
//        );
//        ChatCompletionRequest request = ChatCompletionRequest.forTesting(messages);
//        return createChatCompletion(request);
//    }
//
//    // === ASYNC ВЕРСИИ ===
//
//    public CompletableFuture<Map<String, Object>> createChatCompletionAsync(ChatCompletionRequest request) {
//        return CompletableFuture.supplyAsync(() -> {
//            try { return createChatCompletion(request); }
//            catch (Exception e) { throw new CompletionException(e); }
//        });
//    }
//
//    public CompletableFuture<Map<String, Object>> createCompletionAsync(CompletionRequest request) {
//        return CompletableFuture.supplyAsync(() -> {
//            try { return createCompletion(request); }
//            catch (Exception e) { throw new CompletionException(e); }
//        });
//    }
//
//   /* public CompletableFuture<Map<String, Object>> createTranscriptionAsync(TranscriptionRequest request) {
//        return CompletableFuture.supplyAsync(() -> {
//            try { return createTranscription(request); }
//            catch (Exception e) { throw new CompletionException(e); }
//        });
//    }*/
//}
