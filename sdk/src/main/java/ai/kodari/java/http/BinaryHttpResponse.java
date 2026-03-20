package ai.kodari.java.http;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class BinaryHttpResponse {

    private final int statusCode;
    private final byte[] body;
    private final String filename;

    public int statusCode() {
        return statusCode;
    }

    public byte[] body() {
        return body;
    }

    public String filename() {
        return filename;
    }
}
