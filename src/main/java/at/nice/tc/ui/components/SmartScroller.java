package at.nice.tc.ui.components;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.Scroller;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Расширяет стандартный {@link Scroller} методом {@link #scrollToBottom()},
 * который скроллит к низу панели, если установлен флаг {@link #stickDown}
 */
public class SmartScroller extends Scroller {
    private final AtomicBoolean stickDown = new AtomicBoolean(true);

    public SmartScroller(Component content) {
        super(content);
        addAttachListener(e -> {
            getElement().executeJs(
                    // language=jav
                    """
                                var el = this;
                                var lastScrollTop = 0;
                            
                                el.addEventListener("scroll", function(e) {
                                    var currentScrollTop = el.scrollTop;
                            
                                    if (currentScrollTop < lastScrollTop) { // Скролл вверх
                                        el.$server.onScrollUp();
                            
                                    } else if (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) { // достигли дна (добавляем небольшой допуск (1px) для защиты от ошибок округления)
                                        el.$server.onScrolledToBottom();
                                    }
                                    lastScrollTop = currentScrollTop;
                                });
                            """,
                    getElement()
            );
        });
    }

    /**
     * вызывается из javascript
     */
    @SuppressWarnings("unused")
    @ClientCallable
    public void onScrollUp() {
        setStickDown(false);
    }

    /**
     * вызывается из javascript
     */
    @SuppressWarnings("unused")
    @ClientCallable
    public void onScrolledToBottom() {
        setStickDown(true);
    }

    /**
     * Переключает флаг: true - скроллить, false - не скроллить
     */
    public void setStickDown(boolean flag) {
        stickDown.set(flag);
    }

    @Override
    public void scrollToBottom() {
        if (stickDown.get())
            super.scrollToBottom();
    }
}
