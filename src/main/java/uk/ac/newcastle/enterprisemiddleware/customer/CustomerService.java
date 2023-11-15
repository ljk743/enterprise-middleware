package uk.ac.newcastle.enterprisemiddleware.customer;


import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service Layer
 **/

/**
 * <p>This Service assumes the Control responsibility in the ECB pattern.</p>
 *
 * <p>The validation is done here so that it may be used by other Boundary Resources. Other Business Logic would go here
 * as well.</p>
 *
 * <p>There are no access modifiers on the methods, making them 'package' scope.  They should only be accessed by a
 * Boundary / Web Service class with public methods.</p>
 *
 * @author ljk
 * @see CustomerValidator
 * @see CustomerRepository
 */
@Dependent
public class CustomerService {

    @Inject
    @Named("logger")
    Logger log;

    @Inject
    CustomerValidator validator;

    @Inject
    CustomerRepository crud;

    //Removed temporarily due to non-existing AreaService
    //@RestClient
    //AreaService areaService;

    /**
     * <p>Returns a List of all persisted {@link Customer} objects, sorted alphabetically by last name.<p/>
     *
     * @return List of Customer objects
     */
    List<Customer> findAllOrderedByName() {
        return crud.findAllOrderedByName();
    }

    /**
     * <p>Returns a single Customer object, specified by a Long id.<p/>
     *
     * @param id The id field of the Customer to be returned
     * @return The Customer with the specified id
     */
    public Customer findById(Long id) {
        return crud.findById(id);
    }

    /**
     * <p>Returns a single Customer object, specified by a String email.</p>
     *
     * <p>If there is more than one Customer with the specified email, only the first encountered will be returned.<p/>
     *
     * @param email The email field of the Customer to be returned
     * @return The first Customer with the specified email
     */
    Customer findByEmail(String email) {
        return crud.findByEmail(email);
    }

    /**
     * <p>Returns a single Customer object, specified by a String firstName.<p/>
     *
     * @param firstName The firstName field of the Customer to be returned
     * @return The first Customer with the specified firstName
     */
    List<Customer> findAllByFirstName(String firstName) {
        return crud.findAllByFirstName(firstName);
    }

    /**
     * <p>Returns a single Customer object, specified by a String lastName.<p/>
     *
     * @param lastName The lastName field of the Customers to be returned
     * @return The Customers with the specified lastName
     */
    List<Customer> findAllByLastName(String lastName) {
        return crud.findAllByLastName(lastName);
    }

    /**
     * <p>Writes the provided Customer object to the application database.<p/>
     *
     * <p>Validates the data in the provided Customer object using a {@link CustomerValidator} object.<p/>
     *
     * @param customer The Customer object to be written to the database using a {@link CustomerRepository} object
     * @return The Customer object that has been successfully written to the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Customer create(Customer customer) throws Exception {
        log.info("CustomerService.create() - Creating " + customer.getFirstName() + " " + customer.getLastName());

        // Check to make sure the data fits with the parameters in the Customer model and passes validation.
        validator.validateCustomer(customer);


        //Create client service instance to make REST requests to upstream service
        /*try {
            //Removed temporarily due to non-existing AreaService
            Area area = areaService.getAreaById(Integer.parseInt(customer.getPhoneNumber().substring(1, 4)));
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatusInfo() == Response.Status.NOT_FOUND) {
                throw new InvalidAreaCodeException("The area code provided does not exist", e);
            } else {
                throw e;
            }
        }*/

        // Write the customer to the database.
        return crud.create(customer);
    }

    /**
     * <p>Updates an existing Customer object in the application database with the provided Customer object.<p/>
     *
     * <p>Validates the data in the provided Customer object using a CustomerValidator object.<p/>
     *
     * @param customer The Customer object to be passed as an update to the application database
     * @return The Customer object that has been successfully updated in the application database
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Customer update(Customer customer) throws Exception {
        log.info("CustomerService.update() - Updating " + customer.getFirstName() + " " + customer.getLastName());

        // Check to make sure the data fits with the parameters in the Customer model and passes validation.
        validator.validateCustomer(customer);

        /*try {
            //Removed temporarily due to non-existing AreaService
            Area area = areaService.getAreaById(Integer.parseInt(customer.getPhoneNumber().substring(1, 4)));
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatusInfo() == Response.Status.NOT_FOUND) {
                throw new InvalidAreaCodeException("The area code provided does not exist", e);
            } else {
                throw e;
            }
        }*/

        // Either update the customer or add it if it can't be found.
        return crud.update(customer);
    }

    Customer delete(Customer customer) throws Exception {
        log.info("delete() - Deleting " + customer.toString());

        Customer deletedCustomer = null;

        if (customer.getId() != null) {
            deletedCustomer = crud.delete(customer);
        } else {
            log.info("delete() - No ID was found so can't Delete.");
        }

        return deletedCustomer;
    }
}
