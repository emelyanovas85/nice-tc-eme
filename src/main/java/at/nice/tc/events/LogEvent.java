package at.nice.tc.events;


import at.nice.tc.model.Attachment;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Просто некий текст
 */
@Getter
public class LogEvent extends ChatEvent {
    private final String text;
    private final List<Attachment> attachments;

    public LogEvent(String text, String conversationId, Attachment... attachments) {
        super(conversationId);
        this.text = text;
        this.attachments = Arrays.asList(attachments);
    }

}
