package my.dub.dlp_pilot.exception;

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
