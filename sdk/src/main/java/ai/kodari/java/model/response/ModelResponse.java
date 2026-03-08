package ai.kodari.java.model.response;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ModelResponse {

    private final String kodariModel;
    private final long tokensCost;
    private final JsonElement result;
}
