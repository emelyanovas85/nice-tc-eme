package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.vaadin.flow.component.Unit.PERCENTAGE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.*;

@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> {

    private final AiService aiService;
    private final MessageList messageList = new MessageList();
    private final String chatId = UUID.randomUUID().toString();
    private boolean chatActive = true;
    private ChatInputComponent messageInput;
    private Disposable subscription;


    @PostConstruct
    private void initUI() {

        Scroller scroller = new Scroller(messageList);
        scroller.setHeight(70, PERCENTAGE);
        scroller.setWidth(70, PERCENTAGE);

        getContent().addAndExpand(scroller);
        getContent().setAlignItems(CENTER);

        messageInput = new ChatInputComponent();
        messageInput.setWidthFull();
        messageInput.getSendButton().addClickListener(this::onSubmit);
        messageInput.getStopButton().addClickListener(this::onStop);
        messageInput.setWidth(70, PERCENTAGE);

        getContent().add(messageInput);
        getContent().setSizeFull();
        setSendButtonToSend();
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stopChat();
        setSendButtonToSend();
}

    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = messageInput.input.getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        setSendButtonToStop();

        MessageListItem userMessage = new MessageListItem(userText, Instant.now(), "Пользователь");
        userMessage.setUserColorIndex(3);
        messageList.addItem(userMessage);

        MessageListItem botMessage = new MessageListItem("", Instant.now(), "Агент Jira");
        botMessage.setUserColorIndex(5);
        messageList.addItem(botMessage);


        Optional<UI> uiOptional = buttonClickEvent.getSource().getUI();
        uiOptional.ifPresent(ui -> {
            Flux<String> responseFlux = aiService.sendMessageStream(userText);
            messageInput.input.clear();
            subscription = responseFlux.subscribe(
                    token -> ui.access(() -> botMessage.appendText(token)),
                    err -> ui.access(() -> {
                        botMessage.setText("Ошибка: " + err.getMessage());
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
        messageInput.sendButton.setVisible(true);
        messageInput.stopButton.setVisible(false);
        chatActive = true;
    }

    private void setSendButtonToStop() {
        messageInput.sendButton.setVisible(false);
        messageInput.stopButton.setVisible(true);
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

        private final TextArea input = new TextArea();
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
            input.setPlaceholder("Напишите ваше сообщение здесь...");
            input.setValueChangeMode(ValueChangeMode.EAGER); // Реагировать сразу на изменения
            input.addFocusListener(e -> input.setPlaceholder(""));
            input.addBlurListener(e -> input.setPlaceholder("Напишите ваше сообщение здесь..."));
        }

        private void addComponents() {
            add(input, sendButton, stopButton);
        }

        private void setLayoutDefaults() {
            setPadding(true);
            setSpacing(true);
            // Растягиваем TextField по ширине родителя
            input.setWidthFull();
            setVerticalComponentAlignment(END, input, sendButton, stopButton);
        }
    }
}
