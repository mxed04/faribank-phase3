package ir.ac.kntu.domain.user;

import ir.ac.kntu.exception.ValidationException;

/**
 * System administrator with system governance and user management privileges.
 */
public class AdminUser extends User {
    private final String username;
    private final String creatorAdmin;

    public AdminUser(String firstName, String lastName, String username,
                     String password, String creatorAdmin) {
        super(firstName, lastName, password);
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Admin username cannot be empty.");
        }
        this.username = username.trim();
        this.creatorAdmin = creatorAdmin != null ? creatorAdmin.trim() : null;
    }

    public String getUsername() {
        return username;
    }

    public String getCreatorAdmin() {
        return creatorAdmin;
    }
}