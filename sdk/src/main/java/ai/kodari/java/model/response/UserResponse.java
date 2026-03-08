package ai.kodari.java.model.response;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class UserResponse {

    private final String id;
    private final String providerId;
    private final String email;
    private final String name;
    private final String imageUrl;
    private final String role;
    private final String discordId;
    private final String bio;
    private final String createdAt;
    private final boolean banned;
    private final String bannedReason;
    private final JsonObject settings;
    private final boolean newAccount;
}