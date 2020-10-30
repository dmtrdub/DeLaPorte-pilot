package my.dub.dlp_pilot.exception.client;

import lombok.Getter;

@Getter
public class UnexpectedResponseStatusCodeException extends RuntimeException {
    private final String exchange;
    private final int code;
    private final String url;

    public UnexpectedResponseStatusCodeException(String exchange, int code, String url) {
        super(String.format("Unexpected response status code! Exchange: %s | Code: %s | URL: %s", exchange, code, url));
        this.exchange = exchange;
        this.code = code;
        this.url = url;
    }
}
