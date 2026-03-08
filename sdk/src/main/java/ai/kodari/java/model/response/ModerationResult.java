package ai.kodari.java.model.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ModerationResult {

    private final boolean safe;
    private final String category;
    private final String severity;

    public boolean isToxic() {
        return "toxicity".equals(category);
    }

    public boolean isThreat() {
        return "threat".equals(category);
    }

    public boolean isDoxxing() {
        return "doxxing".equals(category);
    }

    public boolean isAdvertising() {
        return "advertising".equals(category);
    }

    public boolean isSpam() {
        return "spam".equals(category);
    }
}