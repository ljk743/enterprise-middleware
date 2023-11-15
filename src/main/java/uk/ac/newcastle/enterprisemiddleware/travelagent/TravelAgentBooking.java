package uk.ac.newcastle.enterprisemiddleware.travelagent;

import uk.ac.newcastle.enterprisemiddleware.customer.Customer;

import javax.persistence.*;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>This is a the Domain object. The Booking class represents how booking resources are represented in the application
 * database.</p>
 *
 * <p>The class also specifies how a bookings are retrieved from the database (with @NamedQueries), and acceptable values
 * for Booking fields (with @NotNull, @Pattern etc...)<p/>
 *
 * @author ljk
 */
/*
 * The @NamedQueries included here are for searching against the table that reflects this object.  This is the most efficient
 * form of query in JPA though is it more error-prone due to the syntax being in a String.  This makes it harder to debug.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = TravelAgentBooking.FIND_ALL, query = "SELECT b FROM TravelAgentBooking b ORDER BY b.flightId ASC"),
        @NamedQuery(name = TravelAgentBooking.FIND_BY_CUSTOMER_ID, query = "SELECT b FROM TravelAgentBooking b WHERE b.customerId = :customerId")
})
@XmlRootElement
@Table(name = "travel_agent_booking", uniqueConstraints = @UniqueConstraint(columnNames = {"flight_id", "order_dates"}))
public class TravelAgentBooking implements Serializable {
    public static final String FIND_ALL = "TravelAgentBooking.findAll";
    public static final String FIND_BY_CUSTOMER_ID = "TravelAgentBooking.findByCustomerId";
    /**
     * Default value included to remove warning. Remove or modify at will.
     **/
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "flight_id")
    private Long flightId;

    @NotNull
    @Column(name = "taxi_id")
    private Long taxiId;

    @NotNull
    @Column(name = "hotel_id")
    private Long hotelId;

    @NotNull
    @Column(name = "customer_id")
    private Long customerId;

    @NotNull
    @Future(message = "Order dates can not be in the past. Please choose one from the future")
    @Column(name = "order_dates")
    @Temporal(TemporalType.DATE)
    private Date orderDate;

    @ManyToOne
    @JoinColumn(name = "customer_id_union")
    private Customer customer;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public Long getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(Long taxiId) {
        this.taxiId = taxiId;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDates) {
        this.orderDate = orderDates;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TravelAgentBooking)) return false;
        TravelAgentBooking travelAgentBooking = (TravelAgentBooking) o;
        return flightId.equals(travelAgentBooking.flightId) && orderDate.equals(travelAgentBooking.orderDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(flightId + orderDate.hashCode());
    }
}

