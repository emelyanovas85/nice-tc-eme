package at.nice.tc.ui.components;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END;

/**
 * Горизонтальный компонент с полем для ввода текста и кнопкой "Отправить"/"Стоп"
 */
@Getter
public class ChatInputComponent extends HorizontalLayout {

    private final TextField textField = new TextField();
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

        sendButton.addClassName("send-button");
        stopButton.addClassName("stop-button");
    }

    private void configureInput() {
        textField.setPlaceholder("Напишите ваше сообщение здесь...");
        textField.setValueChangeMode(ValueChangeMode.EAGER); // Реагировать сразу на изменения
        textField.addFocusListener(e -> textField.setPlaceholder(""));
        textField.addBlurListener(e -> textField.setPlaceholder("Напишите ваше сообщение здесь..."));
//        area.addClassName("lumo-textarea");
    }

    private void addComponents() {
        add(textField, sendButton, stopButton);
    }

    private void setLayoutDefaults() {
        setPadding(true);
        setSpacing(true);
        // Растягиваем TextField по ширине родителя
        textField.setWidthFull();
        setVerticalComponentAlignment(CENTER, textField, sendButton, stopButton);
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
