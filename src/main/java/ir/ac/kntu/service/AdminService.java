package ir.ac.kntu.service;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.SupportSection;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.domain.user.User;
import ir.ac.kntu.exception.AuthenticationException;
import ir.ac.kntu.exception.InvalidHierarchyOperationException;
import ir.ac.kntu.exception.UserAlreadyExistsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Service orchestrating system administration, role creation, and hierarchy safety.
 */
public class AdminService {
    private final UserRepository userRepo;

    public AdminService(UserRepository userRepo) {
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
    }

    public void createAdmin(String requester, AdminUser newAdmin) {
        validateActiveAdmin(requester);
        String username = newAdmin.getUsername().toLowerCase();
        if (userRepo.findAnyUser(username).isPresent()) {
            throw new UserAlreadyExistsException("User identifier already registered: " + username);
        }
        userRepo.saveAdmin(newAdmin);
    }

    public void createSupport(String requester, SupportUser support) {
        validateActiveAdmin(requester);
        String username = support.getUsername().toLowerCase();
        if (userRepo.findAnyUser(username).isPresent()) {
            throw new UserAlreadyExistsException("User identifier already registered: " + username);
        }
        userRepo.saveSupport(support);
    }

    public void assignSupportSections(String requester, String targetUser, Set<SupportSection> sections) {
        validateActiveAdmin(requester);
        SupportUser support = userRepo.findSupportByUsername(targetUser)
                .orElseThrow(() -> new ValidationException("Support user not found: " + targetUser));
        support.setSections(sections);
    }

    public void blockUser(String requester, String targetUser) {
        validateActiveAdmin(requester);
        User user = userRepo.findAnyUser(targetUser)
                .orElseThrow(() -> new ValidationException("Target user not found: " + targetUser));

        if (user instanceof AdminUser targetAdmin) {
            validateHierarchyConstraint(requester, targetAdmin);
        }
        user.setBlocked(true);
    }

    public void unblockUser(String requester, String targetUser) {
        validateActiveAdmin(requester);
        User user = userRepo.findAnyUser(targetUser)
                .orElseThrow(() -> new ValidationException("Target user not found: " + targetUser));
        user.setBlocked(false);
    }

    public boolean isAncestor(String descendantUser, String ancestorUser) {
        Optional<AdminUser> optAdmin = userRepo.findAdminByUsername(descendantUser);
        if (optAdmin.isEmpty()) {
            return false;
        }
        String curr = optAdmin.get().getCreatorAdmin();
        while (curr != null) {
            if (curr.equalsIgnoreCase(ancestorUser)) {
                return true;
            }
            Optional<AdminUser> parent = userRepo.findAdminByUsername(curr);
            curr = parent.map(AdminUser::getCreatorAdmin).orElse(null);
        }
        return false;
    }

    private void validateActiveAdmin(String username) {
        AdminUser admin = userRepo.findAdminByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Admin not found or unauthorized: " + username));
        if (admin.isBlocked()) {
            throw new AuthenticationException("Admin account is blocked.");
        }
    }

    private void validateHierarchyConstraint(String requester, AdminUser targetAdmin) {
        if (requester.equalsIgnoreCase(targetAdmin.getUsername())) {
            throw new InvalidHierarchyOperationException("Admins cannot block their own account.");
        }
        if (isAncestor(requester, targetAdmin.getUsername())) {
            throw new InvalidHierarchyOperationException("Admins cannot block their creator or ancestors.");
        }
    }
}