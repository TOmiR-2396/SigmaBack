package com.example.gym.exception;

public class DeactivatedUserException extends RuntimeException {
    public DeactivatedUserException(String message) {
        super(message);
    }

    public DeactivatedUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
