package my.dub.dlp_pilot.exception.rest;

import lombok.Getter;

@Getter
public class UnexpectedEndpointResponseException extends RuntimeException {
    private final String exchange;
    private final String description;

    private final String code;

    public UnexpectedEndpointResponseException(String exchange, String code, String description) {
        super(String.format("Unexpected response! Exchange: %s | Error code: %s | Message: %s", exchange, code,
                            description));
        this.exchange = exchange;
        this.code = code;
        this.description = description;
    }

    public UnexpectedEndpointResponseException(String exchange, String description) {
        super(String.format("Unexpected response! Exchange: %s | Message: %s", exchange, description));
        this.exchange = exchange;
        this.description = description;
        this.code = "";
    }
}
