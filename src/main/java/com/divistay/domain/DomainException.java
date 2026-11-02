package com.divistay.domain;

/** A business rule was broken. The message is safe to show to the user. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
