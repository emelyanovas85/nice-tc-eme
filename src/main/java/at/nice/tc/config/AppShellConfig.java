package at.nice.tc.config;


import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push(PushMode.AUTOMATIC)
@Theme(value = "my-theme", variant = Lumo.DARK)
public class AppShellConfig implements AppShellConfigurator {
    // Можно оставить пустым, это маркер для Vaadin
}
