package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import com.vaadin.flow.component.markdown.Markdown;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 * Tool calling режим: буферизация и поиск </tool>, обработка TOOL_UPDATE событий
 */
public class ToolCallingState extends ProcessingState {
    
    private final StringBuilder buffer = new StringBuilder();
    private final Markdown toolMarkdown;

    /**
     * Создает ToolCallingState с новым Markdown компонентом для tool блока
     */
    public ToolCallingState(MarkdownMessageWithThinking context, Markdown toolMarkdown) {
        super(context);
        this.toolMarkdown = toolMarkdown;
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

        // Определяем индекс первого тега
        Optional<Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isEmpty()) {
            // Закрывающего тега нет - отдаём безопасную часть
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
            default -> this; // продолжаем накапливать токены
        };
    }


    private static final int timestampLen = String.valueOf(System.currentTimeMillis()).length();

    /**
     * Извлекает timestamp из текста, начиная с позиции TOOL_UPDATE delimiter'а
     */
    private long extractTimestamp(String text, int pos) {
        int start = pos + TOOL_UPDATE.getPlaceholder().length();
        int end = start + timestampLen;
        try {
            return Long.parseLong(text.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * @see MessageDelimiters#TOOL_UPDATE -> this
     */
    private ProcessingState handleToolUpdate(int pos, String text, long timestamp) {
        if (pos < 0)
            return this;

        // Отправляем содержимое до TOOL_UPDATE в текущий ThinkingMessage
        String beforeUpdate = text.substring(0, pos);
//        checkUiAccessed(isAccessed -> context.getThinkingMessage().appendContent(beforeUpdate));
        context.getThinkingMessage().appendContent(beforeUpdate);

        if (timestamp == 0L || context.getAiToolCallService() == null)
            return this;

        // Получаем ToolEvent по timestamp и обрабатываем его через EventHandlers
        context.getAiToolCallService().getUpdate(timestamp).ifPresent(context::handleEvent);

        // Вычисляем длину delimiter'а с timestamp
        int delimiterLength = TOOL_UPDATE.getPlaceholder().length() + timestampLen;

        // Очищаем буфер и обрабатываем оставшуюся часть после TOOL_UPDATE + timestamp
        buffer.setLength(0);
        String remaining = text.substring(pos + delimiterLength);
        if (remaining.isEmpty())
            return this;
        return process(remaining);
    }

    /**
     * @see MessageDelimiters#THINK_OPEN -> ThinkingState
     */
    private ProcessingState handleThinkOpen(int pos, String text) {
        if (pos < 0)
            return this;

        // Отправляем содержимое до THINK_OPEN в thinkMessage
        String beforeThink = text.substring(0, pos);
        checkUiAccessed(isAccessed -> {
            context.ensureThinkingDetailsCreated();
            context.getThinkingMessage().appendContent(beforeThink);
        });

        // Создаём новый Markdown компонент для нового блока размышлений
        Markdown newMarkdown = context.addNewThinkingMarkdown();

        // Очищаем буфер и обрабатываем оставшуюся часть после THINK_OPEN
        buffer.setLength(0);
        String remaining = text.substring(pos + THINK_OPEN.length());

        ThinkingState thinkingState = new ThinkingState(context, newMarkdown);
        if (remaining.isEmpty())
            return thinkingState;
        return thinkingState.process(remaining);
    }

    /**
     * @see MessageDelimiters#TOOL_CLOSE -> InitialState
     */
    private ProcessingState handleCloseTag(int pos, String text) {
        if (pos < 0)
            return this;

        // Отправляем содержимое до TOOL_CLOSE в thinkMessage
        String beforeClose = text.substring(0, pos);
        checkUiAccessed(isAccessed -> toolMarkdown.appendContent(beforeClose));

        // Очищаем буфер и обрабатываем оставшуюся часть после TOOL_CLOSE
        buffer.setLength(0);
        String remaining = text.substring(pos + TOOL_CLOSE.length());

        InitialState initialState = new InitialState(context);
        if (remaining.isEmpty())
            return initialState;
        return initialState.process(remaining);
    }

    private static final int maxTagLength = tags.stream().mapToInt(MessageDelimiters::length).max().orElse(0);

    /**
     * Отправка в toolMessage части буфера, оставляя часть размером с самым длинным разделителем
     */
    private void flushSafePart() {
        // Безопасная длина: оставляем место для самого длинного тега
        // TOOL_UPDATE может быть длинным из-за timestamp, поэтому учитываем максимальную длину timestamp (13 цифр для миллисекунд)
        int safeLength = Math.max(0, buffer.length() - maxTagLength);
        if (safeLength > 0) {
            String safePart = buffer.substring(0, safeLength);
//            checkUiAccessed(context, isAccessed -> toolMarkdown.appendContent(safePart));
            toolMarkdown.appendContent(safePart);
            buffer.delete(0, safeLength);
        }
    }

    @Override
    public void flush() {
        if (buffer.isEmpty())
            return;
//        checkUiAccessed(isAccessed -> toolMarkdown.appendContent(safePart));
        toolMarkdown.appendContent(buffer.toString());
    }
}

