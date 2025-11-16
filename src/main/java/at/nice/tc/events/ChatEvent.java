package at.nice.tc.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class ChatEvent extends ApplicationEvent {
    private final String conversationId;
    public ChatEvent(String conversationId) {
        super(conversationId);
        this.conversationId = conversationId;
    }
}
