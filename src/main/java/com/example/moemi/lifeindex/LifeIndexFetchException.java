package com.example.moemi.lifeindex;

/**
 * The life index source could not be read (network error, error code, unreadable body).
 * Messages must never contain the service key.
 */
public class LifeIndexFetchException extends RuntimeException {

    public LifeIndexFetchException(String message) {
        super(message);
    }
}
