package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;

/**
 * Обычный режим: прямая передача без буфера
 */
public class MainState extends ProcessingState {
    @Override
    public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
        checkUiAccessed(context, isAccessed -> {
            if (isAccessed)
                context.getMainMessage().appendMarkdownAsync(chunk);
            else
                context.getMainMessage().appendMarkdown(chunk);
        });
        return this;
    }

    @Override
    public void flush(MarkdownMessageWithThinking context) {
        // Нечего сбрасывать - буфера нет
    }
}

