package uk.ac.newcastle.enterprisemiddleware.flight;

import javax.validation.ValidationException;

/**
 * <p>ValidationException caused if a Flight's flight number address conflicts with that of another Flight.</p>
 *
 * <p>This violates the uniqueness constraint.</p>
 *
 * @author hugofirth
 * @see Flight
 */
public class UniqueFlightNumberException extends ValidationException {

    public UniqueFlightNumberException(String message) {
        super(message);
    }

    public UniqueFlightNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public UniqueFlightNumberException(Throwable cause) {
        super(cause);
    }
}

