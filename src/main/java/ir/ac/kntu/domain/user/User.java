package ir.ac.kntu.domain.user;

import ir.ac.kntu.exception.ValidationException;

/**
 * Abstract base user class for customers, support staff, and administrators.
 */
public abstract class User {
    private String firstName;
    private String lastName;
    private String password;
    private boolean blocked;

    public User(String firstName, String lastName, String password) {
        setFirstName(firstName);
        setLastName(lastName);
        setPassword(password);
        this.blocked = false;
    }

    public String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new ValidationException("First name cannot be empty.");
        }
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public final void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new ValidationException("Last name cannot be empty.");
        }
        this.lastName = lastName.trim();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPassword() {
        return password;
    }

    public final void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }
        this.password = password;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}