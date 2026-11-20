package com.slotbook.core.payments;

/** The gateway did not answer in time. The charge may or may not have gone through. */
public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException(String message) {
        super(message);
    }
}
