package uk.ac.newcastle.enterprisemiddleware.flight;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>This is a the Domain object. The Flight class represents how flight resources are represented in the application
 * database.</p>
 *
 * <p>The class also specifies how a flights are retrieved from the database (with @NamedQueries), and acceptable values
 * for Flight fields (with @NotNull, @Pattern etc...)<p/>
 *
 * @author ljk
 */
/*
 * The @NamedQueries included here are for searching against the table that reflects this object.  This is the most efficient
 * form of query in JPA though is it more error-prone due to the syntax being in a String.  This makes it harder to debug.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = Flight.FIND_ALL, query = "SELECT f FROM Flight f ORDER BY f.flightNumber ASC"),
        @NamedQuery(name = Flight.FIND_BY_FLIGHT_NUMBER, query = "SELECT f FROM Flight f WHERE f.flightNumber = :flightNumber")
})
@XmlRootElement
@Table(name = "flight", uniqueConstraints = @UniqueConstraint(columnNames = "flight_number"))
public class Flight implements Serializable {
    public static final String FIND_ALL = "Flight.findAll";
    public static final String FIND_BY_FLIGHT_NUMBER = "Flight.findByFlightNumber";
    /**
     * Default value included to remove warning. Remove or modify at will.
     **/
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @NotNull
    @Size(min = 5, max = 5)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Please use a flight number without specials and low cases")
    @Column(name = "flight_number")
    private String flightNumber;

    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]+$", message = "Please set a departure without low cases, numbers or specials")
    @Column(name = "departure")
    private String departure;

    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[A-Z]+$", message = "Please set a destination without low cases, numbers or specials")
    @Column(name = "destination")
    private String destination;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getDeparture() {
        return departure;
    }

    public void setDeparture(String departure) {
        this.departure = departure;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flight)) return false;
        Flight flight = (Flight) o;
        return flightNumber.equals(flight.flightNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(flightNumber);
    }
}

