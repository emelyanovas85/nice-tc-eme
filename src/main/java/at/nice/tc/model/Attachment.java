package at.nice.tc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface Attachment {

    String getName();
    String getContent();

    @AllArgsConstructor
    @Getter
    class Text implements Attachment {
        private final String name, content;
    }
}
