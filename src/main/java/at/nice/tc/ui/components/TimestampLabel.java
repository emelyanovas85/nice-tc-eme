package at.nice.tc.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@CssImport("./styles/test-tree-styles.css")
public class TimestampLabel extends Span {

    public TimestampLabel() {
        this(Instant.now());
    }

    public TimestampLabel(Instant instant) {
        String formattedTime = formatTime(instant);
        setText(formattedTime);
        addClassNames("timestamp-label");
        getElement().setAttribute("title", instant.toString());

//        scheduleRelativeUpdate(instant);
    }

    public static TimestampLabel create() {
        return new TimestampLabel();
    }

    private String formatTime(Instant instant) {
        // Локальное время MSK (ваш часовой пояс)
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.of("Europe/Moscow"));
        return formatter.format(instant);
    }

//    private void scheduleRelativeUpdate(Instant startTime) {
//        // Каждые 30 сек обновляем "2 мин назад"
//        UI.getCurrent().getPage().addJavaScript(
//                "setInterval(() => { " +
//                        "  this.shadowRoot.querySelector('.timestamp-label').textContent = " +
//                        "  new Date().toLocaleTimeString('ru-RU', {hour: '2-digit', minute: '2-digit', second: '2-digit'}); " +
//                        "}, 30000);"
//        );
//    }
}
