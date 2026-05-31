package com.melody.melody_stream.core.exception;

public class TransientProcessException extends RuntimeException {

    public TransientProcessException(String message) {
        super(message);
    }

    public TransientProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
