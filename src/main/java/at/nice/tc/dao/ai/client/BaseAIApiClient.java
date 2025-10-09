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
 * <p>
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
        this.httpClient = HttpClientFactory.createClient();
    }

    // === ОБЩИЕ ENDPOINTS ДЛЯ ВСЕХ МОДЕЛЕЙ ===

    /**
     * Проверка здоровья сервера.
     * GET /health - Доступен во всех моделях
     *
     * @return код статуса
     */
    public int healthCheck() {
        return GET("/health").statusCode();
    }

    /**
     * Метрики загрузки сервера.
     * GET /load - Доступен во всех моделях
     */
    public Map<String, Object> getServerLoad() {
        String response = GET("/load").body();
        return parseJson(response, new TypeReference<>() {
        });
    }

    /**
     * Ping проверка.
     * GET /ping - Доступен во всех моделях
     *
     * @return код статуса
     */
    public int ping() {
        return GET("/ping").statusCode();
    }

    /**
     * Ping проверка (POST).
     * POST /ping - Доступен во всех моделях
     *
     * @return код статуса
     */
    public int pingPost() {
        return POST("/ping", "{}").statusCode();
    }

    /**
     * Версия сервера.
     * GET /version - Доступен во всех моделях
     *
     * @return версия в формате "0.9.1"
     */
    public String getVersion() {
        String response = GET("/version").body();
        return parseJson(response, new TypeReference<Map<String, String>>() {
        }).get("version");
    }

    /**
     * Prometheus метрики.
     * GET /metrics - Доступен во всех моделях
     */
    public String getMetrics() {
        return GET("/metrics").body();
    }

    /**
     * Список доступных моделей.
     * GET /v1/models - Доступен во всех моделях
     */
    public Map<String, Object> getModels() {
        String response = GET("/v1/models").body();
        return parseJson(response, new TypeReference<>() {
        });
    }

    /**
     * Базовые embeddings.
     * POST /v1/embeddings - Доступен во всех моделях (кроме 32b)
     */
    public Map<String, Object> createEmbedding(EmbeddingRequest request) {
        String json = toJson(request);
        String response = POST("/v1/embeddings", json).body();
        return parseJson(response, new TypeReference<>() {
        });
    }

    /**
     * Оценка сходства текстов.
     * POST /score - Доступен во всех моделях (кроме 32b)
     */
    public Map<String, Object> score(ScoreRequest request) {
        String json = toJson(request);
        String response = POST("/score", json).body();
        return parseJson(response, new TypeReference<>() {
        });
    }

    /**
     * Ранжирование документов.
     * POST /rerank - Доступен во всех моделях
     */
    public Map<String, Object> rerank(RerankRequest request) {
        String json = toJson(request);
        String response = POST("/rerank", json).body();
        return parseJson(response, new TypeReference<>() {
        });
    }

    // === ASYNC ВЕРСИИ ===

    public CompletableFuture<Integer> healthCheckAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return healthCheck();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Map<String, Object>> getModelsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getModels();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    // === УТИЛИТЫ ===

    protected HttpResponse<String> GET(String path) {
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
            return response;
        } catch (IOException | InterruptedException e) {
            throw new AIApiException("Ошибка сети для " + path, modelName, e);
        }
    }

    protected HttpResponse<String> POST(String path, String json) {
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
            return response;
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