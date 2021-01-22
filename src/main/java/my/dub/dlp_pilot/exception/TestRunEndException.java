package my.dub.dlp_pilot.exception;

/**
 * Exception indicating the end of test phase of the application.
 */
public class TestRunEndException extends RuntimeException {

    public TestRunEndException(Throwable t) {
        super(t);
    }

    public TestRunEndException() {
        super("Test Run finished");
    }

    public TestRunEndException(String message) {
        super(message);
    }
}
