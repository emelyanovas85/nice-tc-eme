package at.nice.tc.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class ThrowableUtils {

    /**
     * Переданная ошибка в виде строки как {@link Throwable#printStackTrace()}
     */
    public static String asString(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static <T extends Throwable, R> R reThrow(Throwable t) throws T {
        //noinspection unchecked
        throw (T) t;
    }
}
