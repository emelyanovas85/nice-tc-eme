package at.nice.tc.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Getter
public class TestTree {
    private final Set<Test> children = Collections.synchronizedSet(new LinkedHashSet<>());

    public void addTest(Test test) {
        children.add(test);
    }

    public List<Test> getDescendants() {
        return new ArrayList<>() {{
            if (TestTree.this instanceof Test test)
                add(test);
            children.forEach(test -> addAll(test.getDescendants()));
        }};
    }


    @Getter
    @RequiredArgsConstructor
    public static class Test extends TestTree {
        private final int id;
        private final String key;
        private final int version;
        private int depth;

        @Override
        public void addTest(Test test) {
            super.addTest(test);
            test.depth = depth + 1;
        }

        @Override
        public String toString() {
            return "%s (%s.0)".formatted(key, version);
        }
    }
}
