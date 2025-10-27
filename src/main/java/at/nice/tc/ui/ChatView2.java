/*
package at.nice.tc.ui;

import at.nice.tc.service.AiService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Route("")
//@Push(PushMode.AUTOMATIC)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ChatView2 extends VerticalLayout {

    private final AiService aiService; // final гарантирует инициализацию через конструктор

    private final Div chatArea = new Div();
    private final TextArea input = new TextArea();
    private final Button sendButton = new Button("Отправить");

    private Disposable responseSubscription;
    private Registration detachListener;

//    // Lombok генерирует конструктор для final полей и @Autowired указывает Spring создать его
//    @RequiredArgsConstructor(onConstructor_ = @Autowired)
//    public ChatView(AiService aiService) { // Конструктор будет сгенерирован Lombok
//        this.aiService = aiService; // Присваивание вручную не нужно, Lombok это делает
//    }

    // Если Lombok генерирует конструктор, то инициализацию UI лучше вынести в отдельный метод,
    // чтобы избежать ссылки на поля до их полной инициализации (хотя в данном случае, final поля инициализируются первыми)
    @PostConstruct // Добавьте javax.annotation.PostConstruct
    public void initUI() {
        this.getStyle().set("background-color", "#000"); // Черный фон для родительского layout
        this.setSizeFull(); // Делаем layout на всю доступную высоту (опционально)

        chatArea.getStyle().set("background-color", "#000"); // Черный
        chatArea.getStyle().set("color", "orange");
        chatArea.getStyle().set("padding", "10px");
        chatArea.getStyle().set("height", "400px");
        chatArea.getStyle().set("overflow", "auto");
        chatArea.getStyle().set("border", "1px solid orange");
        chatArea.setWidthFull();


        input.setPlaceholder("Введите сообщение...");
        input.setWidthFull();
        input.getStyle().set("color", "orange");
        input.getStyle().set("border", "1px solid orange");
        input.setHeight("100px");

        sendButton.getStyle().set("margin-top", "10px");
        sendButton.getStyle().set("border", "1px solid orange");


        add(chatArea, input, sendButton);

        sendButton.addClickListener(e -> sendMessage());

        detachListener = getUI().map(ui -> ui.addDetachListener(event -> {
            if (responseSubscription != null && !responseSubscription.isDisposed()) {
                responseSubscription.dispose();
            }
        })).orElse(null);
    }


    private void sendMessage() {
        String message = input.getValue().trim();
        if (message.isEmpty()) {
            Notification.show("Введите сообщение");
            return;
        }

        chatArea.getUI().ifPresent(ui -> ui.access(() -> {
            chatArea.add(new Div(new com.vaadin.flow.component.html.Span("Вы: " + message)));
            chatArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
        }));

        if (responseSubscription != null && !responseSubscription.isDisposed()) {
            responseSubscription.dispose();
        }

        Flux<String> responseFlux = aiService.sendMessageStream(message);

        // Создаем новый div для ответа ИИ при каждом новом запросе
        Div aiResponseDiv = new Div();
        aiResponseDiv.setId("ai-response-" + System.currentTimeMillis()); // Уникальный ID для нового ответа
        chatArea.getUI().ifPresent(ui -> ui.access(() -> chatArea.add(aiResponseDiv)));

        responseSubscription = responseFlux.subscribe(content -> {
            chatArea.getUI().ifPresent(ui ->
                    ui.access(() -> {
                        // Добавляем контент к уже созданному div
                        aiResponseDiv.add(new com.vaadin.flow.component.html.Span(content));

                        chatArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
                    })
            );
        }, error -> chatArea.getUI().ifPresent(ui -> ui.access(() ->
                Notification.show("Ошибка получения ответа: " + error.getMessage()))));


        input.clear();
    }


    @PreDestroy
    public void cleanup() {
        if (responseSubscription != null && !responseSubscription.isDisposed()) {
            responseSubscription.dispose();
        }
        if (detachListener != null) {
            detachListener.remove();
        }
    }
}*/
