package com.slotbook.contract;

/** Core turns this into HTTP 401. */
public class NotSignedInException extends RuntimeException {

    public NotSignedInException() {
        super("Not signed in");
    }
}
