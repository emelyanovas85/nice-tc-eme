/*
package at.nice.tc.config;

import at.nice.tc.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiWebSocketHandler extends TextWebSocketHandler {

    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        try {
            JsonNode input = objectMapper.readTree(message.getPayload());
            String type = input.has("type") ? input.get("type").asText() : "";
            String conversationId = input.has("conversationId") ? input.get("conversationId").asText() : "";
            JsonNode payload = input.has("payload") ? input.get("payload") : null;

            // Здесь выбирай свою бизнес-логику по type
            if ("chat_message".equals(type) && payload != null && payload.has("text")) {
                String text = payload.get("text").asText();

                Flux<String> responseStream = aiService.sendMessageStream(text);

                responseStream.subscribe(
                        contentPart -> {
                            try {
                                Map<String, Object> outMsg = new HashMap<>();
                                outMsg.put("type", "ai_message");
                                outMsg.put("conversationId", conversationId);
                                Map<String, Object> payloadOut = new HashMap<>();
                                payloadOut.put("text", contentPart);
                                payloadOut.put("role", "ai");
                                outMsg.put("payload", payloadOut);

                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outMsg)));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        error -> {
                            try {
                                Map<String, Object> errMsg = new HashMap<>();
                                errMsg.put("type", "error");
                                errMsg.put("conversationId", conversationId);
                                Map<String, Object> errPayload = new HashMap<>();
                                errPayload.put("message", error.getMessage());
                                errMsg.put("payload", errPayload);

                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errMsg)));
                                session.close(CloseStatus.SERVER_ERROR);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        () -> {
                            try {
                                if (session.isOpen()) {
                                    session.close(CloseStatus.NORMAL);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );

            } else {
                // Если type неверный или нет payload — ошибка в стандартизированном виде
                Map<String, Object> errMsg = new HashMap<>();
                errMsg.put("type", "error");
                errMsg.put("conversationId", conversationId);
                Map<String, Object> errPayload = new HashMap<>();
                errPayload.put("message", "Invalid message format or type");
                errMsg.put("payload", errPayload);

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errMsg)));
            }

        } catch (IOException e) {
            e.printStackTrace();
            try {
                Map<String, Object> errMsg = new HashMap<>();
                errMsg.put("type", "error");
                errMsg.put("conversationId", "");
                Map<String, Object> errPayload = new HashMap<>();
                errPayload.put("message", "Unable to parse message: " + e.getMessage());
                errMsg.put("payload", errPayload);

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errMsg)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
*/
