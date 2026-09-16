package ir.ac.kntu.service;

import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.domain.user.SupportSection;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.UnauthorizedSectionAccessException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketServiceTest {
    private TicketRepository ticketRepo;
    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private AuthService authService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepo = new TicketRepository();
        userRepo = new UserRepository();
        accountRepo = new AccountRepository();
        authService = new AuthService(userRepo, accountRepo, ticketRepo);
        ticketService = new TicketService(ticketRepo, userRepo, authService);
    }

    @Test
    void testAutomaticKycTicketCreationOnCustomerRegistration() {
        Customer cust = new Customer("Ali", "Karimi", "09121112233", "1234567890", "Pass@1234");
        authService.registerCustomer(cust);

        List<Ticket> tickets = ticketRepo.findByPhone("09121112233");
        assertEquals(1, tickets.size());
        Ticket kycTicket = tickets.get(0);

        assertTrue(kycTicket.isKycRequest());
        assertEquals(TicketSection.AUTH, kycTicket.getSection());
        assertEquals(TicketStatus.REGISTERED, kycTicket.getStatus());
        assertTrue(kycTicket.getDescription().contains("Ali Karimi"));
    }

    @Test
    void testSupportCanOnlyViewAssignedSectionTickets() {
        SupportUser operator = new SupportUser("Reza", "Rad", "supp01", "Pass@1234");
        operator.setSections(Set.of(SupportSection.TRANSFER, SupportSection.SETTINGS));
        userRepo.saveSupport(operator);

        ticketService.createTicket("09120000001", TicketSection.TRANSFER, "Transfer issue");
        ticketService.createTicket("09120000002", TicketSection.SETTINGS, "Password issue");
        ticketService.createTicket("09120000003", TicketSection.AUTH, "KYC issue");

        List<Ticket> visibleTickets = ticketService.getTicketsForSupport("supp01");
        assertEquals(2, visibleTickets.size());
        for (Ticket tick : visibleTickets) {
            assertTrue(tick.getSection() == TicketSection.TRANSFER || tick.getSection() == TicketSection.SETTINGS);
            assertFalse(tick.getSection() == TicketSection.AUTH);
        }
    }

    @Test
    void testSupportUnauthorizedSectionFilterThrowsException() {
        SupportUser operator = new SupportUser("Reza", "Rad", "supp01", "Pass@1234");
        operator.setSections(Set.of(SupportSection.TRANSFER));
        userRepo.saveSupport(operator);

        assertThrows(UnauthorizedSectionAccessException.class, () ->
                ticketService.filterTicketsForSupport("supp01", null, TicketSection.AUTH, null));
    }

    @Test
    void testApproveKycTicketCompletesCustomerKycAndIssuesAccount() {
        Customer cust = new Customer("Sara", "Ahmadi", "09129998877", "9876543210", "Pass@1234");
        authService.registerCustomer(cust);
        assertEquals(KycStatus.PENDING, cust.getKycStatus());

        SupportUser authOperator = new SupportUser("John", "Doe", "auth_supp", "Pass@1234");
        authOperator.addSection(SupportSection.AUTH);
        userRepo.saveSupport(authOperator);

        Ticket kycTicket = ticketRepo.findByPhone("09129998877").get(0);
        ticketService.approveKycTicket("auth_supp", kycTicket.getTicketId());

        assertEquals(TicketStatus.APPROVED, kycTicket.getStatus());
        assertEquals(KycStatus.APPROVED, cust.getKycStatus());
        assertNotNull(cust.getAccount());
    }

    @Test
    void testRejectKycTicketClosesTicketAndRejectsCustomer() {
        Customer cust = new Customer("Navid", "Kiyani", "09125556677", "1122334455", "Pass@1234");
        authService.registerCustomer(cust);

        SupportUser authOperator = new SupportUser("John", "Doe", "auth_supp", "Pass@1234");
        authOperator.addSection(SupportSection.AUTH);
        userRepo.saveSupport(authOperator);

        Ticket kycTicket = ticketRepo.findByPhone("09125556677").get(0);
        ticketService.rejectKycTicket("auth_supp", kycTicket.getTicketId(), "National ID card is unreadable.");

        assertEquals(TicketStatus.CLOSED, kycTicket.getStatus());
        assertEquals("National ID card is unreadable.", kycTicket.getReply());
        assertEquals(KycStatus.REJECTED, cust.getKycStatus());
    }
}