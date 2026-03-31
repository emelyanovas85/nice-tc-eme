package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.ui.MessageDelimiters;
import com.vaadin.flow.component.markdown.Markdown;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 * Thinking режим: буферизация и поиск THINK_OPEN, THINK_CLOSE, TOOL_OPEN
 */
public class ThinkingState extends ProcessingState {
    
    private final StringBuilder buffer = new StringBuilder();
    private final Markdown currentMarkdown;

    public ThinkingState(MarkdownMessageWithThinking context) {
        super(context);
        context.ensureThinkingDetailsCreated();
        this.currentMarkdown = context.addNewThinkingMarkdown();
    }

    public ThinkingState(MarkdownMessageWithThinking context, Markdown markdown) {
        super(context);
        this.currentMarkdown = markdown;
    }

    private static final Set<MessageDelimiters> tags = Set.of(
            THINK_OPEN,
            THINK_CLOSE,
            TOOL_OPEN
    );

    @Override
    public ProcessingState process(String chunk) {
        // Ищем все возможные теги и обрабатываем тот, который встречается первым
        buffer.append(chunk);

        String text = buffer.toString();

        // Определяем индекс первого тега
        Optional<MessageDelimiters.Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isEmpty()) {
            // Закрывающего тега нет - отдаём безопасную часть
            flushSafePart();
            return this;
        }

        int minPos = firstTag.get().pos();
        return switch (firstTag.get().tag()) {
            case THINK_OPEN -> handleThinkOpen(minPos, text);
            case TOOL_OPEN -> handleToolOpen(minPos, text);
            case THINK_CLOSE -> handleCloseTag(minPos, text);
            default -> this; // продолжаем накапливать токены
        };
    }

    private ProcessingState handleThinkOpen(int pos, String text) {
        if (pos < 0)
            return this;

        // Отправляем содержимое до THINK_OPEN в текущий markdown
        currentMarkdown.appendContent(text.substring(0, pos));

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

    private ProcessingState handleToolOpen(int pos, String text) {
        if (pos < 0)
            return this;

        // Отправляем содержимое до TOOL_OPEN в текущий markdown
        currentMarkdown.appendContent(text.substring(0, pos));

        // Очищаем буфер и обрабатываем оставшуюся часть после TOOL_OPEN
        buffer.setLength(0);
        String remaining = text.substring(pos + TOOL_OPEN.length());

        ToolCallingState toolCallingState = new ToolCallingState(context);
        if (remaining.isEmpty())
            return toolCallingState;
        return toolCallingState.process(remaining);
    }

    private ProcessingState handleCloseTag(int pos, String text) {
        if (pos < 0)
            return this;

        if (pos > 0) {
            currentMarkdown.appendContent(text.substring(0, pos));
        }

        // Очищаем буфер и обрабатываем оставшуюся часть после THINK_CLOSE
        buffer.setLength(0);
        String remaining = text.substring(pos + THINK_CLOSE.length());

        InitialState initialState = new InitialState(context);
        if (remaining.isEmpty()) {
            return initialState;
        }
        return initialState.process(remaining);
    }

    private void flushSafePart() {
        // Безопасная длина: оставляем место для самого длинного тега
        int maxTagLength = Math.max(Math.max(THINK_CLOSE.length(), THINK_OPEN.length()), TOOL_OPEN.length());
        int safeLength = Math.max(0, buffer.length() - maxTagLength);
        if (safeLength > 0) {
            currentMarkdown.appendContent(buffer.substring(0, safeLength));
            buffer.delete(0, safeLength);
        }
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            currentMarkdown.appendContent(buffer.toString());
        }
    }
}

