package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

public class MainState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();

    private static final Set<MessageDelimiters> INTERCEPT_TAGS = Set.of(
            THINK_OPEN,
            TOOL_OPEN
    );

    private static final int MAX_TAG_LEN = INTERCEPT_TAGS.stream()
            .mapToInt(MessageDelimiters::length).max().orElse(0);

    public MainState(MarkdownMessageWithThinking context) {
        super(context);
    }

    @Override
    public ProcessingState process(String chunk) {
        buffer.append(chunk);
        String text = buffer.toString();

        Optional<MessageDelimiters.Tag> firstTag = MessageDelimiters.firstIn(text, INTERCEPT_TAGS);
        if (firstTag.isEmpty()) {
            flushSafePart();
            return this;
        }

        int pos = firstTag.get().pos();
        if (pos > 0) {
            context.getMainMessage().appendMarkdown(text.substring(0, pos));
        }

        String remaining = text.substring(pos + firstTag.get().tag().length());
        buffer.setLength(0);

        return switch (firstTag.get().tag()) {
            case THINK_OPEN -> {
                ThinkingState ts = new ThinkingState(context);
                yield remaining.isEmpty() ? ts : ts.process(remaining);
            }
            case TOOL_OPEN -> {
                ToolCallingState tcs = new ToolCallingState(context);
                yield remaining.isEmpty() ? tcs : tcs.process(remaining);
            }
            default -> this;
        };
    }

    private void flushSafePart() {
        int safeLen = Math.max(0, buffer.length() - MAX_TAG_LEN);
        if (safeLen > 0) {
            context.getMainMessage().appendMarkdown(buffer.substring(0, safeLen));
            buffer.delete(0, safeLen);
        }
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            context.getMainMessage().appendMarkdown(buffer.toString());
            buffer.setLength(0);
        }
    }
}
