/*
package at.nice.tc.controller;

import at.nice.tc.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

*/
/**
 * REST контроллер для Server-Sent Events
 *//*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    */
/**
     * Подключение к SSE потоку
     *//*

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.createConnection();
    }

    */
/**
     * Отправка тестового события (для отладки)
     *//*

    @PostMapping("/sse/test")
    public String sendTestEvent(@RequestParam String message) {
        sseService.sendSystemNotification(message, "info");
        return "Test event sent";
    }
}
*/
