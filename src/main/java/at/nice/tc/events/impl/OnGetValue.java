package at.nice.tc.events.impl;

import at.nice.tc.events.ToolEvent;
import at.nice.tc.model.Attachment;
import at.nice.tc.utils.ThrowableUtils;
import lombok.Getter;

import java.util.function.Function;

/**
 * Получение значения - это цепочка логов:
 * - лог перед получением
 * - лог после получения
 * - лог в случае ошибки
 * @param description описание, что происходит
 * @param conversationId id беседы (чата)
 */
public record OnGetValue<T>(String description, String conversationId) {

    public class Before extends ToolEvent {

        /**
         * Лог, что получение значения {@link #description} начато
         */
        public Before() {
            super(description);
        }
    }

    @Getter
    public class After extends ToolEvent {
        private final T result;

        /**
         * Лог, что получение значения {@link #description} завершено
         */
        public After(T result) {
            super(description + ": завершено");
            this.result = result;
        }

        /**
         * Добавляет в лог текста результата
         */
        public After(T result, Function<T, String> stringifier) {
            super(description, new Attachment.Text("результат", stringifier.apply(result)));
            this.result = result;
        }
    }

    @Getter
    public class Error extends ToolEvent {
        private final Throwable throwable;

        public Error(Throwable t) {
            super(description, new Attachment.Text("ошибка", ThrowableUtils.asString(t)));
            throwable = t;
        }
    }
}
