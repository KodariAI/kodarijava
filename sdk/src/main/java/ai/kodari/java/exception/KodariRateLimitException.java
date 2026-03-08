package ai.kodari.java.exception;

public class KodariRateLimitException extends KodariException {

    public KodariRateLimitException(
            String message
    ) {
        super(
                message
        );
    }
}
