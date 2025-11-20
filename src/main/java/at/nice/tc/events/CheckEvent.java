package at.nice.tc.events;


import at.nice.tc.model.TestTree;
import lombok.Getter;

/**
 * Проверка теста агентами
 */
@Getter
public abstract class CheckEvent extends ToolEvent {
    private final TestTree test;

    public CheckEvent(String conversationId, TestTree test) {
        super(conversationId);
        this.test = test;
    }


    /**
     * Агент построил дерево тестов перед проверкой
     */
    @Getter
    public static class AgentBuiltTestTreeEvent extends CheckEvent {

        public AgentBuiltTestTreeEvent(String conversationId, TestTree test) {
            super(conversationId, test);
        }
    }


    /**
     * Начата проверка теста агентами
     */
    @Getter
    public static class CheckStartedEvent extends CheckEvent {
        private final TestTree.Test test;

        public CheckStartedEvent(String conversationId, TestTree.Test test) {
            super(conversationId, test);
            this.test = test;
        }

    }


    /**
     * Закончена проверка теста агентами
     */
    @Getter
    public static class CheckFinishedEvent extends CheckEvent {
        private final TestTree.Test test;
        private final Throwable throwable;

        public CheckFinishedEvent(String conversationId, TestTree.Test test, Throwable throwable) {
            super(conversationId, test);
            this.test = test;
            this.throwable = throwable;
        }

    }

}
