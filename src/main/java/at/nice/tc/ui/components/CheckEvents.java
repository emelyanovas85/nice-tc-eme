package at.nice.tc.ui.components;

import at.nice.tc.events.impl.CheckEvent;
import at.nice.tc.model.TestTree;
import at.nice.tc.utils.UiUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

@Slf4j
@CssImport(value = "./components/test-tree-styles.css")
public class CheckEvents {
    private TestTreeView currentTestTreeView;

    /**
     * Создаёт интерактивное дерево тестов
     */
    public Component doOnBuiltTestTree(CheckEvent.AgentBuiltTestTreeEvent event) {
        TestTree test = event.getTest();

        log.debug("doOnBuiltTestTree: создаём дерево для теста {}", test);

        TimestampLabel timestampLabel = TimestampLabel.create();
        H3 h3 = new H3("Построено дерево тестов для проверки:");
        h3.addClassName("tree-title");
        HorizontalLayout header = new HorizontalLayout(timestampLabel, h3);

        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.add(header, new TestTreeView(test));

        currentTestTreeView = (TestTreeView) section.getComponentAt(1);
        log.debug("doOnBuiltTestTree: currentTestTreeView установлен, promptStatusMap.size()={}",
                currentTestTreeView.promptStatusMap.size());
        return section;
    }


    /**
           * Обновляет статус на "▶️" (in progress) с ссылкой на чат
 */
    public void doOnPromptStarted(CheckEvent.CheckPromptStartedEvent event) {
        if (isNull(currentTestTreeView)) {
            log.error("doOnPromptStarted: currentTestTreeView == null!");
            throw new RuntimeException("нет построенного дерева тестов");
        }

        TestTree.Test test = event.getTest();
        int promptIndex = event.getPromptIndex();
        String key = test.getId() + "_" + promptIndex;

        log.debug("doOnPromptStarted: test={}, promptIndex={}, key='{}', conversationId='{}'",
                test, promptIndex, key, event.getConversationId());

        UiUtils.doInUI(currentTestTreeView, () -> {
            Span span = currentTestTreeView.promptStatusMap.get(key);
            if (span == null) {
                log.error("doOnPromptStarted: span не найден для ключа '{}'. Доступные ключи: {}",
                        key, currentTestTreeView.promptStatusMap.keySet());
                throw new RuntimeException("span по ключу '%s' не найден".formatted(key));
            }

            // ✅ ИСПРАВЛЕНО: setText + setClassName
            span.setClassName("status-progress qwe");  // progress + пульсация

            String statusText = String.format("▶️ %s проверяется промпт %d: <a href=\"/?chatId=%s\" target=\"_blank\">%s</a>",
                    test, promptIndex, event.getConversationId(), event.getConversationId());
            span.getElement().setProperty("innerHTML", statusText); // HTML остается для ссылок

            log.debug("doOnPromptStarted: обновляем span для ключа '{}' → ▶️", key);
        });
    }

    /**
     * Обновляет статус на "✅" или "💀" с ссылкой на чат
     */
    public void doOnCheckFinished(CheckEvent.CheckFinishedEvent event) {
        if (isNull(currentTestTreeView)) {
            log.error("doOnCheckFinished: currentTestTreeView == null!");
            throw new RuntimeException("нет построенного дерева тестов");
        }

        TestTree.Test test = event.getTest();
        String conversationId = event.getConversationId();
        String key = getAfterFirstUnderscore(conversationId.replace("_prompt", ""));

        log.debug("doOnCheckFinished: test={}, conversationId='{}', вычисленный key='{}'",
                test, conversationId, key);

        UiUtils.doInUI(currentTestTreeView, () -> {
            Span span = currentTestTreeView.promptStatusMap.get(key);
            if (span == null) {
                log.error("doOnCheckFinished: span НЕ НАЙДЕН для ключа '{}'. Доступные ключи: {}",
                        key, currentTestTreeView.promptStatusMap.keySet());
                return;
            }

            Throwable t = event.getThrowable();
            String statusClass = t != null ? "status-failed" : "status-done";
            String emoji = t != null ? "💀" : "✅";

            String status = String.format("%s %s проверен <a href=\"/?chatId=%s\" target=\"_blank\">%s</a>",
                    emoji, test, conversationId, conversationId);

            // ✅ ИСПРАВЛЕНО: правильный порядок + классы
            span.setClassName(statusClass);  // Сначала очищаем и ставим статус-класс
            span.getElement().setProperty("innerHTML", status); // Потом HTML

            log.debug("doOnCheckFinished: обновляем span для ключа '{}' → {}", key, emoji);
        });
    }

    private static String getAfterFirstUnderscore(@NonNull String fullKey) {
        int firstUnderscore = fullKey.indexOf('_');
        String result = firstUnderscore >= 0 ? fullKey.substring(firstUnderscore + 1) : fullKey;
        log.debug("getAfterFirstUnderscore('{}') → '{}'", fullKey, result);
        return result;
    }
}
