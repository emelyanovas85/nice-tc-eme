package at.nice.tc.ui.components;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
@CssImport(value = "./components/vaadin-combo-box-overlay.css", themeFor = "vaadin-combo-box-overlay")
@Getter
public class ChatInputComponent extends HorizontalLayout {

    private final ComboBox<String> comboBox = new ComboBox<>();
    private final Button sendButton = new Button("Отправить");
    private final Button stopButton = new Button("Стоп");
    private final Button saveButton = new Button("Сохранить ответ");

    public ChatInputComponent() {
        configureComboBox();
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

    private void configureComboBox() {
        comboBox.setPlaceholder("Напишите ваше сообщение здесь...");
        comboBox.setItems(
                "Проведи анализ тест-кейса ",
                "Да",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проведи анализ тест-кейса ",
                "Проверь доступна ли Jira"
        );
        comboBox.setPageSize(5);
        comboBox.setAllowCustomValue(true);
        comboBox.setAutoOpen(true);
        comboBox.addCustomValueSetListener(event -> comboBox.setValue(event.getDetail()));
        comboBox.addFocusShortcut(ENTER, CONTROL);
        comboBox.addFocusListener(e -> comboBox.setPlaceholder(""));
        comboBox.addBlurListener(e -> comboBox.setPlaceholder("Напишите ваше сообщение здесь..."));

        // Убираем инлайн min-width, который Vaadin выставляет программно и вызывает горизонтальный скроллбар
        comboBox.getElement().addPropertyChangeListener("opened", event -> {
            if (Boolean.TRUE.equals(event.getValue())) {
                comboBox.getElement().executeJs(
                    "var overlay = this.$.overlay;" +
                    "if (overlay) {" +
                    "  var scroller = overlay.shadowRoot ? overlay.shadowRoot.querySelector('vaadin-combo-box-scroller') : null;" +
                    "  if (!scroller) scroller = overlay.querySelector('vaadin-combo-box-scroller');" +
                    "  if (scroller) {" +
                    "    scroller.style.removeProperty('min-width');" +
                    "    scroller.style.setProperty('max-width', '100%', 'important');" +
                    "    scroller.style.setProperty('overflow-x', 'hidden', 'important');" +
                    "  }" +
                    "}"
                );
            }
        });
    }

    private void addComponents() {
        add(comboBox, sendButton, stopButton, saveButton);
    }

    private void setLayoutDefaults() {
        setPadding(true);
        setSpacing(true);
        comboBox.setWidthFull();
        setVerticalComponentAlignment(CENTER, comboBox, sendButton, stopButton, saveButton);
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
