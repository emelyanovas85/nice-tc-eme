package at.nice.tc.ui.components;

import at.nice.tc.events.ChatEvent;
import at.nice.tc.events.ToolEvent;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.Attachment;
import at.nice.tc.service.AiToolCallService;
import at.nice.tc.ui.components.state.InitialState;
import at.nice.tc.ui.components.state.MainState;
import at.nice.tc.ui.components.state.ProcessingState;
import at.nice.tc.utils.UiUtils;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
@CssImport(value = "./components/test-tree-styles.css")
public class MarkdownMessageWithThinking extends VerticalLayout {

    private Details thinkingDetails;
    private VerticalLayout thinkingContent;
    private Markdown thinkingMessage;

    private final SafeMarkdownMessage mainMessage;

    /**
     * Накапливает весь текст основного (main) сообщения.
     * Используется чтобы вызывать setMarkdown(fullText) вместо appendMarkdown,
     * что гарантирует рендеринг в браузере без зависимости от JS-буфера firitin.
     */
    private final StringBuilder mainTextBuffer = new StringBuilder();

    private volatile ProcessingState state;
    private final EventHandlers handlers = new EventHandlers();
    private final AiToolCallService aiToolCallService;
    private final String authorName;
    private final LocalDateTime timestamp;

    private final List<ProcessingState.Listener> stateListeners = new CopyOnWriteArrayList<>();

    public MarkdownMessageWithThinking(String name, LocalDateTime timestamp, AiToolCallService aiToolCallService) {
        this.aiToolCallService = aiToolCallService;
        this.authorName = name;
        this.timestamp = timestamp;

        addClassNames("chat-message", "assistant-message");

        mainMessage = new SafeMarkdownMessage(name, timestamp);
        add(mainMessage);
        state = new InitialState(this);
    }

    public void notifyStateChanged(ProcessingState newState) {
        stateListeners.forEach(l -> l.changed(newState));
    }

    public void addChangeStateListener(ProcessingState.Listener l) {
        stateListeners.add(l);
    }

    public void setMessageType(MessageType type) {
        removeClassNames("user-message", "assistant-message");
        switch (type) {
            case USER -> addClassName("user-message");
            case ASSISTANT -> addClassName("assistant-message");
        }
    }

    public enum MessageType {
        USER, ASSISTANT
    }

    /**
     * Добавляет текст в основное сообщение через setMarkdown(fullText).
     * Вызывается из ProcessingState (MainState, InitialState) — всегда в UI-потоке.
     * Использует setMarkdown вместо appendMarkdown для надёжного рендеринга.
     */
    public void appendMainText(String text) {
        mainTextBuffer.append(text);
        mainMessage.setMarkdown(mainTextBuffer.toString());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (thinkingDetails != null)
            thinkingDetails.setOpened(!(state instanceof MainState));
    }

    /**
     * Устанавливает полный текст сообщения (для восстановления из истории и для сообщений пользователя).
     * Вызывать только когда компонент уже в DOM (messageList.add вызван до этого метода).
     */
    public void setMarkdown(String fullText) {
        if (fullText == null || fullText.isEmpty()) return;
        mainTextBuffer.setLength(0);
        state = new InitialState(this).process(fullText);
    }

    /**
     * Добавляет чанк маркдауна асинхронно из фонового потока.
     * Внутри вызывает ui.access и затем process через прямой appendMainText.
     */
    public void appendMarkdownAsync(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        getUI().ifPresent(ui -> {
            if (ui.isAttached())
                ui.access(() -> {
                    ProcessingState current = state;
                    if (current != null) {
                        state = current.process(chunk);
                    }
                });
        });
    }

    public void ensureThinkingDetailsCreated() {
        if (thinkingDetails == null) {
            thinkingContent = new VerticalLayout();
            thinkingContent.setPadding(false);
            thinkingContent.setSpacing(false);
            thinkingContent.addClassName("thinking-content");

            thinkingDetails = new Details("Размышления модели", thinkingContent);
            thinkingDetails.addClassName("thinking-details");
            thinkingDetails.setOpened(true);

            addChangeStateListener(newState -> {
                if (newState instanceof MainState)
                    getUI().ifPresent(ui -> {
                        if (ui.isAttached())
                            ui.access(() -> thinkingDetails.setOpened(false));
                    });
            });

            addComponentAsFirst(thinkingDetails);
        }
    }

    public Markdown addNewThinkingMarkdown() {
        ensureThinkingDetailsCreated();
        thinkingMessage = new Markdown();
        thinkingMessage.addClassName("markdown");
        UiUtils.doInUI(this, () -> thinkingContent.add(thinkingMessage));
        return thinkingMessage;
    }

    public boolean isThinkingMessageLatestElement() {
        if (thinkingMessage == null) return false;
        return thinkingContent.indexOf(thinkingMessage) == thinkingContent.getComponentCount() - 1;
    }

    /**
     * Завершает стриминг: сбрасывает хвост буфера MainState (последние MAX_TAG_LEN символов
     * которые удерживались на случай тега).
     */
    public void finish() {
        state.flush();
    }

    public void handleEvent(ChatEvent event) {
        if (event instanceof CheckEvent.AgentBuiltTestTreeEvent e) {
            addNewThinkingMarkdown();
            Component treeSection = getHandlers().check.doOnBuiltTestTree(e);
            UiUtils.doInUI(this, () -> thinkingContent.add(treeSection));
        } else if (event instanceof CheckEvent.CheckPromptStartedEvent e) {
            UiUtils.doInUI(this, () -> getHandlers().check.doOnPromptStarted(e));
        } else if (event instanceof CheckEvent.CheckFinishedEvent e) {
            UiUtils.doInUI(this, () -> getHandlers().check.doOnCheckFinished(e));
        } else if (event instanceof ToolEvent e) {
            getHandlers().log.doOnLog(e, thinkingMessage);
        }
    }

    @RequiredArgsConstructor
    public class EventHandlers {

        public final LogEvents log = new LogEvents();
        public final CheckEvents check = new CheckEvents();

        public class LogEvents {
            public void doOnLog(ToolEvent logEvent, Markdown markdown) {
                if (markdown == null) return;

                markdown.appendContent(timestamp() + "\t" + logEvent.getText() + "  \n");

                if (!logEvent.getAttachments().isEmpty()) {
                    ensureThinkingDetailsCreated();
                    if (thinkingContent == null) return;

                    UiUtils.doInUI(MarkdownMessageWithThinking.this, () -> {
                        if (thinkingDetails != null && !thinkingDetails.isOpened()) {
                            thinkingDetails.setOpened(true);
                        }
                        for (var a : logEvent.getAttachments()) {
                            appendAttachment(a);
                        }
                    });
                }
            }
        }

        private void appendAttachment(Attachment a) {
            Markdown value = new Markdown();
            VerticalLayout content = new VerticalLayout(value);
            content.setPadding(false);
            content.setSpacing(false);
            Details details = new Details(a.getName(), content);
            details.getStyle().set("margin-left", "2em");
            thinkingContent.add(details);
            value.appendContent(a.getContent());
        }

        String timestamp() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("`dd.MM HH:mm:ss`\t"));
        }
    }
}
