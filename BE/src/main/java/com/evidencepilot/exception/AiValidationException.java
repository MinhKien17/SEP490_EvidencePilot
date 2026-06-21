package com.evidencepilot.exception;

/**
 * Thrown when the AI service returns a response that fails integrity
 * validation (e.g. null verdict, null confidence, or confidence outside
 * the 0.0–1.0 range).
 *
 * <p>Because this is an unchecked exception thrown inside a
 * {@link org.springframework.transaction.annotation.Transactional}
 * boundary, Spring will automatically roll back the transaction,
 * preventing corrupt data from reaching the database.</p>
 */
public class AiValidationException extends RuntimeException {

    public AiValidationException(String message) {
        super(message);
    }
}
