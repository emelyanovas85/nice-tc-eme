package at.nice.tc.controller;

import at.nice.tc.service.AiService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * REST контроллер для работы с AI сервисом
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
//    private final SseService sseService;

//    @PostMapping(value = "/chat")
//    public String sendMessage(@RequestBody TestMessage request) {
//        return aiService.sendMessage(request.getMessage());
//    }

    @PostMapping(value = "/chat/stream")
    public Flux<String> sendMessageAsStream(@RequestBody TestMessage request) {
        return aiService.sendMessageStream(request.getMessage());
    }

    @Setter
    @Getter
    public static class TestMessage{
        private  String message;
    }
}
