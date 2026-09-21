package ir.ac.kntu.service;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.SupportSection;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.InvalidHierarchyOperationException;
import ir.ac.kntu.exception.UserAlreadyExistsException;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest {
    private UserRepository userRepo;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        adminService = new AdminService(userRepo);
    }

    @Test
    void testCreateAdminAndSupportSuccess() {
        AdminUser adminB = new AdminUser("Admin", "Two", "admin2", "Pass@1234", "admin");
        adminService.createAdmin("admin", adminB);
        assertTrue(userRepo.findAdminByUsername("admin2").isPresent());

        SupportUser supp = new SupportUser("Supp", "One", "supp1", "Pass@1234");
        adminService.createSupport("admin", supp);
        assertTrue(userRepo.findSupportByUsername("supp1").isPresent());

        adminService.assignSupportSections("admin", "supp1", Set.of(SupportSection.AUTH, SupportSection.FUNDS));
        SupportUser loaded = userRepo.findSupportByUsername("supp1").get();
        assertTrue(loaded.hasSection(SupportSection.AUTH));
        assertTrue(loaded.hasSection(SupportSection.FUNDS));
        assertFalse(loaded.hasSection(SupportSection.REPORT));
    }

    @Test
    void testDuplicateUsernameFails() {
        AdminUser duplicate = new AdminUser("Clone", "Admin", "admin", "Pass@1234", "admin");
        assertThrows(UserAlreadyExistsException.class, () -> adminService.createAdmin("admin", duplicate));
    }

    @Test
    void testAdminCannotBlockSelf() {
        assertThrows(InvalidHierarchyOperationException.class, () -> adminService.blockUser("admin", "admin"));
    }

    @Test
    void testAdminCannotBlockParentOrHigherAncestor() {
        AdminUser admin2 = new AdminUser("Sub", "Admin2", "admin2", "Pass@1234", "admin");
        adminService.createAdmin("admin", admin2);

        AdminUser admin3 = new AdminUser("Sub", "Admin3", "admin3", "Pass@1234", "admin2");
        adminService.createAdmin("admin2", admin3);

        assertThrows(InvalidHierarchyOperationException.class, () -> adminService.blockUser("admin3", "admin2"));
        assertThrows(InvalidHierarchyOperationException.class, () -> adminService.blockUser("admin3", "admin"));

        adminService.blockUser("admin", "admin2");
        assertTrue(userRepo.findAdminByUsername("admin2").get().isBlocked());

        adminService.unblockUser("admin", "admin2");
        assertFalse(userRepo.findAdminByUsername("admin2").get().isBlocked());
    }
}