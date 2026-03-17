package ai.kodari.java;

import ai.kodari.java.exception.KodariAuthenticationException;
import ai.kodari.java.exception.KodariException;
import ai.kodari.java.exception.KodariInsufficientTokensException;
import ai.kodari.java.exception.KodariRateLimitException;
import ai.kodari.java.http.HttpResponse;
import ai.kodari.java.http.NettyHttpClient;
import ai.kodari.java.model.KodariModel;
import ai.kodari.java.model.response.ModerationResult;
import ai.kodari.java.model.response.ModelResponse;
import ai.kodari.java.model.response.UserResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class KodariClient implements Closeable {

    private static final String DEFAULT_BASE_URL = "https://api.kodari.ai";
    private static final String API_PREFIX = "/api/v1"; // used everywhere
    private static final String USER_AGENT = "kodari-java/1.0.0";
    private static final Gson GSON = new Gson();

    private final KodariCredentials credentials;
    private final String baseUrl;
    private final NettyHttpClient httpClient;

    private KodariClient(
            Builder builder
    ) {
        this.credentials = builder.credentials;
        this.baseUrl = builder.baseUrl;
        this.httpClient = new NettyHttpClient();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Normal endpoints
     */

    public CompletableFuture<UserResponse> getMe() {
        return get("/users/me")
                .thenApply(json -> GSON.fromJson(json, UserResponse.class));
    }

    /**
     * Kodari Specialized Models
     * @see <a href="https://kodari.ai/api-keys">Available Models</a>
     */

    public CompletableFuture<ModerationResult> moderate(
            String message
    ) {
        return execute(KodariModel.MODERATION, message)
                .thenApply(response -> {
                    JsonObject result = response.getResult().getAsJsonObject();
                    return new ModerationResult(
                            result.get("safe").getAsBoolean(),
                            result.get("category").getAsString(),
                            result.get("severity").getAsString()
                    );
                });
    }

    /**
     * If you send 5+ messages, you will get a discount of 50%
     */
    public CompletableFuture<List<ModerationResult>> moderateBatch(
            List<String> messages
    ) {
        return executeBatch(KodariModel.MODERATION, messages)
                .thenApply(response -> {
                    JsonArray results = response.getResult().getAsJsonArray();
                    return results.asList().stream()
                            .map(element -> {
                                JsonObject result = element.getAsJsonObject();
                                return new ModerationResult(
                                        result.has("index") ? result.get("index").getAsInt() : -1,
                                        result.get("safe").getAsBoolean(),
                                        result.get("category").getAsString(),
                                        result.get("severity").getAsString()
                                );
                            })
                            .collect(Collectors.toList());
                });
    }

    public CompletableFuture<ModelResponse> execute(
            String model,
            String input
    ) {
        JsonObject body = new JsonObject();
        body.addProperty("input", input);

        return post("/models/" + model, body)
                .thenApply(json -> new ModelResponse(
                        json.get("kodariModel").getAsString(),
                        json.get("tokensCost").getAsLong(),
                        json.get("result")
                ));
    }

    public CompletableFuture<ModelResponse> executeBatch(
            String model,
            List<String> inputs
    ) {
        JsonObject body = new JsonObject();
        body.add("inputs", GSON.toJsonTree(inputs));

        return post("/models/" + model + "/batch", body)
                .thenApply(json -> new ModelResponse(
                        json.get("kodariModel").getAsString(),
                        json.get("tokensCost").getAsLong(),
                        json.get("result")
                ));
    }

    /**
     * Generic API access for endpoints without typed methods
     */

    public CompletableFuture<JsonObject> get(
            String path
    ) {
        String url = baseUrl + API_PREFIX + path;
        return httpClient.get(url, authHeaders())
                .thenApply(this::handleResponse);
    }

    public CompletableFuture<JsonObject> post(
            String path,
            JsonObject body
    ) {
        String url = baseUrl + API_PREFIX + path;
        Map<String, String> headers = authHeaders();
        headers.put("Content-Type", "application/json");

        return httpClient.post(url, headers, GSON.toJson(body))
                .thenApply(this::handleResponse);
    }

    /**
     * Internal
     */

    private Map<String, String> authHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-API-Key", credentials.apiKey());
        headers.put("User-Agent", USER_AGENT);
        return headers;
    }

    private JsonObject handleResponse(
            HttpResponse response
    ) {
        int status = response.statusCode();

        if (status == 401)
            throw new KodariAuthenticationException("Invalid or expired API key");

        if (status == 429)
            throw new KodariRateLimitException("Rate limit exceeded");

        if (status == 402)
            throw new KodariInsufficientTokensException("Insufficient tokens");

        if (status != 200)
            throw new KodariException("API error (HTTP " + status + "): " + response.body());

        return GSON.fromJson(response.body(), JsonObject.class);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    public static final class Builder {

        private KodariCredentials credentials;
        private String baseUrl = DEFAULT_BASE_URL;

        private Builder() {}

        public Builder credentials(
                KodariCredentials credentials
        ) {
            this.credentials = credentials;
            return this;
        }

        public Builder baseUrl(
                String baseUrl
        ) {
            this.baseUrl = baseUrl;
            return this;
        }

        public KodariClient build() {
            Objects.requireNonNull(credentials, "credentials must be set");
            Objects.requireNonNull(baseUrl, "baseUrl must not be null");
            return new KodariClient(this);
        }
    }
}
