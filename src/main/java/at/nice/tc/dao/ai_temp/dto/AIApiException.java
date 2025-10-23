package at.nice.tc.dao.ai_temp.dto;

import lombok.Getter;

/**
 * Исключение для операций AI API клиентов.
 */
@Getter
public class AIApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;
    private final String model;

    public AIApiException(String message, String model) {
        super(message);
        this.statusCode = -1;
        this.responseBody = null;
        this.model = model;
    }

    public AIApiException(String message, String model, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
        this.model = model;
    }

    public AIApiException(String message, String model, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.model = model;
    }

    public boolean hasStatusCode() { return statusCode != -1; }
}
