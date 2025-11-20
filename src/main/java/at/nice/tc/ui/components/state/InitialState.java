package at.nice.tc.ui.components.state;

import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import static at.nice.tc.ui.MessageDelimiters.THINK_OPEN;

/**
 * Начальное состояние: проверяем первые символы на предмет наличия <think>
 */
public class InitialState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();
    private boolean bufferSent = false; // Флаг для отслеживания, был ли буфер уже отправлен

    @Override
    public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
        buffer.append(chunk);

        if (buffer.toString().trim().length() < THINK_OPEN.length()) {
            return this; // Ждём ещё данных
        }

        int tagPos = THINK_OPEN.posIn(buffer);
        if (tagPos >= 0) {
            // Есть тег - переходим в thinking режим
            context.ensureThinkingDetailsCreated();
            buffer.delete(tagPos, tagPos + THINK_OPEN.length());
            return new ThinkingState().process(buffer.toString(), context);
        }

        // Нет тега - переходим в обычный режим
        // Проблема: из-за асинхронности ui.access() несколько чанков могут обрабатываться
        // в InitialState до перехода в MainState, что приводит к дублированию.
        // Решение: отправляем весь буфер только один раз (при первом определении отсутствия тега),
        // затем переходим в MainState. Следующие чанки будут обрабатываться в MainState.

        if (bufferSent) {
            // Буфер уже был отправлен, но из-за асинхронности мы все еще в InitialState
            // Отправляем только новый chunk, чтобы избежать дублирования
            checkUiAccessed(context, isAccessed -> {
                final MarkdownMessage mainMessage = context.getMainMessage();
                if (isAccessed)
                    mainMessage.appendMarkdownAsync(chunk);
                else
                    mainMessage.appendMarkdown(chunk);
            });
            // Очищаем буфер, так как мы уже отправили новый chunk
            buffer.setLength(0);
        } else {
            // Первый раз определяем отсутствие тега - отправляем весь буфер
            final String markdownSnippet = buffer.toString();
            bufferSent = true;

            checkUiAccessed(context, isAccessed -> {
                final MarkdownMessage mainMessage = context.getMainMessage();
                if (isAccessed)
                    mainMessage.appendMarkdownAsync(markdownSnippet);
                else
                    mainMessage.appendMarkdown(markdownSnippet);
            });

            // Очищаем буфер после отправки
            buffer.setLength(0);
        }

        // Переходим в MainState - следующие чанки будут обрабатываться там
        return new MainState();
    }

    @Override
    public void flush(MarkdownMessageWithThinking context) {
        if (!buffer.isEmpty()) {
            checkUiAccessed(context, isAccessed -> {
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

