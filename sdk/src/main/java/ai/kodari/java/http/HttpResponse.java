package ai.kodari.java.http;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class HttpResponse {

    private final int statusCode;
    private final String body;

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }
}
