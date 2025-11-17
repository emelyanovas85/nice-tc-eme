package at.nice.tc.ui.components;

import at.nice.tc.events.*;
import at.nice.tc.model.TestTree;
import at.nice.tc.ui.ChatView;
import at.nice.tc.utils.JiraUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class MarkdownMessageWithThinking extends VerticalLayout {

    private Details thinkingDetails;
    private Markdown thinkingMessage;
    private final MarkdownMessage mainMessage;

    private ProcessingState state;
    private final EventHandlers handlers = new EventHandlers();

    private static final String THINK_OPEN = "<think>";
    private static final String THINK_CLOSE = "</think>";

    public MarkdownMessageWithThinking(String name, LocalDateTime timestamp) {
        mainMessage = new MarkdownMessage(name, timestamp);
        add(mainMessage);
        state = new InitialState();
    }

    public void setMarkdown(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return;
        }

        state = state.process(fullText, this);
    }

    public void appendMarkdownAsync(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        getUI().ifPresent(ui -> { // без этого чанки путаются местами
            if (ui.isAttached())
                ui.access(() -> state = state.process(chunk, this));
        });
    }

    public void ensureThinkingDetailsCreated() {
        if (thinkingDetails == null) {
            thinkingMessage = new Markdown();
            thinkingDetails = new Details("Размышления модели", thinkingMessage);
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

    public void finish() {
        state.flush(this);
    }


    @RequiredArgsConstructor
    public class EventHandlers {

        public final LogEvents log = new LogEvents();
        public final CheckEvents check = new CheckEvents();


        public class LogEvents {

            public void doOnLog(LogEvent log) {
                if (isExpectedState(log.getConversationId())) {
                    thinkingMessage.appendContent(timestamp() + "\t" + log.getText() + "\n");
                    log.getAttachments().forEach(a -> thinkingMessage.appendContent(//"\n" +
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
            public void doOnBuiltTestTree(CheckEvent.AgentBuiltTestTreeEvent event) {
                if (isExpectedState(event.getConversationId())) {
                    TestTree test = event.getTest();
                    thinkingMessage.appendContent(timestamp() + "Построено дерево тестов для проверки:\n");

                    testId$text.clear();
                    test.getDescendants().forEach(t -> testId$text.put(t.getId(), "⏸️ " + t + " ожидает проверки"));

                    String tree = JiraUtils.toMarkdownTree(test, t -> testId$text.get(t.getId()));
                    getUI().ifPresent(ui -> ui.access(() ->
                            thinkingMessage.appendContent(tree)
                    ));
                }
            }

            /**
             * Меняет статус теста на "▶️️️" (in progress)
             */
            public void doOnCheckStarted(CheckEvent.CheckStartedEvent event) {
                changeText(event, test -> "▶️️ " + test + " проверяется (<a href=\"/?chatId=%s\" target=\"_blank\">%1$s</a>)");
            }

            /**
             * Меняет статус теста на "✅️" (completed)
             */
            public void doOnCheckFinished(CheckEvent.CheckFinishedEvent event) {
                changeText(event, test -> "✅ " + test + " проверен (<a href=\"/?chatId=%s\" target=\"_blank\">%1$s</a>)");
            }

            void changeText(CheckEvent event, Function<TestTree.Test, String> stringifier) {
                String conversationId = event.getConversationId();
                if (isExpectedState(conversationId)) {
                    TestTree.Test test = (TestTree.Test) event.getTest();

                    String oldText = testId$text.get(test.getId());
                    String newText = stringifier.apply(test);
                    testId$text.put(test.getId(), newText);

                    String content = thinkingMessage.getContent().replace(oldText, newText);
                    getUI().ifPresent(ui -> ui.access(() ->
                            thinkingMessage.setContent(content)
                    ));
                }
            }

        }

        boolean isExpectedState(String conversationId) {
            return state instanceof ThinkingState && conversationId.startsWith(pageConversationId());
        }

        String pageConversationId() {
            return UI.getCurrent()
                    .getActiveViewLocation()
                    .getQueryParameters()
                    .getSingleParameter(ChatView.Constants.CHAT_ID)
                    .orElse("");
        }

        String timestamp() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH.mm.ss\t"));
        }
    }



    /**
     * State Pattern:
     * - InitialState   → проверка первых 7+ символов на предмет наличия <think>
     * - ThinkingState  → передача потока в thinkingMessage + поиск </think> с помощью буферизации
     * - MainState      → прямая передача потока в mainMessage (без буферизации)
     */
    //
    private static abstract class ProcessingState {
        public abstract ProcessingState process(String chunk, MarkdownMessageWithThinking context);

        public abstract void flush(MarkdownMessageWithThinking context);

        public void checkUiAccessed(Consumer<Boolean> act) {
            UI ui = UI.getCurrent();
            if (ui == null) {
                act.accept(false);
                return;
            }
            act.accept(ui.isAttached());
        }

        // <editor-fold desc="Функциональность слушателей" defaultstate="collapsed">

        private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
        private static ProcessingState currentState;

        public void addChangeStateListener(ProcessingState.Listener l) {
            LISTENERS.add(l);
        }

        {
            LISTENERS.forEach(l -> l.changed(currentState, this));
            currentState = this;
        }

        @FunctionalInterface
        public interface Listener {
            void changed(ProcessingState oldState, ProcessingState newState);
        }
        // </editor-fold>
    }

    // Начальное состояние: проверяем первые 7 символов
    private static class InitialState extends ProcessingState {
        private final StringBuilder  buffer = new StringBuilder();

        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            buffer.append(chunk);

            if (buffer.length() < THINK_OPEN.length()) {
                return this; // Ждём ещё данных
            }

            int tagPos = buffer.indexOf(THINK_OPEN);
            if (tagPos >= 0) {
                // Есть тег - переходим в thinking режим
                context.ensureThinkingDetailsCreated();
                buffer.delete(tagPos, tagPos + THINK_OPEN.length());
                return new ThinkingState().process(buffer.toString(), context);
            } else {
                // Нет тега - переходим в обычный режим
                checkUiAccessed(isAccessed -> {
                    final String markdownSnippet = buffer.toString();
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(markdownSnippet);
                    else
                        mainMessage.appendMarkdown(markdownSnippet);
                });
                return new MainState();
            }
        }

        @Override
        public  void flush(MarkdownMessageWithThinking context) {
            if (!buffer.isEmpty()) {
                checkUiAccessed(isAccessed -> {
                    final String markdownSnippet = buffer.toString();
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(markdownSnippet);
                    else
                        mainMessage.appendMarkdown(markdownSnippet);
                });
            }
        }
    }

    // Thinking режим: буферизация и поиск </think>
    private static class ThinkingState extends ProcessingState {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            buffer.append(chunk);

            String text = buffer.toString();
            int closeIdx = text.indexOf(THINK_CLOSE);

            if (closeIdx >= 0) {
                // Нашли закрывающий тег
                return handleCloseTag(closeIdx, text, context);
            } else {
                // Закрывающего тега нет - отдаём безопасную часть
                flushSafePart(context);
                return this;
            }
        }

        private ProcessingState handleCloseTag(int closeIdx, String text, MarkdownMessageWithThinking context) {
            if (closeIdx > 0) {
                context.thinkingMessage.appendContent(text.substring(0, closeIdx));
            }

            String remaining = text.substring(closeIdx + THINK_CLOSE.length());
            if (!remaining.isEmpty()) {
                checkUiAccessed(isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(remaining);
                    else
                        mainMessage.appendMarkdown(remaining);
                });
            }

            return new InitialState();
        }

        private void flushSafePart(MarkdownMessageWithThinking context) {
            int safeLength = Math.max(0, buffer.length() - THINK_CLOSE.length());
            if (safeLength > 0) {
                context.thinkingMessage.appendContent(buffer.substring(0, safeLength));
                buffer.delete(0, safeLength);
            }
        }

        @Override
        public void flush(MarkdownMessageWithThinking context) {
            if (!buffer.isEmpty()) {
                context.thinkingMessage.appendContent(buffer.toString());
            }
        }
    }

    // Обычный режим: прямая передача без буфера
    private static class MainState extends ProcessingState {
        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            context.mainMessage.appendMarkdownAsync(chunk);
            return this;
        }

        @Override
        public void flush(MarkdownMessageWithThinking context) {
            // Нечего сбрасывать - буфера нет
        }
    }
}
