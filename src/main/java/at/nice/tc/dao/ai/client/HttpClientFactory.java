package at.nice.tc.dao.ai.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Класс для создания настроенных HTTP клиентов.
 * Реализует паттерн Factory для унификации создания HttpClient с различными настройками.
 */
public final class HttpClientFactory {

    /**
     * Создает HttpClient с поддержкой cookies и настройками SSL.
     * Подходит для работы с защищенными API endpoints.
     *
     * @param cookieManager менеджер cookies для управления сессией
     * @return настроенный HttpClient
     * @throws RuntimeException если не удается создать клиент
     */
    public static HttpClient createClient(CookieManager cookieManager) {
        try {
            return buildCommonClientBuilder()
                    .cookieHandler(cookieManager)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать HttpClient с поддержкой cookies", e);
        }
    }

    /**
     * Создает базовый HttpClient без cookies.
     * Подходит для простых HTTP запросов без авторизации.
     *
     * @return настроенный HttpClient
     * @throws RuntimeException если не удается создать клиент
     */
    public static HttpClient createBasicClient() {
        try {
            return buildCommonClientBuilder().build();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать базовый HttpClient", e);
        }
    }

    /**
     * Возвращает базовый билдер с общими настройками безопасности и таймаутов.
     * Настраивает SSL context, таймауты подключения и политику редиректов.
     *
     * @return настроенный HttpClient.Builder
     * @throws Exception при ошибках настройки SSL
     */
    private static HttpClient.Builder buildCommonClientBuilder() throws Exception {
        configureSslSystemProperties();
        SSLContext sslContext = createTrustAllSslContext();

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(60))
                .followRedirects(HttpClient.Redirect.NEVER);
    }

    /**
     * Настраивает системные свойства SSL для работы с самоподписанными сертификатами.
     * Отключает проверку отзыва сертификатов и включает небезопасные перезаключения.
     */
    private static void configureSslSystemProperties() {
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    /**
     * Создает SSL context, который доверяет всем сертификатам.
     * ВНИМАНИЕ: Использовать только в тестовой среде!
     *
     * @return настроенный SSLContext
     * @throws Exception при ошибках создания контекста
     */
    private static SSLContext createTrustAllSslContext() throws Exception {
        TrustManager[] trustAllCertificates = {
                new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // Доверяем всем клиентским сертификатам
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Доверяем всем серверным сертификатам
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) {
                        // Доверяем всем клиентским сертификатам через Socket
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) {
                        // Доверяем всем серверным сертификатам через Socket
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                        // Доверяем всем клиентским сертификатам через SSLEngine
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                        // Доверяем всем серверным сертификатам через SSLEngine
                    }
                }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAllCertificates, new java.security.SecureRandom());
        return context;
    }

    /**
     * Приватный конструктор предотвращает инстанцирование утилитного класса.
     */
    private HttpClientFactory() {
        throw new UnsupportedOperationException("Фабричный класс не может быть инстанциирован");
    }
}
