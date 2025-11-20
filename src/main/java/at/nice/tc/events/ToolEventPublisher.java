package at.nice.tc.events;

import at.nice.tc.service.AiToolCallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Позволяет отправлять события для определенной беседы.
 * Нельзя сделать компонентом, потому что нужно передавать id чата, поэтому для
 * создания экземпляра должен использоваться {@link ToolEventPublisher.Factory}
 */
@Slf4j
public record ToolEventPublisher(AiToolCallService eventPublisher,
                                 String conversationId) {

    /**
     * Предоставляет {@link #conversationId} для формирования события.
     * Отправляет созданное событие
     */
    public void publish(Function<String, ? extends ToolEvent> fn) {
        ToolEvent event = fn.apply(conversationId);
        publish(event);
    }

    /**
     * Отправляет переданное событие.
     * Глушит все исключения чтобы не влиять на приложение
     */
    public void publish(ToolEvent eve) {
        try {
            eventPublisher.updateTool(conversationId, eve);
        } catch (Throwable t) {
            log.warn("Ошибка при отправке события", t);
        }
    }


    /**
     * Это компонент, в который спринг может заинжектить ApplicationEventPublisher
     */
    @Component
    public record Factory(AiToolCallService eventPublisher) {

        public ToolEventPublisher forConversation(String conversationId) {
            return new ToolEventPublisher(eventPublisher, conversationId);
        }
    }

}
