package com.evidencepilot.exception;

/**
 * Thrown when a requested resource does not exist or is not accessible
 * to the authenticated user.
 *
 * <p>
 * By design, this exception is used for <em>both</em> "resource does not
 * exist" and "resource belongs to another tenant" scenarios. Returning the
 * same 404 response in both cases prevents an attacker from distinguishing
 * between non-existence and unauthorized access (enumeration attack).
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
