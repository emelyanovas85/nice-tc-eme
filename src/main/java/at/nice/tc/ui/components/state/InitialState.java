package at.nice.tc.ui.components.state;

import at.nice.tc.ui.MessageDelimiters;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.utils.UiUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static at.nice.tc.ui.MessageDelimiters.*;

@Slf4j
public class InitialState extends ProcessingState {

    private final StringBuilder buffer = new StringBuilder();

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

        // Буферизуем только если текст короче чем самый длинный тег И может ещё начинаться с начала тега
        // Для полного сообщения (setMarkdown) проходим сразу вниз
        boolean couldBeStartOfTag = tags.stream()
                .anyMatch(t -> t.getPlaceholder().startsWith(text));
        if (couldBeStartOfTag && text.length() < THINK_OPEN.length()) {
            return this;
        }

        Optional<MessageDelimiters.Tag> firstTag = MessageDelimiters.firstIn(text, tags);
        if (firstTag.isPresent()) {
            int minPos = firstTag.get().pos();
            return switch (firstTag.get().tag()) {
                case THINK_OPEN -> {
                    if (minPos > 0) context.appendMainText(text.substring(0, minPos));
                    buffer.setLength(0);
                    yield new ThinkingState(context).process(text.substring(minPos + THINK_OPEN.length()));
                }
                case TOOL_OPEN -> {
                    if (minPos > 0) context.appendMainText(text.substring(0, minPos));
                    buffer.setLength(0);
                    yield new ToolCallingState(context).process(text.substring(minPos + TOOL_OPEN.length()));
                }
                default -> {
                    UiUtils.printAndShowNotification(
                            "В InitialState встречен тег %s в тексте %s [%d]".formatted(firstTag.get().tag(), text, minPos)
                    );
                    buffer.setLength(0);
                    yield new InitialState(context).process(text);
                }
            };
        }

        // Нет тегов — отправляем весь буфер в mainMessage и переходим в MainState
        context.appendMainText(text);
        buffer.setLength(0);
        return new MainState(context);
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            context.appendMainText(buffer.toString());
            buffer.setLength(0);
        }
    }
}
