package at.nice.tc.ui;

import lombok.Getter;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public enum MessageDelimiters {
    THINK_OPEN("<think>"),
    THINK_CLOSE("</think>"),
    TOOL_OPEN("<tool>"),
    TOOL_CLOSE("</tool>"),
    TOOL_UPDATE("<upd>") {
        final int timeoutLen = String.valueOf(System.currentTimeMillis()).length();
        final Pattern event = Pattern.compile(getPlaceholder() + "\\d{" + timeoutLen + "}");

        @Override
        public int length() {
            return placeholder.length() + timeoutLen;
        }

        @Override
        public int posIn(String s) {
            return posIn1(s);
        }

        @Override
        public int posIn(StringBuilder s) {
            return posIn1(s);
        }

        public int posIn1(CharSequence text) {
            if (text.length() < this.length())
                return -1;
            Matcher e = event.matcher(text);
            return e.find() ? e.start() : -1;
        }
    };

    final String placeholder;

    MessageDelimiters(String placeholder) {
        this.placeholder = placeholder;
    }

    public int length() {
        return placeholder.length();
    }

    public int posIn(String text) {
        return text.indexOf(placeholder);
    }

    public int posIn(StringBuilder buffer) {
        return buffer.indexOf(placeholder);
    }

    public static Optional<Tag> firstIn(String text, Set<MessageDelimiters> of) {
        int minPos = Integer.MAX_VALUE;
        MessageDelimiters firstTag = null;

        for (MessageDelimiters tag : of) {
            int pos = tag.posIn(text);
            if (pos < 0)
                continue;
            if (pos < minPos) {
                minPos = pos;
                firstTag = tag;
            }
        }
        if (firstTag == null)
            return Optional.empty();
        return Optional.of(new Tag(firstTag, minPos));
    }

    public record Tag(MessageDelimiters tag, int pos) {}
}