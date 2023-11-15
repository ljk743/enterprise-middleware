package uk.ac.newcastle.enterprisemiddleware.booking;

import javax.validation.ValidationException;

/**
 * <p>ValidationException caused if a Flight's email address conflicts with that of another Flight.</p>
 *
 * <p>This violates the uniqueness constraint.</p>
 *
 * @author hugofirth
 * @see Booking
 */
public class UniqueIdException extends ValidationException {

    public UniqueIdException(String message) {
        super(message);
    }

    public UniqueIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public UniqueIdException(Throwable cause) {
        super(cause);
    }
}

