package at.nice.tc.ui.components;

import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.time.LocalDateTime;

/**
 * Простая обёртка над MarkdownMessage без лишней буферизации.
 * Правильный порядок работы: сначала add() в DOM, потом setMarkdown/appendMarkdown.
 * Это гарантирует что executeJs всегда выполняется на attached элементе.
 */
public class SafeMarkdownMessage extends MarkdownMessage {

    public SafeMarkdownMessage(String name, LocalDateTime timestamp) {
        super(name, timestamp);
    }
}
