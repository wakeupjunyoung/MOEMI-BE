package com.example.moemi.hazard;

/**
 * The hazard source could not be read. Messages must never contain the API key.
 */
public class HazardFetchException extends RuntimeException {

    public HazardFetchException(String message) {
        super(message);
    }
}
