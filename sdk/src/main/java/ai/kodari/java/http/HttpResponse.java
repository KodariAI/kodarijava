package ai.kodari.java.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class HttpResponse {

    private final int statusCode;
    private final String body;
}
