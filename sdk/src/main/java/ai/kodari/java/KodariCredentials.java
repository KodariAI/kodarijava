package ai.kodari.java;

import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public final class KodariCredentials {

    private final String apiKey;

    public static KodariCredentials create(
            String apiKey
    ) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");

        if (!apiKey.startsWith("kod-"))
            throw new IllegalArgumentException("Invalid API key format. Keys must start with 'kod-'");

        return new KodariCredentials(apiKey);
    }

    public String apiKey() {
        return apiKey;
    }
}
