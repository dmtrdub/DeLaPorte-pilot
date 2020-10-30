package my.dub.dlp_pilot.exception.client;

import lombok.Getter;

@Getter
public class UnexpectedEndpointResponseException extends RuntimeException {
    private final String exchange;
    private final String details;

    private final String code;

    public UnexpectedEndpointResponseException(String exchange, String code, String details) {
        super(String.format("Unexpected response! Exchange: %s | Error code: %s | Message: %s", exchange, code,
                            details));
        this.exchange = exchange;
        this.code = code;
        this.details = details;
    }

    public UnexpectedEndpointResponseException(String exchange, String details) {
        super(String.format("Unexpected response! Exchange: %s | Message: %s", exchange, details));
        this.exchange = exchange;
        this.details = details;
        this.code = "";
    }
}
