package at.nice.tc.ui;

import at.nice.tc.events.*;
import at.nice.tc.service.AiService;
import at.nice.tc.service.EventService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.ui.components.ChatInputComponent;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.ui.components.SmartScroller;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.Disposable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.Unit.PERCENTAGE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;

@Slf4j
@Route("")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final AiService aiService;
    private final EventService eventService;
    private final MemoryService memoryService;
//    private final JiraService jiraService;

    private SmartScroller scroll; // обертка для панели сообщений
    private VerticalLayout messageList; // панель сообщений
    private ChatInputComponent inputLayout; // textArea с кнопками


    private final Config config = new Config(UUID.randomUUID().toString(), "browser", 70, 70, "", "", "Пользователь");

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
        private String chatId;
        private String mode;
        private int heightPerc;
        private int widthPerc;
        private String scope;
        private String userId;
        private String userFio;
    }

    public static class Constants {
        public static final String CHAT_ID = "chatId";
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters query = event.getLocation().getQueryParameters();
        query.getSingleParameter(Constants.CHAT_ID).ifPresentOrElse(config::setChatId, () -> {
            QueryParameters updated = query.merging(Constants.CHAT_ID, config.getChatId());
            UI.getCurrent().navigate(ChatView.class, updated);
        });
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


    /**
     * Добавляет в чат сообщения из истории, в том числе сообщения, которые ИИ генерит прямо сейчас
     */
    private void restoreUI() {
        final String chatId = config.getChatId();
        final Restorer restorer = new Restorer();
        // Добавление сообщений из истории снизу вверх
        final List<Message> completedMessages = memoryService.getCompletedMessages(chatId);
        Collections.reverse(completedMessages); // отрисовывать снизу вверх
        completedMessages.forEach(m -> {
            switch (m.getMessageType()) {
                case ASSISTANT -> restorer.createCompletedAssistantMessage(m.getText());
                case USER -> restorer.createUserMessage(m.getText());
                default -> log.warn("Не обработано сообщение {}:\n{}", m.getMessageType(), m.getText());
            }
        });
    }


    class Restorer {

        public void createCompletedAssistantMessage(String text) {
            MarkdownMessageWithThinking botMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now()); // TODO: указать правильное время
            botMessage.appendMarkdown(text);
            botMessage.getMainMessage().setUserColorIndex(5);
            messageList.addComponentAtIndex(0, botMessage);
        }


        public void createUserMessage(String text) {
            MarkdownMessage userMessage = new MarkdownMessage(text, config.getUserFio(), LocalDateTime.now()); // TODO: указать правильное время
            userMessage.setUserColorIndex(3);
            messageList.addComponentAtIndex(0, userMessage);
        }
    }


    private Disposable subscription;
    private MarkdownMessageWithThinking actualBotMessage;


    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getArea().getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        scroll.setStickDown(true);
        inputLayout.showStopButton();
        inputLayout.getArea().clear();

        MarkdownMessage userMessage = new MarkdownMessage(userText, config.getUserFio(), LocalDateTime.now());
        userMessage.setUserColorIndex(3);
        messageList.add(userMessage);

        actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now());
        actualBotMessage.getMainMessage().setUserColorIndex(5);
        messageList.add(actualBotMessage);

        StringBuilder prompt = new StringBuilder();
        if (!config.getUserId().isBlank())
            prompt.append("Меня зовут ").append(config.getUserFio()).append(". Обращайся по имени.\n");
        if (!config.getScope().isBlank())
            prompt.append("Я нахожусь на странице ").append(config.getScope()).append(" (определи - ключ теста, прогона или id версии теста).\n");
        prompt.append("\n").append(userText);
        aiService.sendMainMessageStream(prompt.toString(), config.getChatId()); // Токены будут push-иться в MemoryService
        // Подписка на ответ ассистента
        subscribeToChatStream();
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stop();
    }

    private void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subscription = null;
        }
        inputLayout.showSendButton();
    }



    private EventService.Registration eventServiceRegistration;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // restore all previous/active messages on UI init
        restoreUI();
        // Подписка на ответ ассистента
        subscribeToChatStream();
        // Подписка на события для текущего chatId
        eventServiceRegistration = eventService.subscribe(config.getChatId(), this::handleBroadcastEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subscription = null;
        }
        if (eventServiceRegistration != null) {
            eventServiceRegistration.unsubscribe();
            eventServiceRegistration = null;
        }
    }


    private void subscribeToChatStream() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (getUI().isEmpty()) {
            return;
        }
        final UI ui = getUI().get();
        subscription = memoryService.subscribe(config.getChatId())
                .subscribe(token -> ui.access(() -> {
                            if (actualBotMessage == null) {
                                actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now());
                                actualBotMessage.getMainMessage().setUserColorIndex(5);
                                messageList.add(actualBotMessage);
                            }
                            actualBotMessage.appendMarkdownAsync(token);
                            scroll.scrollToBottom();
                        }),
                        err -> ui.access(() -> {
                            actualBotMessage.appendMarkdownAsync("\n\nОшибка: " + err.getMessage());
                            stop();
                        }),
                        () -> ui.access(() -> {
                            actualBotMessage.finish();
                            stop();
                        })
                );
    }

    private void handleBroadcastEvent(ChatEvent event) {
        if (event instanceof CheckEvent.AgentBuiltTestTreeEvent built) {
            Optional.ofNullable(actualBotMessage).ifPresent(message -> message.getHandlers().check.doOnBuiltTestTree(built));
            // TODO: тут можно добавить отрисовку в истории (pending = неактивые кнопки)

        } else if (event instanceof CheckEvent.CheckStartedEvent started) {
            Optional.ofNullable(actualBotMessage).ifPresent(message -> message.getHandlers().check.doOnCheckStarted(started));
            // TODO: тут можно добавить отрисовку в истории (inProgress = появление спиннера + делать кнопку активной)

        } else if (event instanceof CheckEvent.CheckFinishedEvent finished) {
            Optional.ofNullable(actualBotMessage).ifPresent(message -> message.getHandlers().check.doOnCheckFinished(finished));
            // TODO: тут можно добавить отрисовку в истории (finished = убрать спиннер)

        } else if (event instanceof LogEvent logEvent) {
            Optional.ofNullable(actualBotMessage).ifPresent(message -> message.getHandlers().log.doOnLog(logEvent));

        }
    }


}
