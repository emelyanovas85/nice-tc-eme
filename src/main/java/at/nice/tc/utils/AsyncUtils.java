package at.nice.tc.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AsyncUtils {

    public static <T, R> Function<T, CompletableFuture<R>> asCompletableFuture(Function<T, R> func) {
        return t -> CompletableFuture.supplyAsync(() -> func.apply(t));
    }
}
