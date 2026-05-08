package at.nice.tc.ui.components;

import at.nice.tc.ai.client.Prompts;
import at.nice.tc.model.TestTree;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Slf4j
@CssImport(value = "./components/test-tree-styles.css")
public class TestTreeView extends VerticalLayout {
    final Map<Integer, ListItem> testIdToItem = new ConcurrentHashMap<>();
    final Map<String, Span> promptStatusMap = new ConcurrentHashMap<>();
    final TestTree root;

    public TestTreeView(TestTree root) {
        this.root = root;
        setSpacing(true);
        setPadding(true);
        add(buildTestTree(root));
    }

    private Component buildTestTree(TestTree root) {
        OrderedList ul = new OrderedList();
        ul.addClassNames("test-tree");
        buildTreeRecursive(root, ul);
        return ul;
    }

    private void buildTreeRecursive(TestTree node, HtmlContainer parentUl) {
        ListItem item = (ListItem) buildLeafNode((TestTree.Test) node);
        parentUl.add(item);

        if (node.hasChildren()) {
            HtmlContainer childrenUl = new OrderedList();
            childrenUl.addClassNames("children-list");
            item.add(childrenUl);
            node.getChildren().forEach(child ->
                    buildTreeRecursive(child, childrenUl));
        }
    }

    private Component buildLeafNode(TestTree.Test leaf) {
        ListItem li = createTestItem(leaf);
        addPromptsList(li, leaf);
        return li;
    }


    private ListItem createTestItem(TestTree.Test t) {
        ListItem li = new ListItem();
        testIdToItem.put(t.getId(), li);

        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setAlignItems(Alignment.CENTER);
        row.addClassNames("test-row");

        // Иконка + имя теста
        Span icon = new Span(getTreeIcon(t.getDepth()));
        icon.addClassNames("tree-icon");
        row.add(icon);

        Span name = new Span(t.toString());
        name.addClassNames("test-name");
        row.add(name);

        li.add(row);

        return li;
    }

    private void addPromptsList(ListItem li, TestTree.Test test) {
        int promptCount = Prompts.REQUIREMENTS.size();
        if (promptCount == 0) return;

        HtmlContainer promptsUl = new OrderedList();
        promptsUl.addClassNames("prompts-list");

        IntStream.range(0, promptCount).forEach(promptIndex -> {
            ListItem promptLi = new ListItem();
            promptLi.addClassNames("prompt-item");

            // Span для статуса промпта с уникальным ID
            String promptKey = test.getId() + "_" + promptIndex;
            Span promptStatus = new Span("⏸️ " + test + "_" + promptIndex);
            promptStatus.addClassNames("prompt-status", "status-pending");

            log.debug("TestTreeView: создали ключ '{}' для теста {}  test.getId()='{}', promptIndex={}", promptKey, test, test.getId(), promptIndex);
            promptStatusMap.put(promptKey, promptStatus);

            promptLi.add(promptStatus);
            promptsUl.add(promptLi);
        });

        li.add(promptsUl);
    }


    private String getTreeIcon(int depth) {
        return switch (depth) {
            case 0 -> "🌳";
            case 1 -> "📁";
            default -> "📄";
        };
    }
}
