package ai.kodari.java.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum KodariModel {
    MODERATION("moderation"),
    BASIC("basic")
    ;

    String modelId;
}
