package at.nice.tc.ui.components;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END;

/**
 * Поле для ввода текста и кнопки "Отправить" и "Стоп"
 */
@Getter
public class ChatInputComponent extends HorizontalLayout {

    private final TextArea area = new TextArea();
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
        area.setPlaceholder("Напишите ваше сообщение здесь...");
        area.setValueChangeMode(ValueChangeMode.EAGER); // Реагировать сразу на изменения
        area.addFocusListener(e -> area.setPlaceholder(""));
        area.addBlurListener(e -> area.setPlaceholder("Напишите ваше сообщение здесь..."));
    }

    private void addComponents() {
        add(area, sendButton, stopButton);
    }

    private void setLayoutDefaults() {
        setPadding(true);
        setSpacing(true);
        // Растягиваем TextField по ширине родителя
        area.setWidthFull();
        setVerticalComponentAlignment(END, area, sendButton, stopButton);
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
