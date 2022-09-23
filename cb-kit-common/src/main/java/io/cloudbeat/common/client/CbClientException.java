package io.cloudbeat.common.client;

public class CbClientException extends Exception {
    public CbClientException() {
    }

    public CbClientException(String message) {
        super(message);
    }

    public CbClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CbClientException(Throwable cause) {
        super(cause);
    }

    public CbClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
