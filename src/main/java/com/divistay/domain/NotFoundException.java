package com.divistay.domain;

/** The requested hotel, group or payment link does not exist (or the link is invalid). */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
