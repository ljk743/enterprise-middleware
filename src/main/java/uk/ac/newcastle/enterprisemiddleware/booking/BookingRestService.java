package uk.ac.newcastle.enterprisemiddleware.booking;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.Cache;
import uk.ac.newcastle.enterprisemiddleware.customer.CustomerService;
import uk.ac.newcastle.enterprisemiddleware.flight.FlightService;
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
 * <p>This class produces a RESTful service exposing the functionality of {@link BookingService}.</p>
 *
 * <p>The Path annotation defines this as a REST Web Service using JAX-RS.</p>
 *
 * <p>By placing the Consumes and Produces annotations at the class level the methods all default to JSON.  However, they
 * can be overriden by adding the Consumes or Produces annotations to the individual methods.</p>
 *
 * <p>It is Stateless to "inform the container that this RESTful web service should also be treated as an EJB and allow
 * transaction demarcation when accessing the database." - Antonio Goncalves</p>
 *
 * <p>The full path for accessing endpoints defined herein is: api/bookings/*</p>
 *
 * @author Joshua Wilson
 * @see BookingService
 * @see Response
 */
@Path("/bookings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingRestService {
    @Inject
    @Named("logger")
    Logger log;

    @Inject
    BookingService service;

    @Inject
    CustomerService customerService;
    @Inject
    FlightService flightService;

    /**
     * <p>Return all the Bookings.  They are sorted alphabetically by name.</p>
     *
     * <p>The url may optionally include query parameters specifying a Booking's name</p>
     *
     * <p>Examples: <pre>GET api/bookings?firstname=John</pre>, <pre>GET api/bookings?firstname=John&lastname=Smith</pre></p>
     *
     * @return A Response containing a list of Bookings
     */
    @GET
    @Operation(summary = "Fetch all Bookings", description = "Returns a JSON array of all stored Booking objects.")
    @APIResponses(value = {
            @APIResponse(responseCode = "400", description = "Invalid parameters in request body"),
    })
    public Response retrieveAllBookings(@QueryParam("flightid") Long flightId, @QueryParam("orderdate") String orderDate) {
        //Create an empty collection to contain the intersection of Bookings to be returned
        List<Booking> bookings;

        if (flightId == null && orderDate == null) {
            bookings = service.findAllOrdered();
        } else if (orderDate == null || flightId == null) {
            throw new RestServiceException("Bad Request", Response.Status.BAD_REQUEST);
        } else {
            bookings = service.findAllByFlightId(flightId);
            bookings.retainAll(service.findAllByOrderDate(orderDate));
        }

        return Response.ok(bookings).build();
    }

    /**
     * <p>Search for and return a Booking identified by email address.<p/>
     *
     * <p>Path annotation includes very simple regex to differentiate between email addresses and Ids.
     * <strong>DO NOT</strong> attempt to use this regex to validate email addresses.</p>
     *
     * @param customerId The Long parameter value provided as a Booking's customer id
     * @return A Response containing a single Booking
     */
    @GET
    @Cache
    @Path("/{customerid:[0-9]+}")
    @Operation(
            summary = "Fetch a Booking by customer id",
            description = "Returns a JSON representation of the Booking object with the provided customer id."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Booking found"),
            @APIResponse(responseCode = "404", description = "Booking with email not found")
    })
    public Response retrieveBookingsByCustomerId(
            @Parameter(description = "Booking number of Booking to be fetched", required = true)
            @PathParam("customerid")
            Long customerId) {

        Booking booking;
        try {
            booking = service.findByCustomerId(customerId);
        } catch (NoResultException e) {
            // Verify that the booking exists. Return 404, if not present.
            throw new RestServiceException("No Booking with the booking number " + customerId.toString() + " was found!", Response.Status.NOT_FOUND);
        }
        return Response.ok(booking).build();
    }

    /**
     * <p>Search for and return a Booking identified by id.</p>
     *
     * @param id The long parameter value provided as a Booking's id
     * @return A Response containing a single Booking
     */
    @GET
    @Cache
    @Path("/{id:[0-9]+}")
    @Operation(
            summary = "Fetch a Booking by id",
            description = "Returns a JSON representation of the Booking object with the provided id."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Booking found"),
            @APIResponse(responseCode = "404", description = "Booking with id not found")
    })
    public Response retrieveBookingById(
            @Parameter(description = "Id of Booking to be fetched")
            @Schema(minimum = "0", required = true)
            @PathParam("id")
            long id) {

        Booking booking = service.findById(id);
        if (booking == null) {
            // Verify that the booking exists. Return 404, if not present.
            throw new RestServiceException("No Booking with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }
        log.info("findById " + id + ": found Booking = " + booking);

        return Response.ok(booking).build();
    }

    /**
     * <p>Creates a new booking from the values provided. Performs validation and will return a JAX-RS response with
     * either 201 (Resource created) or with a map of fields, and related errors.</p>
     *
     * @param booking The Booking object, constructed automatically from JSON input, to be <i>created</i> via
     *                {@link BookingService#create(Booking)}
     * @return A Response indicating the outcome of the create operation
     */
    @SuppressWarnings("unused")
    @POST
    @Operation(description = "Add a new Booking to the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Booking created successfully."),
            @APIResponse(responseCode = "400", description = "Invalid Booking supplied in request body"),
            @APIResponse(responseCode = "409", description = "Booking supplied in request body conflicts with an existing Booking"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response createBooking(
            @Parameter(description = "JSON representation of Booking object to be added to the database", required = true)
            Booking booking) {

        if (booking == null) {
            throw new RestServiceException("Bad Request,parameters null", Response.Status.BAD_REQUEST);
        }
        if (customerService.findById(booking.getCustomerId()) == null) {
            throw new RestServiceException("Bad Request, customer not found !", Response.Status.BAD_REQUEST);
        }
        if (flightService.findById(booking.getFlightId()) == null) {
            throw new RestServiceException("Bad Request, flight not found !", Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder;

        try {
            // Clear the ID if accidentally set
            booking.setId(null);

            // Go add the new Booking.
            service.create(booking);

            // Create a "Resource Created" 201 Response and pass the booking back in case it is needed.

            builder = Response.status(Response.Status.CREATED).entity(booking);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);

        } catch (UniqueIdException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "That email is already used, please use a unique email");
            throw new RestServiceException("Bad Request", responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }

        log.info("createBooking completed. Booking = " + booking);
        return builder.build();
    }

    /**
     * <p>Updates the booking with the ID provided in the database. Performs validation, and will return a JAX-RS response
     * with either 200 (ok), or with a map of fields, and related errors.</p>
     *
     * @param booking The Booking object, constructed automatically from JSON input, to be <i>updated</i> via
     *                {@link BookingService#update(Booking)}
     * @param id      The long parameter value provided as the id of the Booking to be updated
     * @return A Response indicating the outcome of the create operation
     */
    @PUT
    @Path("/{id:[0-9]+}")
    @Operation(description = "Update a Booking in the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Booking updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid Booking supplied in request body"),
            @APIResponse(responseCode = "404", description = "Booking with id not found"),
            @APIResponse(responseCode = "409", description = "Booking details supplied in request body conflict with another existing Booking"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response updateBooking(
            @Parameter(description = "Id of Booking to be updated", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            long id,
            @Parameter(description = "JSON representation of Booking object to be updated in the database", required = true)
            Booking booking) {

        if (booking == null || booking.getId() == null) {
            throw new RestServiceException("Invalid Booking supplied in request body", Response.Status.BAD_REQUEST);
        }

        if (booking.getId() != null && booking.getId() != id) {
            // The client attempted to update the read-only Id. This is not permitted.
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("id", "The Booking ID in the request body must match that of the Booking being updated");
            throw new RestServiceException("Booking details supplied in request body conflict with another Booking",
                    responseObj, Response.Status.CONFLICT);
        }

        if (service.findById(booking.getId()) == null) {
            // Verify that the booking exists. Return 404, if not present.
            throw new RestServiceException("No Booking with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }

        Response.ResponseBuilder builder;

        try {
            // Apply the changes the Booking.
            service.update(booking);

            // Create an OK Response and pass the booking back in case it is needed.
            builder = Response.ok(booking);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);
        } catch (UniqueIdException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "That email is already used, please use a unique email");
            throw new RestServiceException("Booking details supplied in request body conflict with another Booking",
                    responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }

        log.info("updateBooking completed. Booking = " + booking);
        return builder.build();
    }

    /**
     * <p>Deletes a booking using the ID provided. If the ID is not present then nothing can be deleted.</p>
     *
     * <p>Will return a JAX-RS response with either 204 NO CONTENT or with a map of fields, and related errors.</p>
     *
     * @param id The Long parameter value provided as the id of the Booking to be deleted
     * @return A Response indicating the outcome of the delete operation
     */
    @DELETE
    @Path("/{id:[0-9]+}")
    @Operation(description = "Delete a Booking from the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "The booking has been successfully deleted"),
            @APIResponse(responseCode = "400", description = "Invalid Booking id supplied"),
            @APIResponse(responseCode = "404", description = "Booking with id not found"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response deleteBooking(
            @Parameter(description = "Id of Booking to be deleted", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            long id) {

        Response.ResponseBuilder builder;

        Booking booking = service.findById(id);
        if (booking == null) {
            // Verify that the booking exists. Return 404, if not present.
            throw new RestServiceException("No Booking with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }

        try {
            service.delete(booking);

            builder = Response.noContent();

        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }
        log.info("deleteBooking completed. Booking = " + booking);
        return builder.build();
    }
}