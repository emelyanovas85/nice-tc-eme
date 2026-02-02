package at.nice.tc.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class UiUtils {

    public static void printAndShowNotification(String notification) {
        log.warn(notification);
        Notification.show(notification);
    }

    public static void doInUI(Component container, Runnable r) {
        container.getUI().ifPresentOrElse(
                ui -> {
                    if (ui.isAttached()) {
                        ui.access(r::run);
                    } else {
                        r.run();
                    }
                },
                r
        );
    }
}
