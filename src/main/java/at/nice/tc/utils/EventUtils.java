package at.nice.tc.utils;

import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.OnGetValue;

public abstract class EventUtils {

    /**
     * Получает значение, логируя действия:
     * - начало
     * - завершение
     * - ошибку
     */
    public static <T> T getValueSendingEvents(ThrowableSupplier<T> valueSupplier,
                                              OnGetValue<T> eventBase,
                                              ToolEventPublisher publisher) {
        var toolService = publisher.eventPublisher();
        var chatId = eventBase.conversationId();

        toolService.beginTool(chatId);
        try {
            publisher.publish(eventBase.new Before());
            T value = valueSupplier.get();
            publisher.publish(eventBase.new After(value));

            return value;

        } catch (Throwable t) {
            publisher.publish(eventBase.new Error(t));
            return ThrowableUtils.reThrow(t);

        } finally {
            toolService.endTool(chatId);
        }
    }

}
