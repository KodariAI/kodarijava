package ai.kodari.java.model.response;

import lombok.Getter;

@Getter
public final class ModerationResult {

    private final int index;
    private final boolean safe;
    private final String category;
    private final String severity;

    public ModerationResult(
            boolean safe,
            String category,
            String severity
    ) {
        this(
                -1,
                safe,
                category,
                severity
        );
    }

    public ModerationResult(
            int index,
            boolean safe,
            String category,
            String severity
    ) {
        this.index = index;
        this.safe = safe;
        this.category = category;
        this.severity = severity;
    }

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