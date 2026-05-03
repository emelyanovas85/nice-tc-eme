package at.nice.tc.ui.components;

import com.vaadin.flow.component.AttachEvent;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.time.LocalDateTime;

/**
 * Расширение MarkdownMessage, которое безопасно буферизует контент
 * до момента attach к DOM.
 *
 * Проблема оригинального MarkdownMessage: appendMarkdown/setMarkdown
 * вызывают element.executeJs(), который теряется если компонент ещё
 * не attached к DOM (executeJs на detached-элементе не гарантирует выполнение).
 *
 * Решение: все вызовы до onAttach копятся в pendingMarkdown,
 * и применяются единым setMarkdown сразу после attach.
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
     * Добавляет markdown-фрагмент.
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
}
