package ir.ac.kntu.service;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.AuthenticationException;
import ir.ac.kntu.exception.UserAlreadyExistsException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        accountRepo = new AccountRepository();
        authService = new AuthService(userRepo, accountRepo);
    }

    @Test
    void testCustomerRegistrationAndDuplicateRejections() {
        Customer cust1 = new Customer("Ali", "Rezaei", "09121111111", "1234567890", "Pass@1234");
        authService.registerCustomer(cust1);

        Customer duplicatePhone = new Customer("Sara", "Rad", "09121111111", "9876543210", "Pass@1234");
        assertThrows(UserAlreadyExistsException.class, () -> authService.registerCustomer(duplicatePhone));

        Customer duplicateNatId = new Customer("Sara", "Rad", "09122222222", "1234567890", "Pass@1234");
        assertThrows(UserAlreadyExistsException.class, () -> authService.registerCustomer(duplicateNatId));
    }

    @Test
    void testCustomerAuthenticationFlow() {
        Customer cust = new Customer("Ali", "Rezaei", "09121111111", "1234567890", "Pass@1234");
        authService.registerCustomer(cust);

        Customer authenticated = authService.authenticateCustomer("09121111111", "Pass@1234");
        assertNotNull(authenticated);

        assertThrows(AuthenticationException.class, () ->
                authService.authenticateCustomer("09121111111", "Wrong@Pass"));
        assertThrows(AuthenticationException.class, () ->
                authService.authenticateCustomer("09129999999", "Pass@1234"));

        cust.setBlocked(true);
        assertThrows(AuthenticationException.class, () ->
                authService.authenticateCustomer("09121111111", "Pass@1234"));
    }

    @Test
    void testDefaultAdminAuthentication() {
        AdminUser rootAdmin = authService.authenticateAdmin("admin", "Admin@1234");
        assertNotNull(rootAdmin);
        assertEquals("admin", rootAdmin.getUsername());

        assertThrows(AuthenticationException.class, () ->
                authService.authenticateAdmin("admin", "WrongPass@1"));
    }

    @Test
    void testSupportUserAuthentication() {
        SupportUser operator = new SupportUser("John", "Doe", "supp01", "Str0ng@Pass");
        userRepo.saveSupport(operator);

        SupportUser authenticated = authService.authenticateSupport("supp01", "Str0ng@Pass");
        assertNotNull(authenticated);
        assertEquals("supp01", authenticated.getUsername());

        assertThrows(AuthenticationException.class, () ->
                authService.authenticateSupport("supp01", "Invalid@Pass"));

        operator.setBlocked(true);
        assertThrows(AuthenticationException.class, () ->
                authService.authenticateSupport("supp01", "Str0ng@Pass"));
    }

    @Test
    void testKycApprovalAndAccountIssuance() {
        Customer cust = new Customer("Reza", "Karimi", "09123333333", "1122334455", "Pass@1234");
        authService.registerCustomer(cust);
        assertEquals(KycStatus.PENDING, cust.getKycStatus());

        authService.approveKyc("09123333333");
        assertEquals(KycStatus.APPROVED, cust.getKycStatus());
        assertNotNull(accountRepo.findByPhone("09123333333").orElse(null));
    }

    @Test
    void testKycRejectionFlow() {
        Customer cust = new Customer("Mina", "Alavi", "09124444444", "9988776655", "Pass@1234");
        authService.registerCustomer(cust);

        authService.rejectKyc("09124444444");
        assertEquals(KycStatus.REJECTED, cust.getKycStatus());
    }

    @Test
    void testUpdateCustomerKycData() {
        Customer cust = new Customer("Hassan", "Rad", "09125555555", "5544332211", "Pass@1234");
        authService.registerCustomer(cust);
        authService.approveKyc("09125555555");
        assertEquals(KycStatus.APPROVED, cust.getKycStatus());

        authService.updateCustomerKycData("09125555555", "Hossein", "Radfar", "9988112233");
        assertEquals("Hossein", cust.getFirstName());
        assertEquals("Radfar", cust.getLastName());
        assertEquals("9988112233", cust.getNationalCode());
        assertEquals(KycStatus.PENDING, cust.getKycStatus());
    }
}