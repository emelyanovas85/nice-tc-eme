package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.vaadin.flow.component.Unit.PERCENTAGE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;

@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> {

    private final AiService aiService;
    private final MessageList messageList = new MessageList();
    private final String chatId = UUID.randomUUID().toString();
    private boolean chatActive = true;
    private MessageInput messageInput;


    @PostConstruct
    private void initUI() {

        Scroller scroller = new Scroller(messageList);
        scroller.setHeight(70, PERCENTAGE);
        scroller.setWidth(70, PERCENTAGE);

        getContent().addAndExpand(scroller);
        getContent().setAlignItems(CENTER);

        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onSubmit);
        messageInput.setWidth(70, PERCENTAGE);
        messageInput.setI18n(
                new MessageInputI18n()
                .setSend("Отправить")
                .setMessage("Введите текст сообщения...")
        );

        getContent().add(messageInput);
        getContent().setSizeFull();
    }

    private void onSubmit(MessageInput.SubmitEvent submitEvent) {
        if (!chatActive) {
            // Если кнопка в состоянии "Стоп" и нажата, останавливаем чат и возвращаем "Отправить"
            stopChat();
            setSendButtonToSend();
            return;
        }

        String userText = submitEvent.getValue().trim();
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

        Optional<UI> uiOptional = submitEvent.getSource().getUI();

        uiOptional.ifPresent(ui -> {
            Flux<String> responseFlux = aiService.sendMessageStream(userText);
            responseFlux.subscribe(
                    token -> ui.access(() -> botMessage.appendText(token)),
                    err -> ui.access(() -> {
                        botMessage.setText("Ошибка: " + err.getMessage());
                        setSendButtonToSend();
                    }),
                    () -> ui.access(() -> setSendButtonToSend()));
        });
    }

    private void setSendButtonToSend() {
        messageInput.setI18n(new MessageInputI18n().setSend("Отправить").setMessage("Введите текст сообщения..."));
        chatActive = true;
    }

    private void setSendButtonToStop() {
        messageInput.setI18n(new MessageInputI18n().setSend("Стоп").setMessage("Сообщение отправляется..."));
        chatActive = false;
    }

    private void stopChat() {
        // Логика остановки работы aiService, пока просто переключаем флаг
        chatActive = false;
    }
}
