package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;

/**
 * Обычный режим: прямая передача без буфера
 */
public class MainState extends ProcessingState {
    public MainState(MarkdownMessageWithThinking context) {
        super(context);
    }

    @Override
    public ProcessingState process(String chunk) {
        checkUiAccessed(isAccessed -> {
            if (isAccessed)
                context.getMainMessage().appendMarkdownAsync(chunk);
            else
                context.getMainMessage().appendMarkdown(chunk);
        });
        return this;
    }

    @Override
    public void flush() {
        // Нечего сбрасывать - буфера нет
    }
}

