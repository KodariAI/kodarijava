package ai.kodari.java.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class GenerateResponse {

    private final String sessionId;
    private final boolean success;
    private final String message;
}
