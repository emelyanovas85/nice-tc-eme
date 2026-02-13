package at.nice.tc.ui;

import at.nice.tc.ai.client.Prompts;
import at.nice.tc.service.AiService;
import at.nice.tc.service.AiToolCallService;
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
    private final MemoryService memoryService;
    private final AiToolCallService aiToolCallService;

    private SmartScroller scroll; // обертка для панели сообщений
    private VerticalLayout messageList; // панель сообщений
    private ChatInputComponent inputLayout; // textArea с кнопками

    // Хранилище timestamp последних добавленных сообщений для предотвращения дублирования
    private final Set<Long> addedMessageTimestamps = new HashSet<>();

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

        // restore all previous/active messages on UI init
        restoreUI();
    }


    /**
     * Добавляет в чат сообщения из истории, в том числе сообщения, которые ИИ генерит прямо сейчас
     */
    private void restoreUI() {
        final String chatId = config.getChatId();
        final Restorer restorer = new Restorer();
        // Подписка на активный ответ ассистента
        subscribeToChatStream();
        // Добавление сообщений из истории снизу вверх
        final List<Message> completedMessages = memoryService.getCompletedMessages(chatId);
        Collections.reverse(completedMessages); // отрисовывать снизу вверх

        // Вычисляем время для каждого сообщения на основе его позиции
        // Предполагаем, что сообщения идут последовательно с интервалом ~2 секунды
        final LocalDateTime now = LocalDateTime.now();
        final int messageCount = completedMessages.size();

        for (int i = 0; i < completedMessages.size(); i++) {
            Message m = completedMessages.get(i);
            // Время вычисляется от текущего момента назад, предполагая интервал ~2 секунды между сообщениями
            // Самое старое сообщение будет иметь время (messageCount - i) * 2 секунд назад
            LocalDateTime messageTime = now.minusSeconds((long) (messageCount - i) * 2);

            switch (m.getMessageType()) {
                case ASSISTANT -> restorer.createCompletedAssistantMessage(m.getText(), messageTime);
                case USER -> restorer.createUserMessage(m.getText(), messageTime);
                default -> log.warn("Не обработано сообщение {}:\n{}", m.getMessageType(), m.getText());
            }
        }
    }


    class Restorer {

        public void createCompletedAssistantMessage(String text, LocalDateTime timestamp) {
            MarkdownMessageWithThinking botMessage = new MarkdownMessageWithThinking("Агент Jira", timestamp, aiToolCallService);
            botMessage.setMarkdown(text);
//            botMessage.getMainMessage().setUserColorIndex(5);
            botMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);
            messageList.addComponentAtIndex(0, botMessage);
        }


        public void createUserMessage(String text, LocalDateTime timestamp) {
//            MarkdownMessage userMessage = new MarkdownMessage(text, config.getUserFio(), timestamp);
//            userMessage.setUserColorIndex(3);
//            messageList.addComponentAtIndex(0, userMessage);
            MarkdownMessageWithThinking userMessage = new MarkdownMessageWithThinking(config.getUserFio(), timestamp, aiToolCallService);
            userMessage.setMarkdown(text);
            userMessage.setMessageType(MarkdownMessageWithThinking.MessageType.USER);
            messageList.addComponentAtIndex(0, userMessage);
        }
    }


    private Disposable subscription;
    private MarkdownMessageWithThinking actualBotMessage;


    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getArea().getValue().trim();
        if (userText.isEmpty()) return;

        scroll.setStickDown(true);
        inputLayout.showStopButton();
        inputLayout.getArea().clear();

        LocalDateTime now = LocalDateTime.now();
//        MarkdownMessage userMessage = new MarkdownMessage(userText, config.getUserFio(), now);
//        userMessage.setUserColorIndex(3);
//        messageList.add(userMessage);
        MarkdownMessageWithThinking userMessage = new MarkdownMessageWithThinking(config.getUserFio(), now, aiToolCallService);
        userMessage.setMarkdown(userText);
        userMessage.setMessageType(MarkdownMessageWithThinking.MessageType.USER);
        messageList.add(userMessage);

        // Публикуем событие о новом сообщении пользователя для синхронизации между вкладками
        long timestamp = System.currentTimeMillis();
        // Добавляем timestamp в Set, чтобы не добавить это сообщение снова при получении события
        addedMessageTimestamps.add(timestamp);

        actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
//        actualBotMessage.getMainMessage().setUserColorIndex(5);
        actualBotMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);
        messageList.add(actualBotMessage);

        StringBuilder prompt = new StringBuilder();
        if (!config.getUserId().isBlank())
            prompt.append("Меня зовут ").append(config.getUserFio()).append(". Обращайся по имени.\n");
        if (!config.getScope().isBlank())
            prompt.append("Я нахожусь на странице ").append(config.getScope()).append(" (определи - ключ теста, прогона или id версии теста).\n");
        prompt.append("\n").append(userText);
        prompt.append("\n\n").append(Prompts.aggregatorPrompt);
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


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscribeToChatStream();
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null && !subscription.isDisposed()) subscription.dispose();
        super.onDetach(detachEvent);
    }


/*
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
                                actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
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
*/

    private void subscribeToChatStream() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (getUI().isEmpty()) {
            return;
        }
        final UI ui = getUI().get();

//        actualBotMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);

        subscription = memoryService.subscribe(config.getChatId())
                .subscribe(
                        token -> ui.access(() -> {
                            MarkdownMessageWithThinking botMsg = findActiveBotMessage();
                            if (botMsg != null) {
                                botMsg.appendMarkdownAsync(token);
                                scroll.scrollToBottom();
                            } else {
                                log.warn("Нет активного bot сообщения для chatId: {}", config.getChatId());
                            }
                        }),
                        err -> ui.access(() -> {
                            MarkdownMessageWithThinking botMsg = findActiveBotMessage();
                            if (botMsg != null) {
                                botMsg.appendMarkdownAsync("\n\n**Ошибка:** " + err.getMessage());
                                stop();
                            } else {
                                log.error("Ошибка 429/другая, но нет bot сообщения: {}", err.getMessage());
                                stop();
                            }
                        }),
                        () -> ui.access(() -> {
                            MarkdownMessageWithThinking botMsg = findActiveBotMessage();
                            if (botMsg != null) {
                                botMsg.finish();
                                stop();
                            }
                        })
                );
    }

    private MarkdownMessageWithThinking findActiveBotMessage() {
        return (MarkdownMessageWithThinking) messageList.getChildren()
                .filter(c -> c instanceof MarkdownMessageWithThinking)
                .reduce((first, second) -> second)
                .orElse(null);
    }
}
