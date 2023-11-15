package uk.ac.newcastle.enterprisemiddleware.customer;

/**
 * Entity and Mappers
 **/

import uk.ac.newcastle.enterprisemiddleware.booking.Booking;

import java.util.List;

/**
 * <p>This is a the Domain object. The Customer class represents how customer resources are represented in the application
 * database.</p>
 *
 * <p>The class also specifies how a customers are retrieved from the database (with @NamedQueries), and acceptable values
 * for Customer fields (with @NotNull, @Pattern etc...)<p/>
 *
 * @author ljk
 */
public class CustomerDTO {
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private List<Booking> orders;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<Booking> getOrders() {
        return orders;
    }

    public void setOrders(List<Booking> orders) {
        this.orders = orders;
    }
}

