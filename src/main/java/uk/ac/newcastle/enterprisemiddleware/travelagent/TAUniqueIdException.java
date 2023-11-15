package uk.ac.newcastle.enterprisemiddleware.travelagent;

import javax.validation.ValidationException;

/**
 * <p>ValidationException caused if a Flight's email address conflicts with that of another Flight.</p>
 *
 * <p>This violates the uniqueness constraint.</p>
 *
 * @author hugofirth
 * @see TravelAgentBooking
 */
public class TAUniqueIdException extends ValidationException {

    public TAUniqueIdException(String message) {
        super(message);
    }

    public TAUniqueIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public TAUniqueIdException(Throwable cause) {
        super(cause);
    }
}

