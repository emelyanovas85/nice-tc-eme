package at.nice.tc.ui.components;

import at.nice.tc.events.*;
import at.nice.tc.model.TestTree;
import at.nice.tc.ui.ChatView;
import at.nice.tc.ui.MessageDelimiters;
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
    private VerticalLayout thinkingContent;
    private Markdown thinkingMessage;
    private final MarkdownMessage mainMessage;

    private ProcessingState state;
    private final EventHandlers handlers = new EventHandlers();

    private static final String THINK_OPEN = MessageDelimiters.THINK_OPEN;
    private static final String THINK_CLOSE = MessageDelimiters.THINK_CLOSE;
    private static final String TOOL_OPEN = MessageDelimiters.TOOL_OPEN;
    private static final String TOOL_CLOSE = MessageDelimiters.TOOL_CLOSE;

    public MarkdownMessageWithThinking(String name, LocalDateTime timestamp) {
        mainMessage = new MarkdownMessage(name, timestamp);
        add(mainMessage);
        state = new InitialState();
    }

    public void setMarkdown(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return;
        }

        // Сбрасываем состояние в InitialState при установке полного текста
        // чтобы избежать накопления данных в буферах предыдущих состояний
        state = new InitialState();
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
            thinkingContent = new VerticalLayout();
            thinkingContent.setPadding(false);
            thinkingContent.setSpacing(false);
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
        Markdown newMarkdown = new Markdown();
        getUI().ifPresentOrElse(
            ui -> {
                if (ui.isAttached()) {
                    ui.access(() -> thinkingContent.add(newMarkdown));
                } else {
                    thinkingContent.add(newMarkdown);
                }
            },
            () -> thinkingContent.add(newMarkdown)
        );
        return newMarkdown;
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
                    // Убеждаемся, что thinkingDetails создан
                    MarkdownMessageWithThinking.this.ensureThinkingDetailsCreated();
                    if (thinkingMessage != null) {
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
        }


        public class CheckEvents {

            private final Map<Integer, String> testId$text = new LinkedHashMap<>();


            /**
             * Добавление в размышление дерева тестов (многоуровневый маркированный список).
             * У каждого теста статус "⏸️" (pending)
             */
            public void doOnBuiltTestTree(CheckEvent.AgentBuiltTestTreeEvent event) {
                if (isExpectedState(event.getConversationId())) {
                    // Убеждаемся, что thinkingDetails создан
                    MarkdownMessageWithThinking.this.ensureThinkingDetailsCreated();
                    if (thinkingMessage == null) {
                        return;
                    }
                    
                    TestTree test = event.getTest();
                    thinkingMessage.appendContent(timestamp() + "Построено дерево тестов для проверки:\n");

                    testId$text.clear();
                    test.getDescendants().forEach(t -> testId$text.put(t.getId(), "⏸️ " + t + " ожидает проверки"));

                    String tree = JiraUtils.toMarkdownTree(test, t -> testId$text.get(t.getId()));
                    getUI().ifPresent(ui -> {
                        if (ui.isAttached()) {
                            ui.access(() -> {
                                if (thinkingMessage != null) {
                                    thinkingMessage.appendContent(tree);
                                }
                            });
                        }
                    });
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
                    // Убеждаемся, что thinkingDetails создан
                    MarkdownMessageWithThinking.this.ensureThinkingDetailsCreated();
                    if (thinkingMessage == null) {
                        return;
                    }
                    
                    TestTree.Test test = (TestTree.Test) event.getTest();

                    String oldText = testId$text.get(test.getId());
                    String newText = stringifier.apply(test);
                    testId$text.put(test.getId(), newText);

                    String content = thinkingMessage.getContent().replace(oldText, newText);
                    getUI().ifPresent(ui -> {
                        if (ui.isAttached()) {
                            ui.access(() -> {
                                if (thinkingMessage != null) {
                                    thinkingMessage.setContent(content);
                                }
                            });
                        }
                    });
                }
            }

        }

        boolean isExpectedState(String conversationId) {
            return state instanceof ThinkingState && conversationId.startsWith(pageConversationId());
        }

        String pageConversationId() {
            // Используем getUI() вместо UI.getCurrent(), так как события могут обрабатываться не в UI потоке
            return MarkdownMessageWithThinking.this.getUI()
                    .map(ui -> ui.getActiveViewLocation()
                            .getQueryParameters()
                            .getSingleParameter(ChatView.Constants.CHAT_ID)
                            .orElse(""))
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

        // Используем getUI() из контекста вместо UI.getCurrent(),
        // так как при восстановлении из истории UI.getCurrent() может быть null
        public void checkUiAccessed(MarkdownMessageWithThinking context, Consumer<Boolean> act) {
            context.getUI().ifPresentOrElse(
                ui -> act.accept(ui.isAttached()),
                () -> act.accept(false)
            );
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
        private boolean bufferSent = false; // Флаг для отслеживания, был ли буфер уже отправлен

        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            buffer.append(chunk);

            if (buffer.toString().trim().length() < THINK_OPEN.length()) {
                return this; // Ждём ещё данных
            }

            int tagPos = buffer.indexOf(THINK_OPEN);
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

            if (!bufferSent) {
                // Первый раз определяем отсутствие тега - отправляем весь буфер
                final String markdownSnippet = buffer.toString();
                bufferSent = true;

                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(markdownSnippet);
                    else
                        mainMessage.appendMarkdown(markdownSnippet);
                });

                // Очищаем буфер после отправки
                buffer.setLength(0);
            } else {
                // Буфер уже был отправлен, но из-за асинхронности мы все еще в InitialState
                // Отправляем только новый chunk, чтобы избежать дублирования
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(chunk);
                    else
                        mainMessage.appendMarkdown(chunk);
                });
                // Очищаем буфер, так как мы уже отправили новый chunk
                buffer.setLength(0);
            }

            // Переходим в MainState - следующие чанки будут обрабатываться там
            return new MainState();
        }

        @Override
        public  void flush(MarkdownMessageWithThinking context) {
            if (!buffer.isEmpty()) {
                checkUiAccessed(context, isAccessed -> {
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

    // Thinking режим: буферизация и поиск </think>, THINK_OPEN, TOOL_OPEN
    private static class ThinkingState extends ProcessingState {
        private final StringBuilder buffer = new StringBuilder();
        private final Markdown currentMarkdown;

        ThinkingState() {
            this.currentMarkdown = null;
        }

        ThinkingState(Markdown markdown) {
            this.currentMarkdown = markdown;
        }

        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            buffer.append(chunk);

            String text = buffer.toString();
            
            // Ищем все возможные теги и обрабатываем тот, который встречается первым
            int thinkOpenIdx = text.indexOf(THINK_OPEN);
            int toolOpenIdx = text.indexOf(TOOL_OPEN);
            int closeIdx = text.indexOf(THINK_CLOSE);

            // Определяем индекс первого тега
            int firstTagIdx = Integer.MAX_VALUE;
            String firstTag = null;

            if (thinkOpenIdx >= 0 && thinkOpenIdx < firstTagIdx) {
                firstTagIdx = thinkOpenIdx;
                firstTag = THINK_OPEN;
            }
            if (toolOpenIdx >= 0 && toolOpenIdx < firstTagIdx) {
                firstTagIdx = toolOpenIdx;
                firstTag = TOOL_OPEN;
            }
            if (closeIdx >= 0 && closeIdx < firstTagIdx) {
                firstTagIdx = closeIdx;
                firstTag = THINK_CLOSE;
            }

            if (firstTag != null) {
                if (firstTag.equals(THINK_OPEN)) {
                    return handleThinkOpen(firstTagIdx, text, context);
                } else if (firstTag.equals(TOOL_OPEN)) {
                    return handleToolOpen(firstTagIdx, text, context);
                } else {
                    // THINK_CLOSE
                    return handleCloseTag(firstTagIdx, text, context);
                }
            } else {
                // Закрывающего тега нет - отдаём безопасную часть
                flushSafePart(context);
                return this;
            }
        }

        private ProcessingState handleThinkOpen(int openIdx, String text, MarkdownMessageWithThinking context) {
            // Отправляем содержимое до THINK_OPEN в текущий markdown
            if (openIdx > 0) {
                Markdown markdown = getCurrentMarkdown(context);
                markdown.appendContent(text.substring(0, openIdx));
            }

            // Создаём новый Markdown компонент для нового блока размышлений
            Markdown newMarkdown = context.addNewThinkingMarkdown();

            // Очищаем буфер и обрабатываем оставшуюся часть после THINK_OPEN
            buffer.setLength(0);
            String remaining = text.substring(openIdx + THINK_OPEN.length());
            if (!remaining.isEmpty()) {
                return new ThinkingState(newMarkdown).process(remaining, context);
            }

            return new ThinkingState(newMarkdown);
        }

        private ProcessingState handleToolOpen(int openIdx, String text, MarkdownMessageWithThinking context) {
            // Отправляем содержимое до TOOL_OPEN в текущий markdown
            if (openIdx > 0) {
                Markdown markdown = getCurrentMarkdown(context);
                markdown.appendContent(text.substring(0, openIdx));
            }

            // Очищаем буфер и обрабатываем оставшуюся часть после TOOL_OPEN
            buffer.setLength(0);
            String remaining = text.substring(openIdx + TOOL_OPEN.length());
            if (!remaining.isEmpty()) {
                return new ToolCallingState().process(remaining, context);
            }

            return new ToolCallingState();
        }

        private ProcessingState handleCloseTag(int closeIdx, String text, MarkdownMessageWithThinking context) {
            Markdown markdown = getCurrentMarkdown(context);
            if (closeIdx > 0) {
                markdown.appendContent(text.substring(0, closeIdx));
            }

            // Очищаем буфер и обрабатываем оставшуюся часть после THINK_CLOSE
            buffer.setLength(0);
            String remaining = text.substring(closeIdx + THINK_CLOSE.length());
            if (!remaining.isEmpty()) {
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(remaining);
                    else
                        mainMessage.appendMarkdown(remaining);
                });
            }

            return new InitialState();
        }

        private Markdown getCurrentMarkdown(MarkdownMessageWithThinking context) {
            if (currentMarkdown != null) {
                return currentMarkdown;
            }
            context.ensureThinkingDetailsCreated();
            return context.thinkingMessage;
        }

        private void flushSafePart(MarkdownMessageWithThinking context) {
            // Безопасная длина: оставляем место для самого длинного тега
            int maxTagLength = Math.max(Math.max(THINK_CLOSE.length(), THINK_OPEN.length()), TOOL_OPEN.length());
            int safeLength = Math.max(0, buffer.length() - maxTagLength);
            if (safeLength > 0) {
                Markdown markdown = getCurrentMarkdown(context);
                markdown.appendContent(buffer.substring(0, safeLength));
                buffer.delete(0, safeLength);
            }
        }

        @Override
        public void flush(MarkdownMessageWithThinking context) {
            if (!buffer.isEmpty()) {
                Markdown markdown = getCurrentMarkdown(context);
                markdown.appendContent(buffer.toString());
            }
        }
    }

    // Tool calling режим: буферизация и поиск </tool>
    private static class ToolCallingState extends ProcessingState {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            buffer.append(chunk);

            String text = buffer.toString();
            
            // Ищем все возможные теги и обрабатываем тот, который встречается первым
            int thinkOpenIdx = text.indexOf(THINK_OPEN);
            int closeIdx = text.indexOf(TOOL_CLOSE);

            // Определяем индекс первого тега
            int firstTagIdx = Integer.MAX_VALUE;
            String firstTag = null;

            if (thinkOpenIdx >= 0 && thinkOpenIdx < firstTagIdx) {
                firstTagIdx = thinkOpenIdx;
                firstTag = THINK_OPEN;
            }
            if (closeIdx >= 0 && closeIdx < firstTagIdx) {
                firstTagIdx = closeIdx;
                firstTag = TOOL_CLOSE;
            }

            if (firstTag != null) {
                if (firstTag.equals(THINK_OPEN)) {
                    return handleThinkOpen(firstTagIdx, text, context);
                } else {
                    // TOOL_CLOSE
                    return handleCloseTag(firstTagIdx, text, context);
                }
            } else {
                // Закрывающего тега нет - отдаём безопасную часть
                flushSafePart(context);
                return this;
            }
        }

        private ProcessingState handleThinkOpen(int openIdx, String text, MarkdownMessageWithThinking context) {
            // Отправляем содержимое до THINK_OPEN в mainMessage
            if (openIdx > 0) {
                String beforeThink = text.substring(0, openIdx);
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(beforeThink);
                    else
                        mainMessage.appendMarkdown(beforeThink);
                });
            }

            // Создаём новый Markdown компонент для нового блока размышлений
            Markdown newMarkdown = context.addNewThinkingMarkdown();

            // Очищаем буфер и обрабатываем оставшуюся часть после THINK_OPEN
            buffer.setLength(0);
            String remaining = text.substring(openIdx + THINK_OPEN.length());
            if (!remaining.isEmpty()) {
                return new ThinkingState(newMarkdown).process(remaining, context);
            }

            return new ThinkingState(newMarkdown);
        }

        private ProcessingState handleCloseTag(int closeIdx, String text, MarkdownMessageWithThinking context) {
            // Отправляем содержимое до TOOL_CLOSE в mainMessage
            if (closeIdx > 0) {
                String beforeClose = text.substring(0, closeIdx);
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(beforeClose);
                    else
                        mainMessage.appendMarkdown(beforeClose);
                });
            }

            // Очищаем буфер и обрабатываем оставшуюся часть после TOOL_CLOSE
            buffer.setLength(0);
            String remaining = text.substring(closeIdx + TOOL_CLOSE.length());
            if (!remaining.isEmpty()) {
                return new InitialState().process(remaining, context);
            }

            return new InitialState();
        }

        private void flushSafePart(MarkdownMessageWithThinking context) {
            // Безопасная длина: оставляем место для самого длинного тега
            int maxTagLength = Math.max(THINK_OPEN.length(), TOOL_CLOSE.length());
            int safeLength = Math.max(0, buffer.length() - maxTagLength);
            if (safeLength > 0) {
                String safePart = buffer.substring(0, safeLength);
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(safePart);
                    else
                        mainMessage.appendMarkdown(safePart);
                });
                buffer.delete(0, safeLength);
            }
        }

        @Override
        public void flush(MarkdownMessageWithThinking context) {
            if (!buffer.isEmpty()) {
                checkUiAccessed(context, isAccessed -> {
                    final MarkdownMessage mainMessage = context.mainMessage;
                    if (isAccessed)
                        mainMessage.appendMarkdownAsync(buffer.toString());
                    else
                        mainMessage.appendMarkdown(buffer.toString());
                });
            }
        }
    }

    // Обычный режим: прямая передача без буфера
    private static class MainState extends ProcessingState {
        @Override
        public ProcessingState process(String chunk, MarkdownMessageWithThinking context) {
            checkUiAccessed(context, isAccessed -> {
                if (isAccessed)
                    context.mainMessage.appendMarkdownAsync(chunk);
                else
                    context.mainMessage.appendMarkdown(chunk);
            });
            return this;
        }

        @Override
        public void flush(MarkdownMessageWithThinking context) {
            // Нечего сбрасывать - буфера нет
        }
    }
}
