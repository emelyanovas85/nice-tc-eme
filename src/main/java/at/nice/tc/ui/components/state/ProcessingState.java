package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Базовый класс для состояний обработки markdown сообщений
 */
public abstract class ProcessingState {
    protected final MarkdownMessageWithThinking context;

    protected ProcessingState(MarkdownMessageWithThinking context) {
        this.context = context;
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

    // <editor-fold desc="Функциональность слушателей" defaultstate="collapsed">

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static ProcessingState currentState;

    public void addChangeStateListener(ProcessingState.Listener l) {
        LISTENERS.add(l);
    }

    {
        LISTENERS.forEach(l -> l.changed(currentState, this));
        currentState = this;
    }

    @FunctionalInterface
    public interface Listener {
        void changed(ProcessingState oldState, ProcessingState newState);
    }
    // </editor-fold>
}

