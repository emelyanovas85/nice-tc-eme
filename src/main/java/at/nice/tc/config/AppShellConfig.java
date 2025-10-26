package at.nice.tc.config;


import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;

@Push(PushMode.AUTOMATIC)
public class AppShellConfig implements AppShellConfigurator {
    // Можно оставить пустым, это маркер для Vaadin
}
