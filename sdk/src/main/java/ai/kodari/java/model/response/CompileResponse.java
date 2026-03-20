package ai.kodari.java.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CompileResponse {

    private final String sessionId;
    private final boolean success;
    private final Long jarId;
    private final String pluginName;
    private final Integer jarSize;
    private final String error;
}
