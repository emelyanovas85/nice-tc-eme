package at.nice.tc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient defaultWebClient() {
        return WebClient.create();
    }

    @Bean
    @Qualifier("redirectable")
    public WebClient webClientWithRedirect() {
        return WebClient.builder()
                .clientConnector(
                        new ReactorClientHttpConnector(HttpClient.create().followRedirect(true))
                )
                .build();
    }
}
