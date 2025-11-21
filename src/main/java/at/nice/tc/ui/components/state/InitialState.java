package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.utils.UiUtils;
import com.vaadin.flow.component.notification.Notification;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

/**
 * Начальное состояние: проверяем первые символы на предмет наличия <think>
 */
@Slf4j
public class InitialState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();
    private boolean bufferSent = false; // Флаг для отслеживания, был ли буфер уже отправлен

    public InitialState(MarkdownMessageWithThinking context) {
        super(context);
    }

    private static final Set<MessageDelimiters> tags = Set.of(
            THINK_OPEN,
            TOOL_OPEN
    );

    @Override
    public ProcessingState process(String chunk) {
        buffer.append(chunk);

        final String text = buffer.toString();
        if (text.trim().length() < THINK_OPEN.length()) {
            return this; // Ждём ещё данных
        }

        Optional<Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isPresent()) {
            int minPos = firstTag.get().pos();
            return switch (firstTag.get().tag()) {
                case THINK_OPEN -> {
                    // переходим в thinking режим
                    buffer.delete(minPos, minPos + THINK_OPEN.length());
                    yield new ThinkingState(context).process(buffer.toString());
                }
                case TOOL_OPEN -> {
                    // переходим в tool-calling режим
                    buffer.delete(minPos, minPos + TOOL_OPEN.length());
                    yield new ToolCallingState(context).process(buffer.toString());
                }
                default -> {
                    // уведомляем и остаемся в этом режиме
                    UiUtils.printAndShowNotification(
                            "В InitialState встречен тег %s в тексте %s [%d]".formatted(firstTag.get().tag(), text, minPos)
                    );
                    buffer.delete(minPos, minPos + TOOL_OPEN.length());
                    yield new InitialState(context).process(buffer.toString());
                }
            };
        }

        // Нет тега - переходим в обычный режим
        // Проблема: из-за асинхронности ui.access() несколько чанков могут обрабатываться
        // в InitialState до перехода в MainState, что приводит к дублированию.
        // Решение: отправляем весь буфер только один раз (при первом определении отсутствия тега),
        // затем переходим в MainState. Следующие чанки будут обрабатываться в MainState.

        String markdownSnippet;
        if (bufferSent) {
            // Буфер уже был отправлен, но из-за асинхронности мы все еще в InitialState
            // Отправляем только новый chunk, чтобы избежать дублирования
            markdownSnippet = chunk;

        } else {
            // Первый раз определяем отсутствие тега - отправляем весь буфер
            bufferSent = true;
            markdownSnippet = text;
        }

        checkUiAccessed(isAccessed -> {
            final MarkdownMessage mainMessage = context.getMainMessage();
            if (isAccessed)
                mainMessage.appendMarkdownAsync(markdownSnippet);
            else
                mainMessage.appendMarkdown(markdownSnippet);
        });

        // Очищаем буфер после отправки
        buffer.setLength(0);

        // Переходим в MainState - следующие чанки будут обрабатываться там
        return new MainState(context);
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            checkUiAccessed(isAccessed -> {
                final String markdownSnippet = buffer.toString();
                final MarkdownMessage mainMessage = context.getMainMessage();
                if (isAccessed)
                    mainMessage.appendMarkdownAsync(markdownSnippet);
                else
                    mainMessage.appendMarkdown(markdownSnippet);
            });
        }
    }
}

