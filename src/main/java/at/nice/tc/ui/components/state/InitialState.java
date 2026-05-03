package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.utils.UiUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 * Начальное состояние: проверяем первые символы на предмет наличия &lt;think&gt;
 *
 * Буферизация executeJs до attach решена на уровне SafeMarkdownMessage —
 * здесь не нужно заботиться о forceSync.
 */
@Slf4j
public class InitialState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();
    private boolean bufferSent = false;

    private static final Set<MessageDelimiters> tags = Set.of(
            THINK_OPEN,
            TOOL_OPEN
    );

    public InitialState(MarkdownMessageWithThinking context) {
        super(context);
    }

    @Override
    public ProcessingState process(String chunk) {
        buffer.append(chunk);

        final String text = buffer.toString();
        if (text.trim().length() < THINK_OPEN.length()) {
            return this;
        }

        Optional<MessageDelimiters.Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isPresent()) {
            int minPos = firstTag.get().pos();
            return switch (firstTag.get().tag()) {
                case THINK_OPEN -> {
                    buffer.delete(minPos, minPos + THINK_OPEN.length());
                    yield new ThinkingState(context).process(buffer.toString());
                }
                case TOOL_OPEN -> {
                    buffer.delete(minPos, minPos + TOOL_OPEN.length());
                    yield new ToolCallingState(context).process(buffer.toString());
                }
                default -> {
                    UiUtils.printAndShowNotification(
                            "В InitialState встречен тег %s в тексте %s [%d]".formatted(firstTag.get().tag(), text, minPos)
                    );
                    buffer.delete(minPos, minPos + TOOL_OPEN.length());
                    yield new InitialState(context).process(buffer.toString());
                }
            };
        }

        String markdownSnippet;
        if (bufferSent) {
            markdownSnippet = chunk;
        } else {
            bufferSent = true;
            markdownSnippet = text;
        }

        sendToMain(markdownSnippet);

        buffer.setLength(0);
        return new MainState(context);
    }

    private void sendToMain(String text) {
        // SafeMarkdownMessage сам буферизует до attach, поэтому просто вызываем appendMarkdown
        // без проверок isAttached / isAccessed
        checkUiAccessed(isAccessed -> {
            if (isAccessed)
                context.getMainMessage().appendMarkdownAsync(text);
            else
                context.getMainMessage().appendMarkdown(text);
        });
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            sendToMain(buffer.toString());
        }
    }
}
