package at.nice.tc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient mainChatClient;

    public Flux<String> sendMessageStream(String message) {
        return mainChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
