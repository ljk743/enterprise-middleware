package uk.ac.newcastle.enterprisemiddleware.travelagent;


import uk.ac.newcastle.enterprisemiddleware.customer.Customer;
import uk.ac.newcastle.enterprisemiddleware.customer.CustomerRepository;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>This Service assumes the Control responsibility in the ECB pattern.</p>
 *
 * <p>The validation is done here so that it may be used by other Boundary Resources. Other Business Logic would go here
 * as well.</p>
 *
 * <p>There are no access modifiers on the methods, making them 'package' scope.  They should only be accessed by a
 * Boundary / Web Service class with public methods.</p>
 *
 * @author Joshua Wilson
 * @see TravelAgentBookingValidator
 * @see TravelAgentBookingRepository
 */
@Dependent
public class TravelAgentBookingService {

    @Inject
    @Named("logger")
    Logger log;

    @Inject
    TravelAgentBookingValidator validator;

    @Inject
    TravelAgentBookingRepository crud;

    @Inject
    CustomerRepository customerCurd;
    //Removed temporarily due to non-existing AreaService
    //@RestClient
    //AreaService areaService;

    /**
     * <p>Returns a List of all persisted {@link TravelAgentBooking} objects, sorted alphabetically by last name.<p/>
     *
     * @return List of Booking objects
     */
    List<TravelAgentBooking> findAllOrdered() {
        return crud.findAllOrdered();
    }

    /**
     * <p>Returns a single Booking object, specified by a Long id.<p/>
     *
     * @param id The id field of the Booking to be returned
     * @return The Booking with the specified id
     */
    TravelAgentBooking findById(Long id) {
        return crud.findById(id);
    }

    /**
     * <p>Returns a single Booking object, specified by a String email.</p>
     *
     * <p>If there is more than one Booking with the specified email, only the first encountered will be returned.<p/>
     *
     * @param customerId The email field of the Booking to be returned
     * @return The first Booking with the specified email
     */
    TravelAgentBooking findByCustomerId(Long customerId) {
        return crud.findByCustomerId(customerId);
    }

    /**
     * <p>Returns a single Booking object, specified by a String firstName.<p/>
     *
     * @param flightId The firstName field of the Booking to be returned
     * @return The first Booking with the specified firstName
     */
    List<TravelAgentBooking> findAllByFlightId(Long flightId) {
        return crud.findAllByFlightId(flightId);
    }

    /**
     * <p>Returns a single Booking object, specified by a String lastName.<p/>
     *
     * @param orderDate The lastName field of the Bookings to be returned
     * @return The Bookings with the specified lastName
     */
    List<TravelAgentBooking> findAllByOrderDate(String orderDate) {
        return crud.findAllByOrderDate(orderDate);
    }

    /**
     * <p>Writes the provided Booking object to the application database.<p/>
     *
     * <p>Validates the data in the provided Booking object using a {@link TravelAgentBookingValidator} object.<p/>
     *
     * @param booking The Booking object to be written to the database using a {@link TravelAgentBookingRepository} object
     * @return The Booking object that has been successfully written to the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    TravelAgentBooking create(TravelAgentBooking booking) throws Exception {
        log.info("BookingService.create() - Creating " + booking.getCustomerId());

        // Check to make sure the data fits with the parameters in the Booking model and passes validation.
        validator.validateAgentBooking(booking);

        Customer customer = customerCurd.findById(booking.getCustomerId());
        // Write the booking to the database.
        return crud.create(booking, customer);
    }

    /**
     * <p>Updates an existing Booking object in the application database with the provided Booking object.<p/>
     *
     * <p>Validates the data in the provided Booking object using a BookingValidator object.<p/>
     *
     * @param booking The Booking object to be passed as an update to the application database
     * @return The Booking object that has been successfully updated in the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    TravelAgentBooking update(TravelAgentBooking booking) throws Exception {
        log.info("BookingService.update() - Updating " + booking.getFlightId().toString() + "  " + booking.getOrderDate().toString());

        // Check to make sure the data fits with the parameters in the Booking model and passes validation.
        validator.validateAgentBooking(booking);

        /*try {
            //Removed temporarily due to non-existing AreaService
            Area area = areaService.getAreaById(Integer.parseInt(booking.getPhoneNumber().substring(1, 4)));
            booking.setState(area.getState());
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatusInfo() == Response.Status.NOT_FOUND) {
                throw new InvalidAreaCodeException("The area code provided does not exist", e);
            } else {
                throw e;
            }
        }*/

        // Either update the booking or add it if it can't be found.
        return crud.update(booking);
    }

    /**
     * <p>Deletes the provided Booking object from the application database if found there.<p/>
     *
     * @param booking The Booking object to be removed from the application database
     * @return The Booking object that has been successfully removed from the application database; or null
     * @throws Exception
     */
    TravelAgentBooking delete(TravelAgentBooking booking) throws Exception {
        log.info("delete() - Deleting " + booking.toString());

        TravelAgentBooking deletedBooking = null;

        if (booking.getId() != null) {
            deletedBooking = crud.delete(booking);
        } else {
            log.info("delete() - No ID was found so can't Delete.");
        }

        return deletedBooking;
    }
}
