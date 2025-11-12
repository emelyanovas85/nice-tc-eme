package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import at.nice.tc.service.JiraService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.Unit.PERCENTAGE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END;

@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final AiService aiService;
    private final JiraService jiraService;

    private SmartScroller scroll; // обертка для панели сообщений
    private VerticalLayout messageList; // панель сообщений
    private ChatInputComponent inputLayout; // textArea с кнопками


    private final Config config = new Config("browser", 70, 70, "", "", "Пользователь");

    /**
     * - mode        browser/extension (просто мета-инфа)
     * - heightPerc  высота чата внутри контейнера
     * - widthPerc   ширина чата внутри контенера
     * - scope       "", либо ASDKO-T777, либо ASDKO-C666, либо 12345
     * - userId      40FamiliaIO (в нижнем регистре)
     * - userFio     инициалы пользователя
     */
    @Data
    @AllArgsConstructor
    public static class Config {
        private String mode;
        private int heightPerc;
        private int widthPerc;
        private String scope;
        private String userId;
        private String userFio;
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters query = event.getLocation().getQueryParameters();
        query.getSingleParameter("mode").ifPresent(config::setMode);
        query.getSingleParameter("heightPerc").map(Integer::parseInt).ifPresent(config::setHeightPerc);
        query.getSingleParameter("widthPerc").map(Integer::parseInt).ifPresent(config::setWidthPerc);
        query.getSingleParameter("scope").ifPresent(config::setScope);
        query.getSingleParameter("userId").ifPresent(config::setUserId);
        query.getSingleParameter("userFio").map(ChatView::parseFio).ifPresent(config::setUserFio);

        initUI();
    }

    private static String parseFio(String fio) {
        return Arrays.stream(fio.split("\\s+"))
                .map(s -> s.substring(0, 1))
                .collect(Collectors.joining());
    }


    private void initUI() {
        Button toggleButton = new Button("Toggle theme", click -> {
            getElement().executeJs("document.documentElement.setAttribute('theme', document.documentElement.getAttribute('theme', document) === $0 ? $1 : $0)", Lumo.DARK, Lumo.LIGHT);
        });

        getContent().add(toggleButton);

//        String test = jiraService.getTestWithNestedMarkdown("VPEPVV-T2706").join();
////        test = jiraService.getTestWithNested("VPEPVV-T2706").join();
//        test = jiraService.getTestWithNestedMarkdown("VPEPVV-T800").join();
//        test = jiraService.getTestWithNestedMarkdown("CK7DITR007-T55").join();
//        test = jiraService.getTestWithNestedMarkdown("CK3DITP442-T1547").join();
//        test = testjiraService.getTestWithNested("VPEPVV-T2706").join();

        messageList = new VerticalLayout();

        scroll = new SmartScroller(messageList);
        scroll.setHeight(config.getHeightPerc(), PERCENTAGE);
        scroll.setWidth(config.getWidthPerc(), PERCENTAGE);

        getContent().addAndExpand(scroll);
        getContent().setAlignItems(CENTER);

        inputLayout = new ChatInputComponent();
        inputLayout.setWidthFull();
        inputLayout.getSendButton().addClickListener(this::onSubmit);
        inputLayout.getStopButton().addClickListener(this::onStop);
        inputLayout.setWidth(config.getWidthPerc(), PERCENTAGE);


        getContent().add(inputLayout);
        getContent().setSizeFull();
        inputLayout.showSendButton();
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stopChat();
        inputLayout.showSendButton();
    }

    private Disposable subscription;

    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getArea().getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        scroll.setStickDown(true);
        inputLayout.showStopButton();

        MarkdownMessage userMessage = new MarkdownMessage(userText, config.getUserFio(), LocalDateTime.now());
        userMessage.setUserColorIndex(3);
        messageList.add(userMessage);

        MarkdownMessageWithThinking botMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now());
        botMessage.getMainMessage().setUserColorIndex(5);
        messageList.add(botMessage);

        StringBuilder prompt = new StringBuilder();
        if (!config.getUserId().isBlank())
            prompt.append("Меня зовут ").append(config.getUserFio()).append(". Обращайся по имени.\n");
        if (!config.getScope().isBlank())
            prompt.append("Я нахожусь на странице ").append(config.getScope()).append(" (определи - ключ теста, прогона или id версии теста).\n");
        prompt.append("\n").append(userText);


        getUI().ifPresent(ui -> {
            Flux<String> responseFlux = aiService.sendMessageStream(prompt.toString());
            inputLayout.area.clear();
            subscription = responseFlux.subscribe(
                    token -> ui.access(() -> {
                        botMessage.appendMarkdownAsync(token);
                        scroll.scrollToBottom();
                    }),
                    err -> ui.access(() -> {
                        botMessage.appendMarkdownAsync("\n\nОшибка: " + err.getMessage());
                        inputLayout.showSendButton();
                        subscription = null;
                    }),
                    () -> ui.access(() -> {
                        botMessage.finish();
                        inputLayout.showSendButton();
                        subscription = null;
                    }));
        });
    }

    private void stopChat() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subscription = null;
        }
    }


    /**
     * Поле для ввода текста и кнопки "Отправить" и "Стоп"
     */
    @Getter
    public static class ChatInputComponent extends HorizontalLayout {

        private final TextArea area = new TextArea();
        private final Button sendButton = new Button("Отправить");
        private final Button stopButton = new Button("Стоп");

        public ChatInputComponent() {
            configureInput();
            configureButtons();
            addComponents();
            setLayoutDefaults();
        }

        private void configureButtons() {
            sendButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

            sendButton.setWidth("8em");
            stopButton.setWidth("8em");
        }

        private void configureInput() {
            area.setPlaceholder("Напишите ваше сообщение здесь...");
            area.setValueChangeMode(ValueChangeMode.EAGER); // Реагировать сразу на изменения
            area.addFocusListener(e -> area.setPlaceholder(""));
            area.addBlurListener(e -> area.setPlaceholder("Напишите ваше сообщение здесь..."));
        }

        private void addComponents() {
            add(area, sendButton, stopButton);
        }

        private void setLayoutDefaults() {
            setPadding(true);
            setSpacing(true);
            // Растягиваем TextField по ширине родителя
            area.setWidthFull();
            setVerticalComponentAlignment(END, area, sendButton, stopButton);
        }

        public void showSendButton() {
            sendButton.setVisible(true);
            stopButton.setVisible(false);
        }

        public void showStopButton() {
            sendButton.setVisible(false);
            stopButton.setVisible(true);
        }
    }


    /**
     * Расширяет стандартный {@link Scroller} методом {@link #scrollToBottom()},
     * который скроллит к низу панели, если установлен флаг {@link #stickDown}
     */
    public static class SmartScroller extends Scroller {
        private final AtomicBoolean stickDown = new AtomicBoolean(true);

        public SmartScroller(Component content) {
            super(content);
            addAttachListener(e -> {
                getElement().executeJs(
                        // language=jav
                        """
                                    var el = this;
                                    var lastScrollTop = 0;
                                
                                    el.addEventListener("scroll", function(e) {
                                        var currentScrollTop = el.scrollTop;
                                
                                        if (currentScrollTop < lastScrollTop) { // Скролл вверх
                                            el.$server.onScrollUp();
                                
                                        } else if (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) { // достигли дна (добавляем небольшой допуск (1px) для защиты от ошибок округления)
                                            el.$server.onScrolledToBottom();
                                        }
                                        lastScrollTop = currentScrollTop;
                                    });
                                """,
                        getElement()
                );
            });
        }

        /**
         * вызывается из javascript
         */
        @SuppressWarnings("unused")
        @ClientCallable
        public void onScrollUp() {
            setStickDown(false);
        }

        /**
         * вызывается из javascript
         */
        @SuppressWarnings("unused")
        @ClientCallable
        public void onScrolledToBottom() {
            setStickDown(true);
        }

        /**
         * Переключает флаг: true - скроллить, false - не скроллить
         */
        public void setStickDown(boolean flag) {
            stickDown.set(flag);
        }

        @Override
        public void scrollToBottom() {
            if (stickDown.get())
                super.scrollToBottom();
        }
    }


    @Getter
    public static class MarkdownMessageWithThinking extends VerticalLayout {

        private Details thinkingDetails;
        private Markdown thinkingMessage;
        private final MarkdownMessage mainMessage;

        private ProcessingState state;

        private static final String THINK_OPEN = "<think>";
        private static final String THINK_CLOSE = "</think>";

        public MarkdownMessageWithThinking(String name, LocalDateTime timestamp) {
            mainMessage = new MarkdownMessage(name, timestamp);
            add(mainMessage);
            state = new InitialState();
        }

        public void appendMarkdownAsync(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return;
            }

            getUI().ifPresent(ui -> ui.access(() -> {
                state = state.process(chunk, this);
            }));
        }

        public void ensureThinkingDetailsCreated() {
            if (thinkingDetails == null) {
                thinkingMessage = new Markdown();
                thinkingDetails = new Details("Размышления модели", thinkingMessage);
                thinkingDetails.setOpened(true);

                state.addChangeStateListener((oldState, newState) -> {
                    // когда размышления закончатся:
                    if (newState.getClass() == MainState.class) {
                        getUI().ifPresent(ui -> ui.access(() -> thinkingDetails.setOpened(false)));
                    }
                });

                addComponentAsFirst(thinkingDetails);
            }
        }

        public void finish() {
            getUI().ifPresent(ui -> ui.access(() -> {
                state.flush(this);
            }));
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
            private final StringBuilder buffer = new StringBuilder();

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
                    context.mainMessage.appendMarkdownAsync(buffer.toString());
                    return new MainState();
                }
            }

            @Override
            public void flush(MarkdownMessageWithThinking context) {
                if (!buffer.isEmpty()) {
                    context.mainMessage.appendMarkdownAsync(buffer.toString());
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
                    context.mainMessage.appendMarkdownAsync(remaining);
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

}
