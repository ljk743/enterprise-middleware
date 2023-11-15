package uk.ac.newcastle.enterprisemiddleware.customer;

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
 * Controller Layer
 **/

/**
 * <p>This class produces a RESTful service exposing the functionality of {@link CustomerService}.</p>
 *
 * <p>The Path annotation defines this as a REST Web Service using JAX-RS.</p>
 *
 * <p>By placing the Consumes and Produces annotations at the class level the methods all default to JSON.  However, they
 * can be overridden by adding the Consumes or Produces annotations to the individual methods.</p>
 *
 * <p>It is Stateless to "inform the container that this RESTful web service should also be treated as an EJB and allow
 * transaction demarcation when accessing the database." - Antonio Goncalves</p>
 *
 * <p>The full path for accessing endpoints defined herein is: api/Customers/*</p>
 *
 * @author ljk
 * @see CustomerService
 * @see Response
 */
@Path("/customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerRestService {
    @Inject
    @Named("logger")
    Logger log;

    @Inject
    CustomerService service;

    /**
     * <p>Return all the customers.  They are sorted alphabetically by name.</p>
     *
     * <p>The url may optionally include query parameters specifying a customer's name</p>
     *
     * <p>Examples: <pre>GET api/customers?firstname=John</pre>, <pre>GET api/customers?firstname=John&lastname=Smith</pre></p>
     *
     * @return A Response containing a list of customers
     */
    @GET
    @Operation(summary = "Fetch all customers", description = "Returns a JSON array of all stored customer objects.")
    public Response retrieveAllCustomers(@QueryParam("firstname") String firstname, @QueryParam("lastname") String lastname) {
        //Create an empty collection to contain the intersection of Customers to be returned
        List<Customer> customers;

        if (firstname == null && lastname == null) {
            customers = service.findAllOrderedByName();
        } else if (lastname == null) {
            customers = service.findAllByFirstName(firstname);
        } else if (firstname == null) {
            customers = service.findAllByLastName(lastname);
        } else {
            customers = service.findAllByFirstName(firstname);
            customers.retainAll(service.findAllByLastName(lastname));
        }

        return Response.ok(customers).build();
    }

    /**
     * <p>Search for and return a customer identified by email address.<p/>
     *
     * <p>Path annotation includes very simple regex to differentiate between email addresses and Ids.
     * <strong>DO NOT</strong> attempt to use this regex to validate email addresses.</p>
     *
     * @param email The string parameter value provided as a customer's email
     * @return A Response containing a single customer
     */
    @GET
    @Cache
    @Path("/email/{email:.+[%40|@].+}")
    @Operation(
            summary = "Fetch a customer by Email",
            description = "Returns a JSON representation of the customer object with the provided email."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Customer found"),
            @APIResponse(responseCode = "404", description = "Customer with email not found")
    })
    public Response retrieveCustomersByEmail(
            @Parameter(description = "Email of Customer to be fetched", required = true)
            @PathParam("email")
            String email) {

        Customer customer;
        try {
            customer = service.findByEmail(email);
        } catch (NoResultException e) {
            // Verify that the customer exists. Return 404, if not present.
            throw new RestServiceException("No customer with the email " + email + " was found!", Response.Status.NOT_FOUND);
        }
        return Response.ok(customer).build();
    }

    /**
     * <p>Search for and return a customer identified by id.</p>
     *
     * @param id The long parameter value provided as a customer's id
     * @return A Response containing a single customer
     */
    @GET
    @Cache
    @Path("/{id:[0-9]+}")
    @Operation(
            summary = "Fetch a customer by id",
            description = "Returns a JSON representation of the customer object with the provided id."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Customer found"),
            @APIResponse(responseCode = "404", description = "Customer with id not found")
    })
    public Response retrieveCustomerById(
            @Parameter(description = "Id of customer to be fetched")
            @Schema(minimum = "0", required = true)
            @PathParam("id")
            long id) {

        Customer customer = service.findById(id);
        if (customer == null) {
            // Verify that the Customer exists. Return 404, if not present.
            throw new RestServiceException("No customer with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }
        log.info("findById " + id + ": found Customer = " + customer);

        return Response.ok(customer).build();
    }

    /**
     * <p>Creates a new Customer from the values provided. Performs validation and will return a JAX-RS response with
     * either 201 (Resource created) or with a map of fields, and related errors.</p>
     *
     * @param customer The Customer object, constructed automatically from JSON input, to be <i>created</i> via
     *                 {@link CustomerService#create(Customer)}
     * @return A Response indicating the outcome of the create operation
     */
    @SuppressWarnings("unused")
    @POST
    @Operation(description = "Add a new Customer to the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Customer created successfully."),
            @APIResponse(responseCode = "400", description = "Invalid customer supplied in request body"),
            @APIResponse(responseCode = "409", description = "Customer supplied in request body conflicts with an existing customer"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response createCustomer(
            @Parameter(description = "JSON representation of customer object to be added to the database", required = true)
            Customer customer) {

        if (customer == null) {
            throw new RestServiceException("Bad Request", Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder;

        try {
            // Clear the ID if accidentally set
            customer.setId(null);

            // Check the length of name.

            // Go add the new Customer.
            service.create(customer);

            // Create a "Resource Created" 201 Response and pass the Customer back in case it is needed.
            builder = Response.status(Response.Status.CREATED).entity(customer);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);

        } catch (UniqueEmailException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "That email is already used, please use a unique email");
            throw new RestServiceException("Bad Request", responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }
        /*catch (InvalidAreaCodeException e) {
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("area_code", "The telephone area code provided is not recognised, please provide another");
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, e);
        } */


        log.info("createCustomer completed. customer = " + customer);
        return builder.build();
    }

    /**
     * <p>Updates the customer with the ID provided in the database. Performs validation, and will return a JAX-RS response
     * with either 200 (ok), or with a map of fields, and related errors.</p>
     *
     * @param id   The String parameter value provided as the id of the customer to be updated
     * @param customer The customer object, constructed automatically from JSON input, to be <i>updated</i> via
     *                 {@link CustomerService#update(Customer)}
     * @return A Response indicating the outcome of the create operation
     */
    @PUT
    @Path("/{id:[0-9]+}")
    @Operation(description = "Update customer's status in the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Status updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid customer supplied in request body"),
            @APIResponse(responseCode = "404", description = "Customer with id not found"),
            @APIResponse(responseCode = "409", description = "Customer details supplied in request body conflict with another existing Customer"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response updateCustomer(
            @Parameter(description = "Status of customer to be updated", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            Long id,
            @Parameter(description = "JSON representation of customer object to be updated in the database", required = true)
            Customer customer) {

        if (customer == null || customer.getId() == null) {
            throw new RestServiceException("Invalid customer supplied in request body", Response.Status.BAD_REQUEST);
        }
        /**
         if (customer.getId() != null && customer.getId() != id) {
         // The client attempted to update the read-only Id. This is not permitted.
         Map<String, String> responseObj = new HashMap<>();
         responseObj.put("id", "The Customer ID in the request body must match that of the Customer being updated");
         throw new RestServiceException("Customer details supplied in request body conflict with another Customer",
         responseObj, Response.Status.CONFLICT);
         }**/

        if (service.findById(customer.getId()) == null) {
            // Verify that the Customer exists. Return 404, if not present.
            throw new RestServiceException("No Customer with the id was found!", Response.Status.NOT_FOUND);
        }

        Response.ResponseBuilder builder;

        try {
            // Apply the changes the Customer.
            service.update(customer);

            // Create an OK Response and pass the Customer back in case it is needed.
            builder = Response.ok(customer);


        } catch (ConstraintViolationException ce) {
            //Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();

            for (ConstraintViolation<?> violation : ce.getConstraintViolations()) {
                responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, ce);
        } catch (UniqueEmailException e) {
            // Handle the unique constraint violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "That email is already used, please use a unique email");
            throw new RestServiceException("Customer details supplied in request body conflict with another Customer",
                    responseObj, Response.Status.CONFLICT, e);
        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }/*catch (InvalidAreaCodeException e) {
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("area_code", "The telephone area code provided is not recognised, please provide another");
            throw new RestServiceException("Bad Request", responseObj, Response.Status.BAD_REQUEST, e);
        }*/

        log.info("updateCustomer completed. Customer = " + customer);
        return builder.build();
    }

    /**
     * <p>Deletes a customer using the ID provided. If the ID is not present then nothing can be deleted.</p>
     *
     * <p>Will return a JAX-RS response with either 204 NO CONTENT or with a map of fields, and related errors.</p>
     *
     * @param id The Long parameter value provided as the id of the Customer to be deleted
     * @return A Response indicating the outcome of the delete operation
     */
    @DELETE
    @Path("/{id:[0-9]+}")
    @Operation(description = "Delete a Customer from the database")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "The customer has been successfully deleted"),
            @APIResponse(responseCode = "400", description = "Invalid Customer id supplied"),
            @APIResponse(responseCode = "404", description = "Customer with id not found"),
            @APIResponse(responseCode = "500", description = "An unexpected error occurred whilst processing the request")
    })
    @Transactional
    public Response deleteCustomer(
            @Parameter(description = "Id of Customer to be deleted", required = true)
            @Schema(minimum = "0")
            @PathParam("id")
            long id) {

        Response.ResponseBuilder builder;

        Customer customer = service.findById(id);
        if (customer == null) {
            // Verify that the customer exists. Return 404, if not present.
            throw new RestServiceException("No Customer with the id " + id + " was found!", Response.Status.NOT_FOUND);
        }

        try {
            service.delete(customer);

            builder = Response.noContent();

        } catch (Exception e) {
            // Handle generic exceptions
            throw new RestServiceException(e);
        }
        log.info("deleteCustomer completed. Customer = " + customer);
        return builder.build();
    }
}