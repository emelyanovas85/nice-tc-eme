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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
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

    private void restoreUI() {
        final String chatId = config.getChatId();
        final Restorer restorer = new Restorer();

        final List<Message> completedMessages = new ArrayList<>(memoryService.getCompletedMessages(chatId));
        Collections.reverse(completedMessages);

        final LocalDateTime now = LocalDateTime.now();
        final int messageCount = completedMessages.size();
        boolean hasAssistantMessage = false;

        for (int i = 0; i < completedMessages.size(); i++) {
            Message m = completedMessages.get(i);
            LocalDateTime messageTime = now.minusSeconds((long) (messageCount - i) * 2);
            switch (m.getMessageType()) {
                case ASSISTANT -> {
                    restorer.createCompletedAssistantMessage(m.getText(), messageTime);
                    hasAssistantMessage = true;
                }
                case USER -> restorer.createUserMessage(m.getText(), messageTime);
                default -> log.warn("Не обработано сообщение {}:\n{}", m.getMessageType(), m.getText());
            }
        }

        if (hasAssistantMessage) {
            inputLayout.showSaveButton();
        }
    }

    class Restorer {

        public void createCompletedAssistantMessage(String text, LocalDateTime timestamp) {
            MarkdownMessageWithThinking botMessage = new MarkdownMessageWithThinking("Агент Jira", timestamp, aiToolCallService);
            botMessage.getMainMessage().setUserColorIndex(5);
            messageList.addComponentAtIndex(0, botMessage);
            botMessage.setMarkdown(text);
            botMessage.finish();
        }

        public void createUserMessage(String text, LocalDateTime timestamp) {
            MarkdownMessageWithThinking userMessage = new MarkdownMessageWithThinking(config.getUserFio(), timestamp, aiToolCallService);
            userMessage.setMessageType(MarkdownMessageWithThinking.MessageType.USER);
            messageList.addComponentAtIndex(0, userMessage);
            userMessage.setMarkdown(text);
            userMessage.finish();
        }
    }

    private Disposable subscription;
    private MarkdownMessageWithThinking actualBotMessage;

    private void onSubmit(ClickEvent<Button> buttonClickEvent) {
        String userText = inputLayout.getComboBox().getValue().trim();
        if (userText.isEmpty()) return;

        scroll.setStickDown(true);
        inputLayout.showStopButton();
        inputLayout.getComboBox().clear();

        LocalDateTime now = LocalDateTime.now();

        MarkdownMessageWithThinking userMessage = new MarkdownMessageWithThinking(config.getUserFio(), now, aiToolCallService);
        userMessage.setMessageType(MarkdownMessageWithThinking.MessageType.USER);
        messageList.add(userMessage);
        userMessage.setMarkdown(userText);
        userMessage.finish();

        addedMessageTimestamps.add(System.currentTimeMillis());

        // actualBotMessage создаётся здесь, но в messageList не добавляется.
        // Добавление происходит в subscribeToChatStream при первом токене, чтобы гарантировать
        // правильный порядок: сначала блок добавляется в DOM, затем в него добавляются компоненты.
        actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
        actualBotMessage.getMainMessage().setUserColorIndex(5);
        actualBotMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);

        StringBuilder prompt = new StringBuilder();
        if (!config.getUserId().isBlank())
            prompt.append("Меня зовут ").append(config.getUserFio()).append(". Обращайся по имени.\n");
        if (!config.getScope().isBlank())
            prompt.append("Я нахожусь на странице ").append(config.getScope()).append(" (определи - ключ теста, прогона или id версии теста).\n");
        prompt.append("\n").append(userText);
        prompt.append("\n").append(Prompts.aggregatorPrompt);

        // Подписку пересоздаём явно перед отправкой — старая подписка (если была) dispose'ится здесь.
        // onAttach больше не пересоздаёт подписку, чтобы не сбрасывать state в середине стриминга.
        subscribeToChat();
        aiService.sendMainMessageStream(prompt.toString(), config.getChatId());
    }

    private void onStop(ClickEvent<Button> buttonClickEvent) {
        stop();
    }

    private void onSave(ClickEvent<Button> buttonClickEvent) {
        String scope = config.getScope();
        if (scope != null && !scope.isBlank()) {
            doSave(scope);
        } else {
            showScopeInputDialog();
        }
    }

    private void showScopeInputDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Сохранить ответ");
        dialog.setCloseOnOutsideClick(true);
        dialog.setCloseOnEsc(true);

        TextField scopeField = new TextField("Ключ тест-кейса");
        scopeField.setPlaceholder("Например: VPEPVV-T2834");
        scopeField.setWidth("280px");
        scopeField.focus();

        Button saveBtn = new Button("Сохранить", e -> {
            String entered = scopeField.getValue().trim();
            if (entered.isBlank()) {
                scopeField.setInvalid(true);
                scopeField.setErrorMessage("Введите ключ тест-кейса");
                return;
            }
            dialog.close();
            doSave(entered);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickShortcut(Key.KEY_S, KeyModifier.CONTROL, KeyModifier.SHIFT);

        Button cancelBtn = new Button("Отмена", e -> dialog.close());
        scopeField.addKeyPressListener(Key.ENTER, e -> saveBtn.click());

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(scopeField, buttons);
        content.setPadding(false);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }

    private void doSave(String testCaseId) {
        List<Message> messages = memoryService.getCompletedMessages(config.getChatId());
        String lastAssistantText = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getMessageType() == MessageType.ASSISTANT) {
                lastAssistantText = messages.get(i).getText();
                break;
            }
        }

        if (lastAssistantText == null || lastAssistantText.isBlank()) {
            showNotification("Нет ответа ассистента для сохранения", NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            Path saved = markdownSaveService.save(testCaseId, lastAssistantText);
            showNotification("✅ Сохранено: " + saved.toAbsolutePath(), NotificationVariant.LUMO_SUCCESS);
            log.info("Ответ для '{}' сохранён в файл: {}", testCaseId, saved);
        } catch (Exception e) {
            log.error("Ошибка сохранения ответа для testCaseId={}", testCaseId, e);
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
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.BOTTOM_END);
        notification.setDuration(0);
        notification.addThemeVariants(variant);

        Span text = new Span(message);

        Button closeButton = new Button(new Icon(VaadinIcon.CLOSE), event -> notification.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        closeButton.getElement().setAttribute("aria-label", "Закрыть уведомление");

        HorizontalLayout layout = new HorizontalLayout(text, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        notification.add(layout);
        notification.open();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Подписываемся только если нет активной подписки.
        // Это предотвращает пересоздание подписки в середине стриминга второго (и любого следующего)
        // ответа LLM, что приводило к сбросу state и записи текста ответа в блок "Размышления модели".
        if (subscription == null || subscription.isDisposed()) {
            subscribeToChat();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        super.onDetach(detachEvent);
    }

    /**
     * Подписывается на поток токенов для текущего chatId.
     * Dispose старой подписки выполняется внутри.
     * Вызывать: из onSubmit (всегда) и из onAttach (только если нет активной подписки).
     */
    private void subscribeToChat() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (getUI().isEmpty()) return;

        final UI ui = getUI().get();
        subscription = memoryService.subscribe(config.getChatId())
                .subscribe(
                        token -> ui.access(() -> {
                            if (actualBotMessage == null) {
                                // Отсутствие actualBotMessage здесь означает reconnect без активного запроса
                                actualBotMessage = new MarkdownMessageWithThinking("Агент Jira", LocalDateTime.now(), aiToolCallService);
                                actualBotMessage.getMainMessage().setUserColorIndex(5);
                                actualBotMessage.setMessageType(MarkdownMessageWithThinking.MessageType.ASSISTANT);
                            }
                            // Добавляем в messageList только при первом токене — компонент ещё не в DOM.
                            // Это гарантирует: сначала блок занимает место в списке, затем в него
                            // добавляются дерево тестов и текст анализа.
                            if (!actualBotMessage.isAttached()) {
                                messageList.add(actualBotMessage);
                            }
                            // Используем appendMarkdownInUiThread вместо appendMarkdownAsync:
                            // мы уже в ui.access, поэтому не нужно делать внутренний getUI().ifPresent(ui2 -> ui2.access(...))
                            // который проваливался бы в пустой поскольку компонент ещё не в DOM (getUI() == empty).
                            actualBotMessage.appendMarkdownInUiThread(token);
                            scroll.scrollToBottom();
                        }),
                        err -> ui.access(() -> {
                            if (actualBotMessage != null) {
                                actualBotMessage.appendMarkdownInUiThread("\n\nОшибка: " + err.getMessage());
                            }
                            stop();
                        }),
                        () -> ui.access(() -> {
                            if (actualBotMessage != null) {
                                actualBotMessage.finish();
                            }
                            stop();
                            inputLayout.showSaveButton();
                        })
                );
    }

    // Старый метод оставлен для совместимости — делегирует в subscribeToChat()
    private void subscribeToChatStream() {
        subscribeToChat();
    }
}
