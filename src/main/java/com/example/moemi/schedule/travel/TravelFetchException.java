package com.example.moemi.schedule.travel;

/**
 * ODsay could not be read. Messages must never contain the API key.
 */
public class TravelFetchException extends RuntimeException {

    public TravelFetchException(String message) {
        super(message);
    }
}
