package at.nice.tc.ui.components;

import com.vaadin.flow.component.AttachEvent;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.time.LocalDateTime;

/**
 * Расширение MarkdownMessage, которое безопасно буферизует любой контент
 * до момента attach к DOM.
 *
 * Проблема оригинального MarkdownMessage:
 *   - appendMarkdown/setMarkdown вызывают element.executeJs(), который теряется
 *     если компонент ещё не attached.
 *   - appendMarkdownAsync внутри вызывает getUi(), который NPE если
 *     ui ещё не установлен (т.к. onAttach ещё не вызывался).
 *
 * Решение: все вызовы до onAttach копятся в pendingMarkdown,
 * и применяются единым super.setMarkdown() сразу после attach.
 */
public class SafeMarkdownMessage extends MarkdownMessage {

    private final StringBuilder pendingMarkdown = new StringBuilder();
    private boolean attached = false;

    public SafeMarkdownMessage(String name, LocalDateTime timestamp) {
        super(name, timestamp);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attached = true;
        if (!pendingMarkdown.isEmpty()) {
            // super.setMarkdown — напрямую в firitin, UI уже есть (onAttach гарантирует)
            super.setMarkdown(pendingMarkdown.toString());
            pendingMarkdown.setLength(0);
        }
    }

    /**
     * Устанавливает полный markdown-текст.
     * Если компонент ещё не в DOM — буферизует до onAttach.
     */
    @Override
    public void setMarkdown(String markdown) {
        if (!attached) {
            pendingMarkdown.setLength(0);
            if (markdown != null) pendingMarkdown.append(markdown);
        } else {
            super.setMarkdown(markdown);
        }
    }

    /**
     * Добавляет markdown-фрагмент (синхронно, из UI-потока).
     * Если компонент ещё не в DOM — буферизует до onAttach.
     */
    @Override
    public void appendMarkdown(String markdownSnippet) {
        if (!attached) {
            pendingMarkdown.append(markdownSnippet);
        } else {
            super.appendMarkdown(markdownSnippet);
        }
    }

    /**
     * Добавляет markdown-фрагмент асинхронно (из фонового потока).
     * Если компонент ещё не в DOM — буферизует до onAttach без NPE.
     */
    @Override
    public void appendMarkdownAsync(String markdownSnippet) {
        if (!attached) {
            pendingMarkdown.append(markdownSnippet);
        } else {
            super.appendMarkdownAsync(markdownSnippet);
        }
    }
}
