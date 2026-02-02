package at.nice.tc.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
public class TestTree {
    protected final Set<Test> children = Collections.synchronizedSet(new LinkedHashSet<>());

    public void addTest(Test test) {
        children.add(test);
    }

    /**
     * Все потомки включая себя (плоский список для коллекций)
     */
    public List<Test> getDescendants() {
        List<Test> descendants = new ArrayList<>();
        collectDescendants(this, descendants);
        return descendants;
    }

    /**
     * Прямые дети (для UI-дерева)
     */
    public Set<Test> getChildren() {
        return Collections.unmodifiableSet(children);
    }

    /**
     * Рекурсивный сбор всех потомков
     */
    private void collectDescendants(TestTree node, List<Test> result) {
        if (node instanceof Test test) {
            result.add(test);
        }
        node.getChildren().forEach(child -> collectDescendants(child, result));
    }

    /**
     * Проверяет, есть ли потомки
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Root-конструктор для новых деревьев
     */
    public TestTree() {}


    @Getter
    @Setter
    public static class Test extends TestTree {
        private final int id;
        private final String key;
        private final int version;
        private int depth = 0;  // ← Default 0 для root

        public Test(int id, String key, int version) {
            this(id, key, version, 0);
        }

        public Test(int id, String key, int version, int depth) {
            this.id = id;
            this.key = key;
            this.version = version;
            this.depth = depth;
        }

        @Override
        public void addTest(Test test) {
            super.addTest(test);
            test.setDepth(depth + 1);
        }

        @Override
        public String toString() {
            return "%s (v%s.%d)".formatted(key, version, depth);
        }
    }
}
