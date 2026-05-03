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
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

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
    private final MarkdownMessage mainMessage;

    private volatile ProcessingState state;
    private final EventHandlers handlers = new EventHandlers();
    private final AiToolCallService aiToolCallService;
    private final String authorName;
    private final LocalDateTime timestamp;

    // Слушатели смены состояния — пер экземпляр, не статические!
    private final List<ProcessingState.Listener> stateListeners = new CopyOnWriteArrayList<>();

    public MarkdownMessageWithThinking(String name, LocalDateTime timestamp, AiToolCallService aiToolCallService) {
        this.aiToolCallService = aiToolCallService;
        this.authorName = name;
        this.timestamp = timestamp;

        addClassNames("chat-message", "assistant-message");

        mainMessage = new MarkdownMessage(name, timestamp);
        add(mainMessage);
        state = new InitialState(this);
    }

    /**
     * Уведомляет всех зарегистрированных слушателей о смене состояния.
     * Вызывается из конструктора ProcessingState.
     */
    public void notifyStateChanged(ProcessingState newState) {
        stateListeners.forEach(l -> l.changed(newState));
    }

    /**
     * Добавляет слушатель смены состояния для этого конкретного сообщения.
     */
    public void addChangeStateListener(ProcessingState.Listener l) {
        stateListeners.add(l);
    }

    /**
     * Устанавливает тип сообщения (пользователь или ИИ)
     */
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (thinkingDetails != null)
            thinkingDetails.setOpened(!(state instanceof MainState));
    }

    /**
     * Устанавливает полный текст сообщения.
     * Всегда откладывает рендеринг через addAttachListener,
     * чтобы гарантировать что MarkdownMessage уже прикреплён к DOM.
     */
    public void setMarkdown(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return;
        }

        // addAttachListener срабатывает когда компонент (и все его дети) прикреплены к DOM.
        // Это надёжнее чем isAttached(), который остаётся false
        // до следующего roundtrip даже после messageList.add().
        addAttachListener(e -> {
            state = new InitialState(this).process(fullText);
        });
    }

    public void appendMarkdownAsync(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        getUI().ifPresent(ui -> { // без этого чанки путаются местами
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
            thinkingContent = new VerticalLayout() {{
                setPadding(false);
                setSpacing(false);
                addClassName("thinking-content");
            }};

            thinkingDetails = new Details("Размышления модели", thinkingContent);
            thinkingDetails.addClassName("thinking-details");
            thinkingDetails.setOpened(true);

            // Слушатель на этом екземпляре: когда размышления закончатся — свернуть Details
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
        if (thinkingMessage == null)
            return false; // нужно создавать
        return thinkingContent.indexOf(thinkingMessage) == thinkingContent.getComponentCount() - 1;
    }

    public void finish() {
        state.flush();
    }


    /**
     * Обрабатывает событие, логика обработки в {@link EventHandlers}
     * Выполняются в UI потоке (синхронно)
     */
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

            /**
             * Обрабатывает ToolEvent, добавляя его в указанный Markdown компонент
             */
            public void doOnLog(ToolEvent logEvent, Markdown markdown) {
                if (markdown == null)
                    return;

                markdown.appendContent(тиместамп() + "\t" + logEvent.getText() + "  \n");

                if (!logEvent.getAttachments().isEmpty()) {
                    ensureThinkingDetailsCreated();
                    if (thinkingContent == null) {
                        return;
                    }

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
            VerticalLayout content = new VerticalLayout(value) {{
                setPadding(false);
                setSpacing(false);
            }};
            Details details = new Details(a.getName(), content);
            details.getStyle().set("margin-left", "2em");

            thinkingContent.add(details);
            value.appendContent(a.getContent());
        }


        String тиместамп() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("`dd.MM HH:mm:ss`\t"));
        }
    }

}
