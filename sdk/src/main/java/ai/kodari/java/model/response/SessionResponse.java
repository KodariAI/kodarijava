package ai.kodari.java.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class SessionResponse {

    private final String id;
    private final String name;
    private final String gameType;
    private final String category;
    private final String status;
}
