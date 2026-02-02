package at.nice.tc.utils;

import java.util.function.Supplier;

/**
 * Как обычный {@link Supplier}, но принимает код, бросающий исключения
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {
    T get() throws Throwable;
}
