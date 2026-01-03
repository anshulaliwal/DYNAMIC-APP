package com.saasapp.dynamic_app.exception;

/**
 * Exception thrown when email sending operation fails
 * Provides clear error messages for debugging email delivery issues
 */
public class EmailSendingException extends RuntimeException {

    public EmailSendingException(String message) {
        super(message);
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}

