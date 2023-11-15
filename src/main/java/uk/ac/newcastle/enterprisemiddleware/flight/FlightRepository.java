package uk.ac.newcastle.enterprisemiddleware.flight;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>This is a Repository class and connects the Service/Control layer (see {@link FlightService} with the
 * Domain/Entity Object (see {@link Flight}).<p/>
 *
 * <p>There are no access modifiers on the methods making them 'package' scope.  They should only be accessed by a
 * Service/Control object.<p/>
 *
 * @author Joshua Wilson
 * @see Flight
 * @see EntityManager
 */
@RequestScoped
public class FlightRepository {

    @Inject
    @Named("logger")
    Logger log;

    @Inject
    EntityManager em;

    /**
     * <p>Returns a List of all persisted {@link Flight} objects, sorted alphabetically by last name.</p>
     *
     * @return List of Flight objects
     */
    List<Flight> findAllOrdered() {
        TypedQuery<Flight> query = em.createNamedQuery(Flight.FIND_ALL, Flight.class);
        return query.getResultList();
    }

    /**
     * <p>Returns a single Flight object, specified by a Long id.<p/>
     *
     * @param id The id field of the Flight to be returned
     * @return The Flight with the specified id
     */
    Flight findById(Long id) {
        return em.find(Flight.class, id);
    }

    /**
     * <p>Returns a single Flight object, specified by a String flight number.</p>
     *
     * <p>If there is more than one Flight with the specified flight number, only the first encountered will be returned.<p/>
     *
     * @param flightNumber The flight_number field of the Flight to be returned
     * @return The first Flight with the specified flight number
     */
    Flight findByFlightNumber(String flightNumber) {
        TypedQuery<Flight> query = em.createNamedQuery(Flight.FIND_BY_FLIGHT_NUMBER, Flight.class).setParameter("flightNumber", flightNumber);
        return query.getSingleResult();
    }

    /**
     * <p>Returns a list of Flight objects, specified by a String firstName.<p/>
     *
     * @param departure The firstName field of the Flights to be returned
     * @return The Flights with the specified firstName
     */
    List<Flight> findAllByDeparture(String departure) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Flight> criteria = cb.createQuery(Flight.class);
        Root<Flight> flight = criteria.from(Flight.class);
        // Swap criteria statements if you would like to try out type-safe criteria queries, a new feature in JPA 2.0.
        // criteria.select(flight).where(cb.equal(flight.get(Flight_.firstName), firstName));
        criteria.select(flight).where(cb.equal(flight.get("departure"), departure));
        return em.createQuery(criteria).getResultList();
    }

    /**
     * <p>Returns a single Flight object, specified by a String lastName.<p/>
     *
     * @param destination The lastName field of the Flights to be returned
     * @return The Flights with the specified lastName
     */
    List<Flight> findAllByDestination(String destination) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Flight> criteria = cb.createQuery(Flight.class);
        Root<Flight> flight = criteria.from(Flight.class);
        // Swap criteria statements if you would like to try out type-safe criteria queries, a new feature in JPA 2.0.
        // criteria.select(flight).where(cb.equal(flight.get(Flight_.lastName), lastName));
        criteria.select(flight).where(cb.equal(flight.get("destination"), destination));
        return em.createQuery(criteria).getResultList();
    }

    /**
     * <p>Persists the provided Flight object to the application database using the EntityManager.</p>
     *
     * <p>{@link EntityManager#persist(Object) persist(Object)} takes an entity instance, adds it to the
     * context and makes that instance managed (ie future updates to the entity will be tracked)</p>
     *
     * <p>persist(Object) will set the @GeneratedValue @Id for an object.</p>
     *
     * @param flight The Flight object to be persisted
     * @return The Flight object that has been persisted
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Flight create(Flight flight) throws Exception {
        log.info("FlightRepository.create() - Creating " + flight.getFlightNumber() + "  "
                + flight.getDeparture() + "  " + flight.getDestination());

        // Write the flight to the database.
        em.persist(flight);

        return flight;
    }

    /**
     * <p>Updates an existing Flight object in the application database with the provided Flight object.</p>
     *
     * <p>{@link EntityManager#merge(Object) merge(Object)} creates a new instance of your entity,
     * copies the state from the supplied entity, and makes the new copy managed. The instance you pass in will not be
     * managed (any changes you make will not be part of the transaction - unless you call merge again).</p>
     *
     * <p>merge(Object) however must have an object with the @Id already generated.</p>
     *
     * @param flight The Flight object to be merged with an existing Flight
     * @return The Flight that has been merged
     * @throws ConstraintViolationException, ValidationException, Exception
     */
    Flight update(Flight flight) throws Exception {
        log.info("FlightRepository.update() - Updating " + flight.getDeparture() + " " + flight.getDestination());

        // Either update the flight or add it if it can't be found.
        em.merge(flight);

        return flight;
    }

    /**
     * <p>Deletes the provided Flight object from the application database if found there</p>
     *
     * @param flight The Flight object to be removed from the application database
     * @return The Flight object that has been successfully removed from the application database; or null
     * @throws Exception
     */
    Flight delete(Flight flight) throws Exception {
        log.info("FlightRepository.delete() - Deleting " + flight.getFlightNumber());

        if (flight.getId() != null) {
            /*
             * The Hibernate session (aka EntityManager's persistent context) is closed and invalidated after the commit(),
             * because it is bound to a transaction. The object goes into a detached status. If you open a new persistent
             * context, the object isn't known as in a persistent state in this new context, so you have to merge it.
             *
             * Merge sees that the object has a primary key (id), so it knows it is not new and must hit the database
             * to reattach it.
             *
             * Note, there is NO remove method which would just take a primary key (id) and an entity class as argument.
             * You first need an object in a persistent state to be able to delete it.
             *
             * Therefore, we merge first and then we can remove it.
             */
            em.remove(em.merge(flight));

        } else {
            log.info("FlightRepository.delete() - No ID was found so can't Delete.");
        }

        return flight;
    }

}
