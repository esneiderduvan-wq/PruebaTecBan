package com.ban.brp.exception;

public class BrpImportException extends RuntimeException {
    public BrpImportException(String message) {
        super(message);
    }

    public BrpImportException(String message, Throwable cause) {
        super(message, cause);
    }
}