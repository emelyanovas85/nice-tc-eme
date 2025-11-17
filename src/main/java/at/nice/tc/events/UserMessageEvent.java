package at.nice.tc.events;

import lombok.Getter;

/**
 * Событие о новом сообщении пользователя.
 * Используется для синхронизации сообщений пользователя между вкладками браузера
 */
@Getter
public class UserMessageEvent extends ChatEvent {
    private final String userText;
    private final String userFio;
//    private final int messagePosition;
    private final long messageTimestamp;

    public UserMessageEvent(String conversationId, String userText, String userFio, long timestamp) {
        super(conversationId);
        this.userText = userText;
        this.userFio = userFio;
//        this.messagePosition = messagePosition;
        this.messageTimestamp = timestamp;
    }
}

