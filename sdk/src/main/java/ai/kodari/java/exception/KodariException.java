package ai.kodari.java.exception;

public class KodariException extends RuntimeException {

    public KodariException(
            String message
    ) {
        super(
                message
        );
    }

    public KodariException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}
