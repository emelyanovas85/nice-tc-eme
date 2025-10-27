package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.messages.MessageInput;
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
import java.util.UUID;

@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> {

    private final AiService aiService;
    private final MessageList messageList = new MessageList();
    private final String chatId = UUID.randomUUID().toString();


    @PostConstruct
    private void initUI() {
        var scroller = new Scroller(messageList);
        scroller.setHeightFull();
        getContent().addAndExpand(scroller);

        var messageInput = new MessageInput();
        messageInput.setWidthFull();

        messageInput.addSubmitListener(this::onSubmit);

        getContent().add(messageInput);

        getContent().setSizeFull();
    }

    private void onSubmit(MessageInput.SubmitEvent submitEvent) {
        var userText = submitEvent.getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        var userMessage = new MessageListItem(userText, Instant.now(), "User");
        userMessage.setUserColorIndex(0);
        messageList.addItem(userMessage);

        var botMessage = new MessageListItem("", Instant.now(), "Bot");
        botMessage.setUserColorIndex(1);
        messageList.addItem(botMessage);

        var uiOptional = submitEvent.getSource().getUI();

        uiOptional.ifPresent(ui -> {
            Flux<String> responseFlux = aiService.sendMessageStream(userText);
            responseFlux.subscribe(token -> ui.access(() -> botMessage.appendText(token)),
                    err -> ui.access(() -> botMessage.setText("Ошибка: " + err.getMessage())));
        });
    }
}
