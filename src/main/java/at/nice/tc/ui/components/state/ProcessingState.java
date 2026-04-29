package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;

import java.util.function.Consumer;

/**
 * Базовый класс для состояний обработки markdown сообщений
 */
public abstract class ProcessingState {
    protected final MarkdownMessageWithThinking context;

    protected ProcessingState(MarkdownMessageWithThinking context) {
        this.context = context;
        // Уведомляем контекст о создании нового состояния
        // (передаем ссылку на экземпляр этого состояния)
        context.notifyStateChanged(this);
    }

    public abstract ProcessingState process(String chunk);

    public abstract void flush();

    // Используем getUI() из контекста вместо UI.getCurrent(),
    // так как при восстановлении из истории UI.getCurrent() может быть null
    protected void checkUiAccessed(Consumer<Boolean> act) {
        context.getUI().ifPresentOrElse(
            ui -> act.accept(ui.isAttached()),
            () -> act.accept(false)
        );
    }

    @FunctionalInterface
    public interface Listener {
        void changed(ProcessingState newState);
    }
}
