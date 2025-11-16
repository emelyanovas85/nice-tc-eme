package at.nice.tc.events;

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

    public class Before extends LogEvent {

        /**
         * Лог, что получение значения {@link #description} начато
         */
        public Before() {
            super(description, conversationId);
        }
    }

    @Getter
    public class After extends LogEvent {
        private final T result;

        /**
         * Лог, что получение значения {@link #description} завершено
         */
        public After(T result) {
            super(description, conversationId);
            this.result = result;
        }

        /**
         * Добавляет в лог текста результата
         */
        public After(T result, Function<T, String> stringifier) {
            super(description, conversationId, new Attachment.Text("результат", stringifier.apply(result)));
            this.result = result;
        }
    }

    @Getter
    public class Error extends LogEvent {
        private final Throwable throwable;

        public Error(Throwable t) {
            super(description, conversationId, new Attachment.Text("ошибка", ThrowableUtils.asString(t)));
            throwable = t;
        }
    }
}
