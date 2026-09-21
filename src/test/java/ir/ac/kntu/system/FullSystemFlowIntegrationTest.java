package ir.ac.kntu.system;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.TransferReceipt;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.fund.RemainingFund;
import ir.ac.kntu.domain.report.FinancialSummary;
import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;
import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.service.InterestSchedulerService;
import ir.ac.kntu.ui.BankServices;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullSystemFlowIntegrationTest {
    private BankServices services;

    @TempDir
    Path tempFolder;

    @BeforeEach
    void setUp() {
        services = new BankServices();
    }

    @Test
    void testAdminAndCreatorHierarchyModel() {
        AdminUser rootAdmin = new AdminUser("Root", "Admin", "root_admin", "Admin@123", null);
        AdminUser subAdmin = new AdminUser("Sub", "Admin", "sub_admin", "Admin@456", "root_admin");

        assertEquals("root_admin", subAdmin.getCreatorAdmin());
        assertNull(rootAdmin.getCreatorAdmin());
    }

    @Test
    void testEndToEndCustomerLifecycleAndBankingServices() throws IOException {
        // 1. Customer registration & KYC Ticket creation
        Customer customer = new Customer("Fariborz", "Farid", "09129998877", "0012345678", "Pass@1234");
        services.getAuthService().registerCustomer(customer);
        assertEquals(KycStatus.PENDING, customer.getKycStatus());

        List<Ticket> kycTickets = services.getTicketService().filterTickets(
                TicketStatus.REGISTERED, TicketSection.AUTH, customer.getPhoneNumber());
        assertFalse(kycTickets.isEmpty());

        // 2. KYC Approval & Automatic Account Provisioning
        services.getAuthService().approveKyc(customer.getPhoneNumber());
        assertEquals(KycStatus.APPROVED, customer.getKycStatus());
        assertNotNull(customer.getAccount());

        Account custAccount = customer.getAccount();
        custAccount.deposit(5000000.0);
        assertEquals(5000000.0, custAccount.getBalance(), 0.001);

        // 3. Register counterparty recipient
        Customer recipient = new Customer("Reza", "Rezayi", "09128887766", "0098765432", "Pass@1234");
        services.getAuthService().registerCustomer(recipient);
        services.getAuthService().approveKyc(recipient.getPhoneNumber());
        assertNotNull(recipient.getAccount());
        Account recipAccount = recipient.getAccount();

        // 4. Multi-channel transfers
        TransferReceipt fariReceipt = services.getTransferService().transferFariToFari(
                customer.getPhoneNumber(), recipAccount.getAccountNumber(), 500000.0);
        assertNotNull(fariReceipt);
        assertEquals(0.0, fariReceipt.getFee(), 0.001);

        TransferReceipt cardReceipt = services.getTransferService().transferCardToCard(
                customer.getPhoneNumber(), recipAccount.getCreditCard().getCardNumber(), 50000.0);
        assertNotNull(cardReceipt);
        assertEquals(300.0, cardReceipt.getFee(), 0.001);

        TransferReceipt payaReceipt = services.getTransferService().transferPaya(
                customer.getPhoneNumber(), recipAccount.getAccountNumber(), 200000.0);
        assertNotNull(payaReceipt);
        assertEquals(2000.0, payaReceipt.getFee(), 0.001);

        int payaCleared = services.getAdminBatchService().settlePayaQueue(Calendar.now());
        assertTrue(payaCleared >= 1);

        // 5. Capital investment funds
        RemainingFund remFund = services.getFundService().openRemainingFund(
                customer.getPhoneNumber(), 100000.0);
        assertNotNull(remFund);

        BonusFund bonusFund = services.getFundService().openBonusFund(
                customer.getPhoneNumber(), 1000000.0, 30, 0.12);
        assertNotNull(bonusFund);
        assertFalse(bonusFund.isInterestPaid());

        // 6. Automatic background interest payout via Concurrency scheduler
        Instant future = Calendar.now().plus(35, ChronoUnit.DAYS);
        InterestSchedulerService scheduler = new InterestSchedulerService(
                services.getFundService(), () -> future);
        int yieldedFunds = scheduler.runCycle();
        assertTrue(yieldedFunds >= 1);
        assertTrue(bonusFund.isInterestPaid());

        // 7. HTML Statement analytics
        FinancialSummary summary = services.getReportService().calculateSummary(
                customer.getPhoneNumber(), null, null);
        assertNotNull(summary);
        assertTrue(summary.getTotalOutflow() > 0);

        String htmlReport = services.getReportService().generateHtmlStatement(
                customer.getPhoneNumber(), null, null);
        assertNotNull(htmlReport);
        assertTrue(htmlReport.contains("Faribank Account Statement"));
        assertTrue(htmlReport.contains("Fariborz Farid"));

        // 8. JSON Persistence (Full Export & Import)
        Path stateFile = tempFolder.resolve("system_backup.json");
        services.getStorageService().exportToJsonFile(stateFile.toString());
        assertTrue(stateFile.toFile().exists());

        BankServices freshServices = new BankServices();
        freshServices.getStorageService().importFromJsonFile(stateFile.toString());
        Account restoredAcc = freshServices.getAccountService().findAccountByNumber(custAccount.getAccountNumber());
        assertNotNull(restoredAcc);
        assertEquals(custAccount.getBalance(), restoredAcc.getBalance(), 0.001);

        // 9. Support ticket reply workflow
        Ticket supportTicket = services.getTicketService().createTicket(
                customer.getPhoneNumber(), TicketSection.TRANSFER, "Transfer issue inquiry");
        assertNotNull(supportTicket);
        services.getTicketService().replyTicket(
                supportTicket.getTicketId(), "Issue resolved", TicketStatus.CLOSED);
        assertEquals(TicketStatus.CLOSED, supportTicket.getStatus());
        assertEquals("Issue resolved", supportTicket.getSupportReply());
    }
}