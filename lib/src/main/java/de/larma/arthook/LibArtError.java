package de.larma.arthook;

public class LibArtError extends RuntimeException {

    public LibArtError() {
    }

    public LibArtError(String detailMessage) {
        super(detailMessage);
    }

    public LibArtError(Throwable cause) {
        super(cause);
    }

    public LibArtError(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
