package uk.ac.newcastle.enterprisemiddleware.flight;


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
 * @see FlightValidator
 * @see FlightRepository
 */
@Dependent
public class FlightService {

    @Inject
    @Named("logger")
    Logger log;

    @Inject
    FlightValidator validator;

    @Inject
    FlightRepository crud;

    //Removed temporarily due to non-existing AreaService
    //@RestClient
    //AreaService areaService;

    /**
     * <p>Returns a List of all persisted {@link Flight} objects, sorted alphabetically by last name.<p/>
     *
     * @return List of Flight objects
     */
    List<Flight> findAllOrdered() {
        return crud.findAllOrdered();
    }

    /**
     * <p>Returns a single Flight object, specified by a Long id.<p/>
     *
     * @param id The id field of the Flight to be returned
     * @return The Flight with the specified id
     */
    public Flight findById(Long id) {
        return crud.findById(id);
    }

    /**
     * <p>Returns a single Flight object, specified by a String flight number.</p>
     *
     * <p>If there is more than one Flight with the specified flight number, only the first encountered will be returned.<p/>
     *
     * @param flightNumber The flight number field of the Flight to be returned
     * @return The first Flight with the specified flight number
     */
    Flight findByFlightNumber(String flightNumber) {
        return crud.findByFlightNumber(flightNumber);
    }

    /**
     * <p>Returns a single Flight object, specified by a String firstName.<p/>
     *
     * @param departure The firstName field of the Flight to be returned
     * @return The first Flight with the specified firstName
     */
    List<Flight> findAllByDeparture(String departure) {
        return crud.findAllByDeparture(departure);
    }

    /**
     * <p>Returns a single Flight object, specified by a String lastName.<p/>
     *
     * @param destination The lastName field of the Flights to be returned
     * @return The Flights with the specified lastName
     */
    List<Flight> findAllByDestination(String destination) {
        return crud.findAllByDestination(destination);
    }

    /**
     * <p>Writes the provided Flight object to the application database.<p/>
     *
     * <p>Validates the data in the provided Flight object using a {@link FlightValidator} object.<p/>
     *
     * @param flight The Flight object to be written to the database using a {@link FlightRepository} object
     * @return The Flight object that has been successfully written to the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Flight create(Flight flight) throws Exception {
        log.info("FlightService.create() - Creating " + flight.getFlightNumber());

        // Check to make sure the data fits with the parameters in the Flight model and passes validation.
        validator.validateFlight(flight);

        // Write the flight to the database.
        return crud.create(flight);
    }

    /**
     * <p>Updates an existing Flight object in the application database with the provided Flight object.<p/>
     *
     * <p>Validates the data in the provided Flight object using a FlightValidator object.<p/>
     *
     * @param flight The Flight object to be passed as an update to the application database
     * @return The Flight object that has been successfully updated in the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Flight update(Flight flight) throws Exception {
        log.info("FlightService.update() - Updating " + flight.getFlightNumber());

        // Check to make sure the data fits with the parameters in the Flight model and passes validation.
        validator.validateFlight(flight);

        /*try {
            //Removed temporarily due to non-existing AreaService
            Area area = areaService.getAreaById(Integer.parseInt(flight.getPhoneNumber().substring(1, 4)));
            flight.setState(area.getState());
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatusInfo() == Response.Status.NOT_FOUND) {
                throw new InvalidAreaCodeException("The area code provided does not exist", e);
            } else {
                throw e;
            }
        }*/

        // Either update the flight or add it if it can't be found.
        return crud.update(flight);
    }

    /**
     * <p>Deletes the provided Flight object from the application database if found there.<p/>
     *
     * @param flight The Flight object to be removed from the application database
     * @return The Flight object that has been successfully removed from the application database; or null
     * @throws Exception
     */
    Flight delete(Flight flight) throws Exception {
        log.info("delete() - Deleting " + flight.toString());

        Flight deletedFlight = null;

        if (flight.getId() != null) {
            deletedFlight = crud.delete(flight);
        } else {
            log.info("delete() - No ID was found so can't Delete.");
        }

        return deletedFlight;
    }
}
