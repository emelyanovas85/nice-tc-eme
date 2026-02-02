package at.nice.tc.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@CssImport(value = "./components/test-tree-styles.css")
public class TimestampLabel extends Span {

    public TimestampLabel() {
        this(Instant.now());
    }

    public TimestampLabel(Instant instant) {
        String formattedTime = formatTime(instant);
        setText(formattedTime);
        addClassNames("timestamp-label");
        getElement().setAttribute("title", instant.toString());
    }

    public static TimestampLabel create() {
        return new TimestampLabel();
    }

    private String formatTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneId.of("Europe/Moscow"));
        return formatter.format(instant);
    }
}
