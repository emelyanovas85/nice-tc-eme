package at.nice.tc.ui.components;

import at.nice.tc.events.ChatEvent;
import at.nice.tc.events.ToolEvent;
import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.TestTree;
import at.nice.tc.service.AiToolCallService;
import at.nice.tc.ui.components.state.InitialState;
import at.nice.tc.ui.components.state.MainState;
import at.nice.tc.ui.components.state.ProcessingState;
import at.nice.tc.utils.JiraUtils;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
            thinkingMessage = new Markdown();
            thinkingContent.add(thinkingMessage);
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
        getUI().ifPresentOrElse(
            ui -> {
                if (ui.isAttached()) {
                    ui.access(() -> thinkingContent.add(thinkingMessage));
                } else {
                    thinkingContent.add(thinkingMessage);
                }
            },
            () -> thinkingContent.add(thinkingMessage)
        );
        return thinkingMessage;
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
            getHandlers().check.doOnBuiltTestTree(e, thinkingMessage);

        } else if (event instanceof CheckEvent.CheckStartedEvent e) {
            getHandlers().check.doOnCheckStarted(e, thinkingMessage);

        } else if (event instanceof CheckEvent.CheckFinishedEvent e) {
            getHandlers().check.doOnCheckFinished(e, thinkingMessage);

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
                if (markdown != null) {
                    markdown.appendContent(timestamp() + "\t" + logEvent.getText() + "\n");
                    logEvent.getAttachments().forEach(a -> markdown.appendContent(
                            """
                                    <details>
                                      <summary>%s</summary>
                                      
                                      %s
                                    </details>
                                    """.formatted(a.getName(), a.getContent())
                    ));
                }
            }
        }


        public class CheckEvents {

            private final Map<Integer, String> testId$text = new LinkedHashMap<>();


            /**
             * Добавление в размышление дерева тестов (многоуровневый маркированный список).
             * У каждого теста статус "⏸️" (pending)
             */
            public void doOnBuiltTestTree(CheckEvent.AgentBuiltTestTreeEvent event, Markdown markdown) {
                if (markdown == null) {
                    return;
                }

                TestTree test = event.getTest();
                markdown.appendContent(timestamp() + "Построено дерево тестов для проверки:\n");

                testId$text.clear();
                test.getDescendants().forEach(t -> testId$text.put(t.getId(), "⏸️ " + t + " ожидает проверки"));

                String tree = JiraUtils.toMarkdownTree(test, t -> testId$text.get(t.getId()));
                getUI().ifPresent(ui -> {
                    if (ui.isAttached()) {
                        ui.access(() -> markdown.appendContent(tree));
                    }
                });
            }

            /**
             * Меняет статус теста на "▶️️️" (in progress)
             */
            public void doOnCheckStarted(CheckEvent.CheckStartedEvent event, Markdown markdown) {
                changeText(event, markdown, test -> "▶️️ " + test + " проверяется (<a href=\"/?chatId=%s\" target=\"_blank\">%1$s</a>)");
            }

            /**
             * Меняет статус теста на "✅️" (completed)
             */
            public void doOnCheckFinished(CheckEvent.CheckFinishedEvent event, Markdown markdown) {
                changeText(event, markdown, test -> "✅ " + test + " проверен (<a href=\"/?chatId=%s\" target=\"_blank\">%1$s</a>)");
            }

            void changeText(CheckEvent event, Markdown markdown, Function<TestTree.Test, String> stringifier) {
                if (markdown == null) {
                    return;
                }

                TestTree.Test test = (TestTree.Test) event.getTest();

                String oldText = testId$text.get(test.getId());
                String newText = stringifier.apply(test);
                testId$text.put(test.getId(), newText);

                String content = markdown.getContent().replace(oldText, newText);
                getUI().ifPresent(ui -> {
                    if (ui.isAttached()) {
                        ui.access(() -> markdown.setContent(content));
                    }
                });
            }

        }

        String timestamp() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH.mm.ss\t"));
        }
    }

}
