package at.nice.tc.ui.components;


import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import static com.vaadin.flow.component.Key.ENTER;
import static com.vaadin.flow.component.Key.KEY_S;
import static com.vaadin.flow.component.KeyModifier.CONTROL;
import static com.vaadin.flow.component.KeyModifier.SHIFT;
import static com.vaadin.flow.component.button.ButtonVariant.*;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;

/**
 * Горизонтальный компонент с полем для ввода текста и кнопками "Отправить"/"Стоп"/"Сохранить ответ"
 */
@Getter
public class ChatInputComponent extends HorizontalLayout {

    private final TextField textField = new TextField();
    private final Button sendButton = new Button("Отправить");
    private final Button stopButton = new Button("Стоп");
    private final Button saveButton = new Button("Сохранить ответ");

    public ChatInputComponent() {
        configureInput();
        configureButtons();
        addComponents();
        setLayoutDefaults();
    }

    private void configureButtons() {
        sendButton.addThemeVariants(LUMO_SUCCESS, LUMO_PRIMARY);
        sendButton.addClassName("send-button");
        sendButton.setWidth("8em");
        sendButton.setTooltipText("Shift + Enter");

        stopButton.addThemeVariants(LUMO_ERROR, LUMO_PRIMARY);
        stopButton.setWidth("8em");
        stopButton.addClassName("stop-button");
        stopButton.setVisible(false);
        stopButton.addClickShortcut(KEY_S, CONTROL);

        saveButton.addThemeVariants(LUMO_PRIMARY);
        saveButton.addClassName("save-button");
        saveButton.setVisible(false);
        saveButton.addClickShortcut(KEY_S, CONTROL, SHIFT);
    }

    private void configureInput() {
        textField.setPlaceholder("Напишите ваше сообщение здесь...");
        textField.addKeyPressListener(ENTER, event -> {
            if (sendButton.isVisible() && sendButton.isEnabled()) {
                sendButton.click();
            }
        }, CONTROL);
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addFocusListener(e -> textField.setPlaceholder(""));
        textField.addBlurListener(e -> textField.setPlaceholder("Напишите ваше сообщение здесь..."));
    }

    private void addComponents() {
        add(textField, sendButton, stopButton, saveButton);
    }

    private void setLayoutDefaults() {
        setPadding(true);
        setSpacing(true);
        textField.setWidthFull();
        setVerticalComponentAlignment(CENTER, textField, sendButton, stopButton, saveButton);
    }

    public void showSendButton() {
        sendButton.setVisible(true);
        stopButton.setVisible(false);
    }

    public void showStopButton() {
        sendButton.setVisible(false);
        stopButton.setVisible(true);
        saveButton.setVisible(false);
    }

    public void showSaveButton() {
        saveButton.setVisible(true);
    }

    public void hideSaveButton() {
        saveButton.setVisible(false);
    }
}
