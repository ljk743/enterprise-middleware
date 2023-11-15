package uk.ac.newcastle.enterprisemiddleware.booking;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>This class provides methods to check Booking objects against arbitrary requirements.</p>
 *
 * @author Joshua Wilson
 * @see Booking
 * @see BookingRepository
 * @see Validator
 */
@ApplicationScoped
public class BookingValidator {
    @Inject
    Validator validator;

    @Inject
    BookingRepository crud;

    /**
     * <p>Validates the given Booking object and throws validation exceptions based on the type of error. If the error is standard
     * bean validation errors then it will throw a ConstraintValidationException with the set of the constraints violated.<p/>
     *
     *
     * <p>If the error is caused because an existing booking with the same email is registered it throws a regular validation
     * exception so that it can be interpreted separately.</p>
     *
     *
     * @param booking The Booking object to be validated
     * @throws ConstraintViolationException If Bean Validation errors exist
     * @throws ValidationException If booking with the same email already exists
     */
    void validateBooking(Booking booking) throws ConstraintViolationException, ValidationException {
        // Create a bean validator and check for issues.
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<ConstraintViolation<?>>(violations));
        }

        // Check the uniqueness of the email address
        if (bookingNumberAlreadyExists(booking.getFlightId(),booking.getOrderDate(),
                booking.getId())) {
            throw new UniqueIdException("Unique FlightId & orderDate Violation");
        }
    }

    /**
     * <p>Checks if a booking with the same email address is already registered. This is the only way to easily capture the
     * "@UniqueConstraint(columnNames = "email")" constraint from the Booking class.</p>
     *
     * <p>Since Update will being using an email that is already in the database we need to make sure that it is the email
     * from the record being updated.</p>
     *
     * @param flightId The flightId to check is unique
     * @param orderDate The orderDates to check is unique
     * @param id The user id to check the email against if it was found
     * @return boolean which represents whether the email was found, and if so if it belongs to the user with id
     */
    boolean bookingNumberAlreadyExists(Long flightId, Date orderDate, Long id) {
        Booking booking = null;
        Booking bookingWithID = null;
        try {
            booking = crud.findByFlightIdAndOrderDate(flightId,orderDate);
        } catch (NoResultException e) {
            // ignore
        }

        //Check the contact which need to do modification

        if (booking != null && id != null) {
            try {
                bookingWithID = crud.findById(id);
                if (bookingWithID != null && bookingWithID.getFlightId().equals(flightId) &&
                        bookingWithID.getOrderDate().toString().equals(orderDate)) {
                    booking = null;
                }
            } catch (NoResultException e) {
                // ignore
            }
        }
        return booking != null;
    }

}

