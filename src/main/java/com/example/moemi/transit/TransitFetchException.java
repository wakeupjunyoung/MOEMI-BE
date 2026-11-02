package com.example.moemi.transit;

/**
 * TAGO could not be read. Messages must never contain the service key.
 */
public class TransitFetchException extends RuntimeException {

    public TransitFetchException(String message) {
        super(message);
    }
}
