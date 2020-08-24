package my.dub.dlp_pilot.exception.rest;

import lombok.Getter;

@Getter
public class UnexpectedEndpointResponseException extends RuntimeException {
    private final String exchange;
    private final String message;

    private final String code;

    public UnexpectedEndpointResponseException(String exchange, String code, String message) {
        super(String.format("Unexpected response! Exchange: %s | Error code: %s | Message: %s", exchange, code,
                            message));
        this.exchange = exchange;
        this.code = code;
        this.message = message;
    }

    public UnexpectedEndpointResponseException(String exchange, String message) {
        super(String.format("Unexpected response! Exchange: %s | Message: %s", exchange, message));
        this.exchange = exchange;
        this.message = message;
        this.code = "";
    }
}
