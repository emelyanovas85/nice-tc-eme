package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;

/**
 * Базовый класс для состояний обработки markdown-сообщений.
 * Все методы process/flush вызываются уже из UI-потока (request thread или ui.access),
 * поэтому внутри используем прямой appendMarkdown, без appendMarkdownAsync.
 */
public abstract class ProcessingState {

    protected final MarkdownMessageWithThinking context;

    protected ProcessingState(MarkdownMessageWithThinking context) {
        this.context = context;
        context.notifyStateChanged(this);
    }

    public abstract ProcessingState process(String chunk);

    public abstract void flush();

    @FunctionalInterface
    public interface Listener {
        void changed(ProcessingState newState);
    }
}
