package at.nice.tc.ui;

import at.nice.tc.ai.client.Prompts;
import at.nice.tc.service.AiService;
import at.nice.tc.service.AiToolCallService;
import at.nice.tc.service.MarkdownSaveService;
import at.nice.tc.service.MemoryService;
import at.nice.tc.ui.components.ChatInputComponent;
import at.nice.tc.ui.components.MarkdownMessageWithThinking;
import at.nice.tc.ui.components.SmartScroller;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import reactor.core.Disposable;

import java.nio.file.Path;
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
    private final MarkdownSaveService markdownSaveService;

    private SmartScroller scroll;
    private VerticalLayout messageList;
    private ChatInputComponent inputLayout;

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
        inputLayout.getSaveButton().addClickListener(this::onSave);
        inputLayout.setWidth(config.getWidthPerc(), PERCENTAGE);

        getContent().add(inputLayout);
        getContent().setSizeFull();
        inputLayout.showSendButton();

        restoreUI();
    }


    /**
     * Добавляет в чат сообщения из истории, в том числе сообщения, которые ИИ генерит прямо сейчас
     */
    private void restoreUI() {
        final String chatId = config.getChatId();
        final Restorer restorer = new Restorer();

        final List<Message> completedMessages = memoryService.getCompletedMessages(chatId);
        Collections.reverse(completedMessages);

        final LocalDateTime now = LocalDateTime.now();
        final int messageCount = completedMessages.size();

        for (int i = 0; i < completedMessages.size(); i++) {
            Message m = completedMessages.get(i);
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
            botMessage.getMainMessage().setUserColorIndex(5);
            messageList.addComponentAtIndex(0, botMessage);
        }

        public void createUserMessage(String text, LocalDateTime timestamp) {
            MarkdownMessage userMessage = new MarkdownMessage(text, config.getUserFio(), timestamp);
            userMessage.setUserColorIndex(3);
            messageList.addComponentAtIndex(0, userMessage);
        }
    }


    private Disposable subscription;
    private MarkdownMessageWithThinking actualBotMessage;


    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getTextField().getValue().trim();
        if (userText.isEmpty()) {
            return;
        }

        scroll.setStickDown(true);
        inputLayout.showStopButton();
        inputLayout.getTextField().clear();

        LocalDateTime now = LocalDateTime.now();
        MarkdownMessageWithThinking userMessage = new MarkdownMessageWithThinking(config.getUserFio(), now, aiToolCallService);
        userMessage.setMarkdown(userText);
        userMessage.setMessageType(MarkdownMessageWithThinking.MessageType.USER);
        messageList.add(userMessage);

        long timestamp = System.currentTimeMillis();
        addedMessageTimestamps.add(timestamp);

        actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
        actualBotMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);
        messageList.add(actualBotMessage);

        StringBuilder prompt = new StringBuilder();
        if (!config.getUserId().isBlank())
            prompt.append("Меня зовут ").append(config.getUserFio()).append(". Обращайся по имени.\n");
        if (!config.getScope().isBlank())
            prompt.append("Я нахожусь на странице ").append(config.getScope()).append(" (определи - ключ теста, прогона или id версии теста).\n");
        prompt.append("\n").append(userText);
        prompt.append("\n").append(Prompts.aggregatorPrompt);

        subscribeToChatStream();
        aiService.sendMainMessageStream(prompt.toString(), config.getChatId());
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stop();
    }

    /**
     * Обработчик кнопки "Сохранить ответ".
     * Берёт последнее сообщение ассистента из истории и сохраняет в файл {scope}.md
     */
    private void onSave(ClickEvent<Button> buttonClickEvent) {
        String scope = config.getScope();
        if (scope == null || scope.isBlank()) {
            showNotification("Сохранение невозможно: scope (ключ тест-кейса) не задан. Передайте ?scope=КЛЮЧ-ТXXX в URL",
                    NotificationVariant.LUMO_ERROR);
            return;
        }

        // Гетем последнее сообщение ассистента из ChatMemory
        List<Message> messages = memoryService.getCompletedMessages(config.getChatId());
        String lastAssistantText = messages.stream()
                .filter(m -> m.getMessageType() == MessageType.ASSISTANT)
                .findFirst() // список в обратном порядке после reverse() в restoreUI — первый = последний
                .map(Message::getText)
                .orElse(null);

        if (lastAssistantText == null || lastAssistantText.isBlank()) {
            showNotification("Нет ответа ассистента для сохранения", NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            Path saved = markdownSaveService.save(scope, lastAssistantText);
            showNotification("✅ Сохранено: " + saved.toAbsolutePath(), NotificationVariant.LUMO_SUCCESS);
            log.info("Ответ для '{}' сохранён в файл: {}", scope, saved);
        } catch (Exception e) {
            log.error("Ошибка сохранения ответа для scope={}", scope, e);
            showNotification("❌ Ошибка сохранения: " + e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            subscription = null;
        }
        actualBotMessage = null;
        inputLayout.showSendButton();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscribeToChatStream();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        super.onDetach(detachEvent);
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
                                actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
                                actualBotMessage.getMainMessage().setUserColorIndex(5);
                                messageList.add(actualBotMessage);
                            }
                            actualBotMessage.appendMarkdownAsync(token);
                            scroll.scrollToBottom();
                        }),
                        err -> ui.access(() -> {
                            if (actualBotMessage != null) {
                                actualBotMessage.appendMarkdownAsync("\n\nОшибка: " + err.getMessage());
                            }
                            stop();
                        }),
                        () -> ui.access(() -> {
                            if (actualBotMessage != null) {
                                actualBotMessage.finish();
                            }
                            stop();
                            // Показываем кнопку "Сохранить ответ" только если scope задан (тест-кейс открыт)
                            if (!config.getScope().isBlank()) {
                                inputLayout.showSaveButton();
                            }
                        })
                );
    }

}
