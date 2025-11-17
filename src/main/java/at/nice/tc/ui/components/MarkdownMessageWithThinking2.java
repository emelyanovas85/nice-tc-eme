package at.nice.tc.ui.components;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.UI;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MarkdownMessageWithThinking2 extends VerticalLayout {

    private final Details details;
    private final MarkdownMessage mainMessage;
    private final UI ui;

    private ProcessingState state;
    private final StringBuilder buffer;
    private Markdown currentThinkingMarkdown;
    private Markdown currentToolMarkdown;
    private final List<String> streamBuffer;

    public MarkdownMessageWithThinking2(String name, LocalDateTime timestamp) {
        this.details = new Details("Размышления модели");
        this.mainMessage = new MarkdownMessage(name, timestamp);
        this.ui = UI.getCurrent();
        this.buffer = new StringBuilder();
        this.streamBuffer = new ArrayList<>();

        details.setOpened(false);

        add(details, mainMessage);
        setPadding(false);
        setSpacing(false);

        this.state = new InitialState(this);
    }

    public void handleStreamingResponse(Flux<ChatResponse> responseFlux) {
        resetState();

        responseFlux.subscribe(
                this::processResponse,
                this::handleError,
                this::handleComplete
        );
    }

    private void resetState() {
        buffer.setLength(0);
        streamBuffer.clear();
        currentThinkingMarkdown = null;
        currentToolMarkdown = null;
        state = new InitialState(this);

        ui.access(() -> {
            details.removeAll();
            mainMessage.setMarkdown("");
        });
    }

    public void processResponse(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults().isEmpty()) {
            return;
        }

        Generation generation = chatResponse.getResult();

        if (isToolCall(generation)) {
            state.handleToolCall(generation);
            return;
        }

        String content = generation.getOutput().getText();
        if (content != null && !content.isEmpty()) {
            state.processContent(content);
        }
    }

    private boolean isToolCall(Generation generation) {
        return generation.getOutput().hasToolCalls();
    }

    public void onToolExecuted(String toolResult) {
        state.onToolExecuted(toolResult);
    }

    public void handleError(Throwable error) {
        ui.access(() -> mainMessage.appendMarkdownAsync("\n\n❌ Error: " + error.getMessage()));
    }

    public void handleComplete() {
        ui.access(() -> {
            if (buffer.isEmpty())
                return;
            mainMessage.appendMarkdownAsync(buffer.toString());
            buffer.setLength(0);
        });
    }





    // ==================== СОСТОЯНИЯ ====================

    abstract static class ProcessingState {
        protected final MarkdownMessageWithThinking2 context;

        protected ProcessingState(MarkdownMessageWithThinking2 context) {
            this.context = context;
        }

        abstract void processContent(String content);

        void handleToolCall(Generation generation) {
            // По умолчанию игнорируем
        }

        void onToolExecuted(String toolResult) {
            // По умолчанию игнорируем
        }
    }

    // ==================== INITIAL STATE ====================

    static class InitialState extends ProcessingState {

        InitialState(MarkdownMessageWithThinking2 context) {
            super(context);
        }

        @Override
        void processContent(String content) {
            context.getBuffer().append(content);
            String buffered = context.getBuffer().toString();

            int thinkStart = buffered.indexOf("<think>");

            if (thinkStart != -1) {
                // Найден <think>
                String beforeThink = buffered.substring(0, thinkStart);
                String afterThink = buffered.substring(thinkStart + 7);

                // Текст до <think> идет в main
                if (!beforeThink.trim().isEmpty()) {
                    context.getUi().access(() ->
                            context.getMainMessage().appendMarkdownAsync(beforeThink)
                    );
                }

                // Переходим в THINKING
                context.getBuffer().setLength(0);
                context.getBuffer().append(afterThink);

                context.setState(new ThinkingState(context));

                // Обрабатываем остаток
                if (!afterThink.isEmpty()) {
                    context.state.processContent(afterThink);
                }
            } else {
                // <think> не найден
                if (context.getBuffer().length() > 100) {
                    context.setState(new MainState(context));
                    String toFlush = context.getBuffer().substring(0, context.getBuffer().length() - 20);
                    context.getBuffer().delete(0, context.getBuffer().length() - 20);
                    context.getUi().access(() ->
                            context.getMainMessage().appendMarkdownAsync(toFlush)
                    );
                }
            }
        }
    }

    // ==================== THINKING STATE ====================

    static class ThinkingState extends ProcessingState {
        private static final String CLOSING_TAG = "</think>";

        ThinkingState(MarkdownMessageWithThinking2 context) {
            super(context);

            // Создаем новый Markdown в Details
            context.getUi().access(() -> {
                Markdown thinkingMd = new Markdown();
                context.setCurrentThinkingMarkdown(thinkingMd);
                context.getDetails().add(thinkingMd);
                context.getDetails().setOpened(true);
            });
        }

        @Override
        void processContent(String content) {
            context.getBuffer().append(content);
            String buffered = context.getBuffer().toString();

            if (buffered.trim().length() < CLOSING_TAG.length())
                return; // накапливаем токены

            int thinkEnd = buffered.indexOf(CLOSING_TAG);

            if (thinkEnd < 0) {
                // </think> не найден
                int stayInBuffer = CLOSING_TAG.length() - 1; // 1 сливаем т.к. тег не найден и все равно нужен хотябы один новый токен
                String toFlush = context.getBuffer().substring(0, context.getBuffer().length() - stayInBuffer);
                context.getBuffer().delete(0, toFlush.length());

                context.getUi().access(() -> {
                    if (context.getCurrentThinkingMarkdown() != null) {
                        context.getCurrentThinkingMarkdown().appendContent(toFlush);
                    }
                });

            } else {
                // Найден </think>
                String thinkingContent = buffered.substring(0, thinkEnd);
                String afterThink = buffered.substring(thinkEnd + CLOSING_TAG.length());

                // Добавляем thinking контент
                context.getUi().access(() -> {
                    if (context.getCurrentThinkingMarkdown() != null) {
                        context.getCurrentThinkingMarkdown().appendContent(thinkingContent);
                    }
                });

                // Возвращаемся в INITIAL
                context.getBuffer().setLength(0);
                context.getBuffer().append(afterThink);
//                context.setCurrentThinkingMarkdown(null); // TODO: проверить, нужно ли
                context.setState(new InitialState(context));

                // Обрабатываем остаток
                if (!afterThink.isEmpty()) {
                    context.state.processContent(afterThink);
                }
            }
        }

        @Override
        void handleToolCall(Generation generation) {
            // Получаем информацию о тулах из AssistantMessage
            var assistantMessage = generation.getOutput();

            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            String name = toolCalls.isEmpty() ? "" : ": " + toolCalls.get(0).name();
            String toolInfo = "🔧 **Tool call**%s\n\n".formatted(name);

            // Создаем новый Markdown для тула
            context.getUi().access(() -> {
                Markdown toolMd = new Markdown();
                context.setCurrentToolMarkdown(toolMd);
                toolMd.appendContent(toolInfo);
                context.getDetails().add(toolMd);
            });

            // Переходим в TOOL_CALLING
            context.getStreamBuffer().clear();
            context.setState(new ToolCallingState(context));
        }
    }

    // ==================== TOOL CALLING STATE ====================

    static class ToolCallingState extends ProcessingState {

        ToolCallingState(MarkdownMessageWithThinking2 context) {
            super(context);
        }

        @Override
        void processContent(String content) {
            // Буферизуем весь контент
            context.getStreamBuffer().add(content);
        }

        @Override
        void onToolExecuted(String toolResult) {
            context.getUi().access(() -> {
                if (context.getCurrentToolMarkdown() != null) {
                    context.getCurrentToolMarkdown().appendContent("✅ **Result**: " + toolResult + "\n\n");
                }
            });

            // Возвращаемся в THINKING
            context.setState(new ThinkingState(context));

            // Обрабатываем буфер
            for (String bufferedContent : context.getStreamBuffer()) {
                context.state.processContent(bufferedContent);
            }
            context.getStreamBuffer().clear();
            context.setCurrentToolMarkdown(null);
        }
    }

    // ==================== MAIN STATE ====================

    static class MainState extends ProcessingState {

        MainState(MarkdownMessageWithThinking2 context) {
            super(context);
        }

        @Override
        void processContent(String content) {
            context.getUi().access(() ->
                    context.getMainMessage().appendMarkdownAsync(content)
            );
        }
    }
}