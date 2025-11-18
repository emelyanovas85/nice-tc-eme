package at.nice.tc.ui.components;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.UI;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public class MarkdownMessageWithThinking2 extends VerticalLayout {

    private final Details details;
    private final MarkdownMessage markdownMessage;
    private final UI ui;

    private ProcessingState currentState;
    private final StringBuilder buffer;
    private Markdown currentThinkingMarkdown;
    private Markdown currentToolMarkdown;
    private final List<String> streamBuffer;

    public MarkdownMessageWithThinking2() {
        this.details = new Details("Thinking & Tools");
        this.markdownMessage = new MarkdownMessage();
        this.ui = UI.getCurrent();
        this.buffer = new StringBuilder();
        this.streamBuffer = new ArrayList<>();

        details.setOpened(false);

        add(details, markdownMessage);
        setPadding(false);
        setSpacing(false);

        this.currentState = new InitialState(this);
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
        currentState = new InitialState(this);

        ui.access(() -> {
            details.removeAll();
            markdownMessage.setMarkdown("");
        });
    }

    private void processResponse(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResults().isEmpty()) {
            return;
        }

        Generation generation = chatResponse.getResult();

        if (isToolCall(generation)) {
            currentState.handleToolCall(generation);
            return;
        }

        String content = generation.getOutput().getText();
        if (content != null && !content.isEmpty()) {
            currentState.processContent(content);
        }
    }

    private boolean isToolCall(Generation generation) {
        return generation.getOutput().hasToolCalls();
    }

    public void onToolExecuted(String toolResult) {
        currentState.onToolExecuted(toolResult);
    }

    private void handleError(Throwable error) {
        ui.access(() -> markdownMessage.appendMarkdownAsync("\n\n❌ Error: " + error.getMessage()));
    }

    private void handleComplete() {
        ui.access(() -> {
            if (buffer.isEmpty())
                return;
            markdownMessage.appendMarkdownAsync(buffer.toString());
            buffer.setLength(0);
        });
    }

    // Getters для состояний
    void setState(ProcessingState newState) {
        this.currentState = newState;
    }

    StringBuilder getBuffer() {
        return buffer;
    }

    MarkdownMessage getMarkdownMessage() {
        return markdownMessage;
    }

    Details getDetails() {
        return details;
    }

    UI getUi() {
        return ui;
    }

    Markdown getCurrentThinkingMarkdown() {
        return currentThinkingMarkdown;
    }

    void setCurrentThinkingMarkdown(Markdown markdown) {
        this.currentThinkingMarkdown = markdown;
    }

    Markdown getCurrentToolMarkdown() {
        return currentToolMarkdown;
    }

    void setCurrentToolMarkdown(Markdown markdown) {
        this.currentToolMarkdown = markdown;
    }

    List<String> getStreamBuffer() {
        return streamBuffer;
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
                            context.getMarkdownMessage().appendMarkdownAsync(beforeThink)
                    );
                }

                // Переходим в THINKING
                context.getBuffer().setLength(0);
                context.getBuffer().append(afterThink);

                // Создаем новый Markdown в Details
                context.getUi().access(() -> {
                    Markdown thinkingMd = new Markdown();
                    context.setCurrentThinkingMarkdown(thinkingMd);
                    context.getDetails().add(thinkingMd);
                    context.getDetails().setOpened(true);
                });

                context.setState(new ThinkingState(context));

                // Обрабатываем остаток
                if (!afterThink.isEmpty()) {
                    context.currentState.processContent(afterThink);
                }
            } else {
                // <think> не найден
                if (context.getBuffer().length() > 100) {
                    context.setState(new MainState(context));
                    String toFlush = context.getBuffer().substring(0, context.getBuffer().length() - 20);
                    context.getBuffer().delete(0, context.getBuffer().length() - 20);
                    context.getUi().access(() ->
                            context.getMarkdownMessage().appendMarkdownAsync(toFlush)
                    );
                }
            }
        }
    }

    // ==================== THINKING STATE ====================

    static class ThinkingState extends ProcessingState {

        ThinkingState(MarkdownMessageWithThinking2 context) {
            super(context);
        }

        @Override
        void processContent(String content) {
            context.getBuffer().append(content);
            String buffered = context.getBuffer().toString();

            int thinkEnd = buffered.indexOf("</think>");

            if (thinkEnd != -1) {
                // Найден </think>
                String thinkingContent = buffered.substring(0, thinkEnd);
                String afterThink = buffered.substring(thinkEnd + 8);

                // Добавляем thinking контент
                context.getUi().access(() -> {
                    if (context.getCurrentThinkingMarkdown() != null) {
                        context.getCurrentThinkingMarkdown().appendContent(thinkingContent);
                    }
                });

                // Возвращаемся в INITIAL
                context.getBuffer().setLength(0);
                context.getBuffer().append(afterThink);
                context.setCurrentThinkingMarkdown(null);
                context.setState(new InitialState(context));

                // Обрабатываем остаток
                if (!afterThink.isEmpty()) {
                    context.currentState.processContent(afterThink);
                }
            } else {
                // </think> не найден, продолжаем накапливать
                if (context.getBuffer().length() > 100) {
                    String toFlush = context.getBuffer().substring(0, context.getBuffer().length() - 20);
                    context.getBuffer().delete(0, context.getBuffer().length() - 20);

                    context.getUi().access(() -> {
                        if (context.getCurrentThinkingMarkdown() != null) {
                            context.getCurrentThinkingMarkdown().appendContent(toFlush);
                        }
                    });
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
                context.currentState.processContent(bufferedContent);
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
                    context.getMarkdownMessage().appendMarkdownAsync(content)
            );
        }
    }
}