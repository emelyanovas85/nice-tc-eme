package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 * Обычный режим: передает текст в главное сообщение,
 * но буферизует чанки для перехвата тегов &lt;tool&gt; / &lt;think&gt;,
 * которые могут появиться в середине ответа после обычного текста.
 */
public class MainState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();

    private static final Set<MessageDelimiters> INTERCEPT_TAGS = Set.of(
            THINK_OPEN,
            TOOL_OPEN
    );

    // Максимальная длина тега, которая может быть разбита между чанками — для безопасной части
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
            // Тегов нет — отдаём безопасную часть, оставляя хвост для возможного начала тега
            flushSafePart();
            return this;
        }

        int pos = firstTag.get().pos();

        // Отправляем текст до тега в главное сообщение
        if (pos > 0) {
            String before = text.substring(0, pos);
            sendToMain(before);
        }

        // Убираем тег из буфера, остаток передаём следующему состоянию
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

    /**
     * Отправляет безопасную часть буфера (без последних MAX_TAG_LEN символов,
     * которые могут быть началом тега)
     */
    private void flushSafePart() {
        int safeLen = Math.max(0, buffer.length() - MAX_TAG_LEN);
        if (safeLen > 0) {
            String safe = buffer.substring(0, safeLen);
            sendToMain(safe);
            buffer.delete(0, safeLen);
        }
    }

    private void sendToMain(String text) {
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
            buffer.setLength(0);
        }
    }
}
