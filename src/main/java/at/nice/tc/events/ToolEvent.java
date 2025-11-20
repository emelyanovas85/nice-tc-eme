package at.nice.tc.events;


import at.nice.tc.model.Attachment;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Просто некий текст
 */
@Getter
public class ToolEvent {
    private final String text;
    private final List<Attachment> attachments;

    public ToolEvent(String text, Attachment... attachments) {
        this.text = text;
        this.attachments = Arrays.asList(attachments);
    }

}
