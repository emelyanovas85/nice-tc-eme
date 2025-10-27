/*
package at.nice.tc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

*/
/**
 * Сервис для управления Server-Sent Events
 *//*

@Service
public class SseService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter createConnection() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        emitters.add(emitter);

        sendToEmitter(emitter, "connected", Map.of("message", "SSE соединение установлено"));

        return emitter;
    }

    public void sendChatMessage(String testId, String checkId, String content, String messageType) {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", testId);
        data.put("checkId", checkId);
        data.put("message", Map.of(
            "type", messageType,
            "content", content,
            "timestamp", LocalDateTime.now().toString()
        ));

        broadcastEvent("chat_message", data);
    }

    public void sendStatusUpdate(String testId, String checkId, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", testId);
        data.put("checkId", checkId);
        data.put("status", status);

        broadcastEvent("status_update", data);
    }

    public void sendProcessingStart(String testId, String checkId) {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", testId);
        data.put("checkId", checkId);

        broadcastEvent("processing_start", data);
    }

    public void sendProcessingEnd(String testId, String checkId) {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", testId);
        data.put("checkId", checkId);

        broadcastEvent("processing_end", data);
    }

    public void sendSystemNotification(String message, String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("type", type);

        broadcastEvent("system_notification", data);
    }

    public void sendTestChanged(String testId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", testId);
        data.put("message", message);

        broadcastEvent("test_changed", data);
    }

    private void broadcastEvent(String eventType, Map<String, Object> data) {
        data.put("type", eventType);

        emitters.removeIf(emitter -> {
            try {
                return !sendToEmitter(emitter, eventType, data);
            } catch (Exception e) {
                return true;
            }
        });
    }

    private boolean sendToEmitter(SseEmitter emitter, String eventType, Map<String, Object> data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                .name(eventType)
                .data(jsonData));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public int getActiveConnectionsCount() {
        return emitters.size();
    }

    public void closeAllConnections() {
        emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Игнорируем ошибки при закрытии
            }
        });
        emitters.clear();
    }
}
*/
