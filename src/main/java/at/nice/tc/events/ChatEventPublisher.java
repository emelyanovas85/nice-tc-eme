package at.nice.tc.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Позволяет отправлять события для определенной беседы.
 * Нельзя сделать компонентом, потому что нужно передавать id чата, поэтому для
 * создания экземпляра должен использоваться {@link ChatEventPublisher.Factory}
 */
@Slf4j
public record ChatEventPublisher(ApplicationEventPublisher eventPublisher,
                                 String conversationId) {

    /**
     * Предоставляет {@link #conversationId} для формирования события.
     * Отправляет созданное событие
     */
    public void publish(Function<String, ? extends ChatEvent> fn) {
        ChatEvent event = fn.apply(conversationId);
        publish(event);
    }

    /**
     * Отправляет переданное событие.
     * Глушит все исключения чтобы не влиять на приложение
     */
    public void publish(ChatEvent eve) {
        try {
            eventPublisher.publishEvent(eve);
        } catch (Throwable t) {
            log.warn("Ошибка при отправке события", t);
        }
    }


    /**
     * Это компонент, в который спринг может заинжектить ApplicationEventPublisher
     */
    @Component
    public record Factory(ApplicationEventPublisher eventPublisher) {

        public ChatEventPublisher forConversation(String conversationId) {
            return new ChatEventPublisher(eventPublisher, conversationId);
        }
    }

}
