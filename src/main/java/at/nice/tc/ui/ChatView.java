package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vaadin.flow.component.Unit.PERCENTAGE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END;

@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> {

    private final AiService aiService;

    private SmartScroller scroll;
    private VerticalLayout messageList;
    private ChatInputComponent inputLayout;
    private boolean chatActive;
    private Disposable subscription;


    @PostConstruct
    private void initUI() {
        messageList = new VerticalLayout();
        chatActive = true;

        scroll = new SmartScroller(messageList);
        scroll.setHeight(70, PERCENTAGE);
        scroll.setWidth(70, PERCENTAGE);

        getContent().addAndExpand(scroll);
        getContent().setAlignItems(CENTER);

        inputLayout = new ChatInputComponent();
        inputLayout.setWidthFull();
        inputLayout.getSendButton().addClickListener(this::onSubmit);
        inputLayout.getStopButton().addClickListener(this::onStop);
        inputLayout.setWidth(70, PERCENTAGE);

        getContent().add(inputLayout);
        getContent().setSizeFull();
        setSendButtonToSend();
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stopChat();
        setSendButtonToSend();
    }

    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getArea().getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        scroll.setStickDown(true);
        setSendButtonToStop();

        MarkdownMessage userMessage = new MarkdownMessage(userText, "Пользователь", LocalDateTime.now());
        userMessage.setUserColorIndex(3);
        messageList.add(userMessage);

        MarkdownMessage botMessage = new MarkdownMessage("Агент Jira", LocalDateTime.now());
        botMessage.setUserColorIndex(5);
        messageList.add(botMessage);


        Optional<UI> uiOptional = buttonClickEvent.getSource().getUI();
        uiOptional.ifPresent(ui -> {
            Flux<String> responseFlux = aiService.sendMessageStream(userText);
            inputLayout.area.clear();
            subscription = responseFlux.subscribe(
                    token -> ui.access(() -> {
                        botMessage.appendMarkdownAsync(token);
                        scroll.scrollIfNeeded();
                    }),
                    err -> ui.access(() -> {
                        botMessage.setMarkdown("Ошибка: " + err.getMessage());
                        setSendButtonToSend();
                        subscription = null;
                    }),
                    () -> ui.access(() -> {
                        setSendButtonToSend();
                        subscription = null;
                    }));
        });
    }

    private void setSendButtonToSend() {
        inputLayout.sendButton.setVisible(true);
        inputLayout.stopButton.setVisible(false);
        chatActive = true;
    }

    private void setSendButtonToStop() {
        inputLayout.sendButton.setVisible(false);
        inputLayout.stopButton.setVisible(true);
        chatActive = false;
    }

    private void stopChat() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subscription = null;
        }
        chatActive = false;
    }


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
    }


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
                                
                                        } else if (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) { // достигли дна
                                            // Проверяем достаточно ли точные вычисления для дна
                                            // Добавляем небольшой допуск (1px) для защиты от ошибок округления
                                            el.$server.onScrolledToBottom();
                                        }
                                        lastScrollTop = currentScrollTop;
                                    });
                                """,
                        getElement()
                );
            });
        }

        @ClientCallable
        public void onScrollUp() {
            setStickDown(false);
        }

        @ClientCallable
        public void onScrolledToBottom() {
            setStickDown(true);
        }

        public void setStickDown(boolean flag) {
            stickDown.set(flag);
        }

        public void scrollIfNeeded() {
            if (stickDown.get())
                scrollToBottom();
        }
    }
}
