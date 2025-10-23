package at.nice.tc.controller;

import at.nice.tc.service.AiService;
import at.nice.tc.service.SseService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST контроллер для работы с AI сервисом
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final SseService sseService;

    @PostMapping(value = "/chat")
    public String sendMessage(@RequestBody TestMessage request) {
        return aiService.sendMessage(request.getMessage());
    }

    @Setter
    @Getter
    public static class TestMessage{
        private  String message;
    }
}
