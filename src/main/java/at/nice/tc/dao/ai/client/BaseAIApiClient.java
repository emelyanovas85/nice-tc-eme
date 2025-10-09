package at.nice.tc.dao.ai.client;

import at.nice.tc.dao.ai.dto.AIApiException;
import at.nice.tc.dao.ai.dto.EmbeddingRequest;
import at.nice.tc.dao.ai.dto.RerankRequest;
import at.nice.tc.dao.ai.dto.ScoreRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Базовый AI API клиент с общими endpoints для всех моделей.
 *
 * Поддерживает endpoints, доступные во ВСЕХ 5 моделях:
 * • Health checks и метрики
 * • Tokenization операции
 * • Базовые completions
 * • Embeddings (базовые)
 * • Audio transcription
 * • Score и rerank операции
 */
@Getter
public class BaseAIApiClient {
    protected final HttpClient httpClient;
    protected final String baseUrl;
    protected final String apiKey;
    protected final ObjectMapper objectMapper;
    protected final String modelName;

    public BaseAIApiClient(String baseUrl, String apiKey, String modelName) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClientFactory.createBasicClient();
    }

    // === ОБЩИЕ ENDPOINTS ДЛЯ ВСЕХ МОДЕЛЕЙ ===

    /**
     * Проверка здоровья сервера.
     * GET /health - Доступен во всех моделях
     */
    public String healthCheck() throws AIApiException {
        return get("/health");
    }

    /**
     * Метрики загрузки сервера.
     * GET /load - Доступен во всех моделях
     */
    public Map<String, Object> getServerLoad() throws AIApiException {
        String response = get("/load");
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Ping проверка.
     * GET /ping - Доступен во всех моделях
     */
    public String ping() throws AIApiException {
        return get("/ping");
    }

    /**
     * Ping проверка (POST).
     * POST /ping - Доступен во всех моделях
     */
    public String pingPost() throws AIApiException {
        return post("/ping", "{}");
    }

    /**
     * Версия сервера.
     * GET /version - Доступен во всех моделях
     */
    public Map<String, Object> getVersion() throws AIApiException {
        String response = get("/version");
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Prometheus метрики.
     * GET /metrics - Доступен во всех моделях
     */
    public String getMetrics() throws AIApiException {
        return get("/metrics");
    }

    /**
     * Список доступных моделей.
     * GET /v1/models - Доступен во всех моделях
     */
    public Map<String, Object> getModels() throws AIApiException {
        String response = get("/v1/models");
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Базовые embeddings.
     * POST /v1/embeddings - Доступен во всех моделях
     */
    public Map<String, Object> createEmbedding(EmbeddingRequest request) throws AIApiException {
        String json = toJson(request);
        String response = post("/v1/embeddings", json);
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Оценка сходства текстов.
     * POST /score - Доступен во всех моделях
     */
    public Map<String, Object> score(ScoreRequest request) throws AIApiException {
        String json = toJson(request);
        String response = post("/score", json);
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Ранжирование документов.
     * POST /rerank - Доступен во всех моделях
     */
    public Map<String, Object> rerank(RerankRequest request) throws AIApiException {
        String json = toJson(request);
        String response = post("/rerank", json);
        return parseJson(response, new TypeReference<Map<String, Object>>() {});
    }

    // === ASYNC ВЕРСИИ ===

    public CompletableFuture<String> healthCheckAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try { return healthCheck(); }
            catch (Exception e) { throw new CompletionException(e); }
        });
    }

    public CompletableFuture<Map<String, Object>> getModelsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try { return getModels(); }
            catch (Exception e) { throw new CompletionException(e); }
        });
    }

    // === УТИЛИТЫ ===

    protected String get(String path) throws AIApiException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET();

        if (apiKey != null) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new AIApiException("HTTP ошибка " + response.statusCode() + " для " + path,
                        modelName, response.statusCode(), response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new AIApiException("Ошибка сети для " + path, modelName, e);
        }
    }

    protected String post(String path, String json) throws AIApiException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (apiKey != null) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    BodyHandlers.ofString()
            );

            if (response.statusCode() / 100 != 2) {
                throw new AIApiException("HTTP ошибка " + response.statusCode() + " для " + path,
                        modelName, response.statusCode(), response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new AIApiException("Ошибка сети для " + path, modelName, e);
        }
    }

    protected String toJson(Object obj) throws AIApiException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new AIApiException("Ошибка сериализации JSON", modelName, e);
        }
    }

    protected <T> T parseJson(String json, TypeReference<T> typeRef) throws AIApiException {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new AIApiException("Ошибка парсинга JSON", modelName, e);
        }
    }
}