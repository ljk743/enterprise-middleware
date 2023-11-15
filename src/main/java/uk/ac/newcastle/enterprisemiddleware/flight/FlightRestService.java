package uk.ac.newcastle.enterprisemiddleware.flight;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.Cache;
import uk.ac.newcastle.enterprisemiddleware.util.RestServiceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * <p>This class produces a RESTful service exposing the functionality of {@link FlightService}.</p>
 *
 * <p>The Path annotation defines this as a REST Web Service using JAX-RS.</p>
 *
 * <p>By placing the Consumes and Produces annotations at the class level the methods all default to JSON.  However, they
 * can be overriden by adding the Consumes or Produces annotations to the individual methods.</p>
 *
 * <p>It is Stateless to "inform the container that this RESTful web service should also be treated as an EJB and allow
 * transaction demarcation when accessing the database." - Antonio Goncalves</p>
 *
 * <p>The full path for accessing endpoints defined herein is: api/flights/*</p>
 *
 * @author Joshua Wilson
 * @see FlightService
 * @see Response
 */
@Path("/flights")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FlightRestService {
    @Inject
    @Named("logger")
    Logger log;

    @Inject
    FlightService service;

    /**
     * <p>Return all the Flights.  They are sorted alphabetically by name.</p>
     *
     * <p>The url may optionally include query parameters specifying a Flight's name</p>
     *
     * <p>Examples: <pre>GET api/flights?firstname=John</pre>, <pre>GET api/flights?firstname=John&lastname=Smith</pre></p>
     *
     * @return A Response containing a list of Flights
     */
    @GET
    @Operation(summary = "Fetch all Flights", description = "Returns a JSON array of all stored Flight objects.")
    public Response retrieveAllFlights(@QueryParam("departure") String departure, @QueryParam("destination") String destination) {
        //Create an empty collection to contain the intersection of Flights to be returned
        List<Flight> flights;

        if (departure == null && destination == null) {
            flights = service.findAllOrdered();
        } else if (destination == null) {
            flights = service.findAllByDeparture(departure);
        } else if (departure == null) {
            flights = service.findAllByDestination(destination);
        } else {
            flights = service.findAllByDeparture(departure);
            flights.retainAll(service.findAllByDestination(destination));
        }

        return Response.ok(flights).build();
    }

    /**
     * <p>Search for and return a Flight identified by flight number address.<p/>
     *
     * <p>Path annotation includes very simple regex to differentiate between flight number addresses and Ids.
     * <strong>DO NOT</strong> attempt to use this regex to validate flight number addresses.</p>
     *
     * @param flightNumber The string parameter value provided as a Flight's flight number
     * @return A Response containing a single Flight
     */
    @GET
    @Cache
    @Path("/flightnumber/{flightnumber:[A-Z0-9]{5}$}")
    @Operation(
            summary = "Fetch a Flight by Flight Number",
            description = "Returns a JSON representation of the Flight object with the provided flight number."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Flight found"),
            @APIResponse(responseCode = "404", description = "Flight with flight number not found")
    })
    public Response retrieveFlightsByFlightNumber(
            @Parameter(description = "Flight number of Flight to be fetched", required = true)
            @PathParam("flightnumber")
            String flightNumber) {

        Flight flight;
        try {
            flight = service.findByFlightNumber(flightNumber);
        } catch (NoResultException e) {
            // Verify that the flight exists. Return 404, if not present.
            throw new RestServiceException("No Flight with the flight number " + flightNumber + " was found!", Response.Status.NOT_FOUND);
        }
        return Response.ok(flight).build();
    }

    /**
     * <p>Search for and return a Flight identified by id.</p>
     *
     * @param id The long parameter value provided as a Flight's id
     * @return A Response containing a single Flight
     */
    @GET
    @Cache
    @Path("/{id:[0-9]+}")
    @Operation(
            summary = "Fetch a Flight by id",
            description = "Returns a JSON representation of the Flight object with the provided id."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Flight found"),
            @APIResponse(responseCode = "404", description = "Flight with id not found")
    })
    public Response retrieveFlightById(
            @Parameter(description = "Id of Flight to be fetched")
            @Schema(minimum = "0", required = true)
            @PathParam("id")
            long id) {

        Flight flight = service.findById(id);
        if (flight == null) {
            // Verify that the flight exists. Return 404, if not present.
            throw new RestServiceException("No Flight with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }
        log.info("findById " + id + ": found Flight = " + flight);

        return Response.ok(flight).build();
    }

    /**
     * <p>Creates a new flight from the values provided. Performs validation and will return a JAX-RS response with
     * either 201 (Resource created) or with a map of fields, and related errors.</p>
     *
     * @param flight The Flight object, constructed automatically from JSON input, to be <i>created</i> via
     *               {@link FlightService#create(Flight)}
     * @return A Response indicating the outcome of the create operation
     */
    @SuppressWarnings("unused")
    @POST
    @Operation(description = "Add a new Flight to the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Flight created successfully."),
            @APIResponse(responseCode = "400", description = "Invalid Flight supplied in request body"),
            @APIResponse(responseCode = "409", description = "Flight supplied in request body conflicts with an existing Flight"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response createFlight(
            @Parameter(description = "JSON representation of Flight object to be added to the database", required = true)
            Flight flight) {

        if (flight == null) {
            throw new RestServiceException("Bad Request", Response.Status.BAD_REQUEST);
        }
        if (flight.getDestination().equals(flight.getDeparture())) {
            throw new RestServiceException("Bad Request", Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder;


        try {
            // Clear the ID if accidentally set
            flight.setId(null);

            // Go add the new Flight.
            service.create(flight);

            // Create a "Resource Created" 201 Response and pass the flight back in case it is needed.
            builder = Response.status(Response.Status.CREATED).entity(flight);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);

        } catch (UniqueFlightNumberException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("flight number", "That flight number is already used, please use a unique number");
            throw new RestServiceException("Bad Request", responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }

        log.info("createFlight completed. Flight = " + flight);
        return builder.build();
    }

    /**
     * <p>Updates the flight with the ID provided in the database. Performs validation, and will return a JAX-RS response
     * with either 200 (ok), or with a map of fields, and related errors.</p>
     *
     * @param flight The Flight object, constructed automatically from JSON input, to be <i>updated</i> via
     *               {@link FlightService#update(Flight)}
     * @param id     The long parameter value provided as the id of the Flight to be updated
     * @return A Response indicating the outcome of the create operation
     */
    @PUT
    @Path("/{id:[0-9]+}")
    @Operation(description = "Update a Flight in the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Flight updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid Flight supplied in request body"),
            @APIResponse(responseCode = "404", description = "Flight with id not found"),
            @APIResponse(responseCode = "409", description = "Flight details supplied in request body conflict with another existing Flight"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response updateFlight(
            @Parameter(description = "Id of Flight to be updated", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            long id,
            @Parameter(description = "JSON representation of Flight object to be updated in the database", required = true)
            Flight flight) {

        if (flight == null || flight.getId() == null) {
            throw new RestServiceException("Invalid Flight supplied in request body", Response.Status.BAD_REQUEST);
        }

        if (flight.getId() != null && flight.getId() != id) {
            // The client attempted to update the read-only Id. This is not permitted.
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("id", "The Flight ID in the request body must match that of the Flight being updated");
            throw new RestServiceException("Flight details supplied in request body conflict with another Flight",
                    responseObj, Response.Status.CONFLICT);
        }

        if (service.findById(flight.getId()) == null) {
            // Verify that the flight exists. Return 404, if not present.
            throw new RestServiceException("No Flight with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }

        if (flight.getDestination().equals(flight.getDeparture())) {
            throw new RestServiceException("Bad Request", Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder;

        try {
            // Apply the changes the Flight.
            service.update(flight);

            // Create an OK Response and pass the flight back in case it is needed.
            builder = Response.ok(flight);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);
        } catch (UniqueFlightNumberException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("flight number", "That flight number is already used, please use a unique flight number");
            throw new RestServiceException("Flight details supplied in request body conflict with another Flight",
                    responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }

        log.info("updateFlight completed. Flight = " + flight);
        return builder.build();
    }

    /**
     * <p>Deletes a flight using the ID provided. If the ID is not present then nothing can be deleted.</p>
     *
     * <p>Will return a JAX-RS response with either 204 NO CONTENT or with a map of fields, and related errors.</p>
     *
     * @param id The Long parameter value provided as the id of the Flight to be deleted
     * @return A Response indicating the outcome of the delete operation
     */
    @DELETE
    @Path("/{id:[0-9]+}")
    @Operation(description = "Delete a Flight from the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "The flight has been successfully deleted"),
            @APIResponse(responseCode = "400", description = "Invalid Flight supplied"),
            @APIResponse(responseCode = "404", description = "Flight with id not found"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response deleteFlight(
            @Parameter(description = "Id of Flight to be deleted", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            long id) {

        Response.ResponseBuilder builder;

        Flight flight = service.findById(id);
        if (flight == null) {
            // Verify that the flight exists. Return 404, if not present.
            throw new RestServiceException("No Flight with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }

        try {
            service.delete(flight);

            builder = Response.noContent();

        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }
        log.info("deleteFlight completed. Flight = " + flight);
        return builder.build();
    }
}