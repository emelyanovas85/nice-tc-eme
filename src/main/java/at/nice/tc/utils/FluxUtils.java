package at.nice.tc.utils;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public abstract class FluxUtils {

    /**
     * Работает как {@link CompletableFuture#join()}.
     * Важно!
     * Переданный flux должен быть {@link Flux#replay()}
     */
    public static String blockHotFlux(Flux<String> flux, Duration timeout) {
        try {
            return flux
                    .reduce(new StringBuilder(), StringBuilder::append)
                    .map(StringBuilder::toString)
                    .block(timeout);  // ← С таймаутом!
        } catch (RuntimeException e) {
            return ThrowableUtils.reThrow(e);
        }
    }
}
