package at.nice.tc.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

@Service
public class GoogleDocsService {
    private final WebClient webClient;

    public GoogleDocsService(@Qualifier("redirectable") WebClient webClient) {
        this.webClient = webClient;
    }


    public String getTableFromAppsScript(String scriptBaseUrl, String docId, String tabName, int tableIndex, int... columns) {
        String columnsParam = stream(columns)
                .mapToObj(String::valueOf)
                .collect(joining(","));

        URI uri = UriComponentsBuilder.fromHttpUrl(scriptBaseUrl)
                .queryParam("docId", docId)
                .queryParam("tabName", tabName)
                .queryParam("tableIndex", tableIndex)
                .queryParam("columns", columnsParam)
                .encode()
                .build()
                .toUri();

        try {
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return "Ошибка HTTP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "Ошибка при загрузке данных из Apps Script: " + e.getMessage();
        }
    }
}
