package at.nice.tc.utils;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;

public abstract class ToolUtils {

    /**
     * Извлекает {@link ChatMemory#CONVERSATION_ID} из переданного контекста
     */
    public static String conversationId(ToolContext context) {
        Object conversationId = context.getContext().get(ChatMemory.CONVERSATION_ID);
        if (conversationId == null) {
            throw new RuntimeException(ChatMemory.CONVERSATION_ID + " not provided via ToolContext");
        }
        return (String) conversationId;
    }
}
