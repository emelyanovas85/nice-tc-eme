package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import com.vaadin.flow.component.markdown.Markdown;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

public class ToolCallingState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();
    private final Markdown toolMarkdown;

    public ToolCallingState(MarkdownMessageWithThinking context) {
        super(context);
        context.ensureThinkingDetailsCreated();
        this.toolMarkdown = context.addNewThinkingMarkdown();
    }

    private static final Set<MessageDelimiters> tags = Set.of(
            THINK_OPEN,
            TOOL_CLOSE,
            TOOL_UPDATE
    );

    @Override
    public ProcessingState process(String chunk) {
        buffer.append(chunk);
        String text = buffer.toString();

        Optional<Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isEmpty()) {
            flushSafePart();
            return this;
        }

        int minPos = firstTag.get().pos();
        return switch (firstTag.get().tag()) {
            case THINK_OPEN -> handleThinkOpen(minPos, text);
            case TOOL_UPDATE -> {
                long updateTimestamp = extractTimestamp(text, minPos);
                yield handleToolUpdate(minPos, text, updateTimestamp);
            }
            case TOOL_CLOSE -> handleCloseTag(minPos, text);
            default -> this;
        };
    }

    private static final int timestampLen = String.valueOf(System.currentTimeMillis()).length();

    private long extractTimestamp(String text, int pos) {
        int start = pos + TOOL_UPDATE.getPlaceholder().length();
        int end = start + timestampLen;
        if (end > text.length()) return 0L;
        try {
            return Long.parseLong(text.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private ProcessingState handleToolUpdate(int pos, String text, long timestamp) {
        if (pos < 0) return this;

        // Пишем в toolMarkdown всё, что накопилось ДО тега <tool_update>
        if (pos > 0) {
            context.getThinkingMessage().appendContent(text.substring(0, pos));
        }

        // Если timestamp не распознан — ждём ещё данных (timestamp ещё не полностью пришёл)
        if (timestamp == 0L) {
            // Если начало timestamp уже есть, но он неполный — удерживаем в буфере
            buffer.setLength(0);
            buffer.append(text.substring(pos));
            return this;
        }

        // Обрабатываем событие по timestamp
        if (context.getAiToolCallService() != null) {
            context.getAiToolCallService().getUpdate(timestamp).ifPresent(context::handleEvent);
        }

        // remaining — всё после <tool_update> + 13-значного timestamp
        int skipLen = TOOL_UPDATE.getPlaceholder().length() + timestampLen;
        buffer.setLength(0);
        String remaining = text.substring(pos + skipLen);
        if (remaining.isEmpty()) return this;
        return process(remaining);
    }

    private ProcessingState handleThinkOpen(int pos, String text) {
        if (pos < 0) return this;

        context.ensureThinkingDetailsCreated();
        context.getThinkingMessage().appendContent(text.substring(0, pos));

        Markdown newMarkdown = context.addNewThinkingMarkdown();

        buffer.setLength(0);
        String remaining = text.substring(pos + THINK_OPEN.length());

        ThinkingState thinkingState = new ThinkingState(context, newMarkdown);
        if (remaining.isEmpty()) return thinkingState;
        return thinkingState.process(remaining);
    }

    private ProcessingState handleCloseTag(int pos, String text) {
        if (pos < 0) return this;

        toolMarkdown.appendContent(text.substring(0, pos));

        buffer.setLength(0);
        String remaining = text.substring(pos + TOOL_CLOSE.length());

        InitialState initialState = new InitialState(context);
        if (remaining.isEmpty()) return initialState;
        return initialState.process(remaining);
    }

    private static final int maxTagLength = tags.stream().mapToInt(MessageDelimiters::length).max().orElse(0);

    private void flushSafePart() {
        int safeLength = Math.max(0, buffer.length() - maxTagLength);
        if (safeLength > 0) {
            toolMarkdown.appendContent(buffer.substring(0, safeLength));
            buffer.delete(0, safeLength);
        }
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty())
            toolMarkdown.appendContent(buffer.toString());
    }
}
