package at.nice.tc.ui;

import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.beans.factory.annotation.Autowired;

@Route("markdown")
public class MarkdownView extends VerticalLayout {

    @Autowired
    private JiraService jiraService;
    private boolean isDarkTheme = false;
    private TextArea resultArea;
    private Markdown markdown;
    private ProgressBar progressBar;

    public MarkdownView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        //TODO нужно добавить в css файле (frontend/components/test-tree-styles.css) цвета для светлой темы и тогда вернуть тогл
/*        Button themeToggle = new Button(VaadinIcon.ADJUST.create());
        themeToggle.addThemeVariants(ButtonVariant.LUMO_ICON);
        themeToggle.addClickListener(e -> toggleTheme());
        themeToggle.getElement().setAttribute("aria-label", "Toggle theme");*/

        TextField inputField = new TextField();
        inputField.setPlaceholder("Введите ключ теста, типа VPEPVV-T777 или ID версии теста, типа 123456");
        inputField.setWidthFull();

        Button processButton = new Button("Markdown it");
        processButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        inputField.addKeyDownListener(Key.ENTER, event -> {
            processButton.click();
        });

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setWidthFull();

        //TODO нужно добавить в css файле (frontend/components/test-tree-styles.css) цвета для светлой темы и тогда вернуть тогл
        HorizontalLayout inputLayout = new HorizontalLayout(/*themeToggle, */inputField, processButton);
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, inputField);

        resultArea = new TextArea();
        resultArea.setLabel("Markdown Source");
        resultArea.setWidthFull();
        resultArea.getStyle().set("background-color", "transparent");
//        resultArea.setReadOnly(true);

        markdown = new Markdown();
        markdown.setWidthFull();
        markdown.getStyle().set("padding", "var(--lumo-space-m)");
        markdown.getStyle().set("overflow-y", "auto");

        resultArea.addValueChangeListener(e -> {
            markdown.setContent(e.getValue());
        });

        SplitLayout splitLayout = new SplitLayout(resultArea, markdown);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(80);

        processButton.addClickListener(e -> {
            processButton.setEnabled(false);
            progressBar.setVisible(true);
            String input = inputField.getValue();

            getUI().ifPresent(ui -> ui.access(() -> {
                try {
                    String result = processInput(input);
                    resultArea.setValue(result);
                } finally {
                    progressBar.setVisible(false);
                    processButton.setEnabled(true);
                }
            }));
        });

        add(inputLayout, progressBar, splitLayout);
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        getElement().executeJs(
                "document.documentElement.setAttribute('theme', $0)",
                isDarkTheme ? Lumo.DARK : Lumo.LIGHT
        );
    }

    private String processInput(String input) {
        return jiraService.getTestWithNestedMarkdown(input)
                .handle((md, throwable) -> throwable != null ? ThrowableUtils.asString(throwable) : md)
                .join();
    }
}
