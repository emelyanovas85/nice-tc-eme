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
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Getter
public class MarkdownMessageWithThinking extends VerticalLayout {

    private Details thinkingDetails;
    private VerticalLayout thinkingContent;
    private Markdown thinkingMessage;
    private final MarkdownMessage mainMessage;

    private ProcessingState state;
    private final EventHandlers handlers = new EventHandlers();
    private final AiToolCallService aiToolCallService;

    public MarkdownMessageWithThinking(String name, LocalDateTime timestamp, AiToolCallService aiToolCallService) {
        this.aiToolCallService = aiToolCallService;
        mainMessage = new MarkdownMessage(name, timestamp);
        add(mainMessage);
        state = new InitialState(this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (thinkingDetails != null)
            thinkingDetails.setOpened(!(state instanceof MainState));
    }

    public void setMarkdown(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return;
        }

        // Сбрасываем состояние в InitialState при установке полного текста
        // чтобы избежать накопления данных в буферах предыдущих состояний
        state = new InitialState(this);
        state = state.process(fullText);
    }

    public void appendMarkdownAsync(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        getUI().ifPresent(ui -> { // без этого чанки путаются местами
            if (ui.isAttached())
                ui.access(() -> state = state.process(chunk));
        });
    }

    public void ensureThinkingDetailsCreated() {
        if (thinkingDetails == null) {
            thinkingContent = new VerticalLayout() {{
                setPadding(false);
                setSpacing(false);
            }};
            thinkingDetails = new Details("Размышления модели", thinkingContent);
            thinkingDetails.setOpened(true);

            state.addChangeStateListener((oldState, newState) -> {
                // когда размышления закончатся:
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
        if (!isThinkingMessageLatestElement())
            addNewThinkingMarkdown();

        if (event instanceof CheckEvent.AgentBuiltTestTreeEvent e) {
            addNewThinkingMarkdown();
            Component treeSection = getHandlers().check.doOnBuiltTestTree(e);
            UiUtils.doInUI(this, () -> thinkingContent.add(treeSection));

//        } else if (event instanceof CheckEvent.CheckPreparingEvent e) {
//            UiUtils.doInUI(this, () -> getHandlers().check.doOnCheckPreparing(e));

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

                markdown.appendContent(timestamp() + "\t" + logEvent.getText() + "  \n");

                // Для аттачментов используем компоненты Vaadin вместо HTML в markdown
                // потому что Vaadin Markdown может не рендерить HTML теги <details>
                if (!logEvent.getAttachments().isEmpty()) {
                    ensureThinkingDetailsCreated();
                    if (thinkingContent == null) {
                        return; // На всякий случай проверяем
                    }

                    UiUtils.doInUI(MarkdownMessageWithThinking.this, () -> {
                        // Убеждаемся, что thinkingDetails открыт, чтобы аттачменты были видны
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
            // Добавляем Details сразу после соответствующего markdown
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


        String timestamp() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("`dd.MM HH:mm:ss`\t"));
        }
    }

}
