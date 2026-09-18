package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.transfer.PayaRecord;
import ir.ac.kntu.domain.transfer.PayaStatus;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.PayaRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOperationsTest {
    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private PayaRepository payaRepo;
    private FundRepository fundRepo;
    private FundService fundService;
    private TransferService transferService;
    private AdminBatchService batchService;
    private AdminCustomerService adminCustomerService;

    private Customer alice;
    private Customer bob;
    private Account accAlice;
    private Account accBob;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        accountRepo = new AccountRepository();
        payaRepo = new PayaRepository();
        fundRepo = new FundRepository();

        fundService = new FundService(fundRepo, accountRepo, userRepo);
        transferService = new TransferService(accountRepo, userRepo, new ContactRepository());
        transferService.setFundService(fundService);
        transferService.setPayaRepository(payaRepo);

        batchService = new AdminBatchService(payaRepo, accountRepo, fundService);
        adminCustomerService = new AdminCustomerService(userRepo);

        alice = new Customer("Alice", "Miller", "09121110001", "1000000001", "Pass@1234");
        alice.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(alice);

        bob = new Customer("Bob", "Vance", "09121110002", "1000000002", "Pass@1234");
        bob.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(bob);

        accAlice = new Account("10001", alice.getPhoneNumber(), new CreditCard("6037100011110001"));
        accAlice.deposit(500000.0);
        alice.setAccount(accAlice);
        accountRepo.save(accAlice);

        accBob = new Account("10002", bob.getPhoneNumber(), new CreditCard("6037100022220002"));
        accBob.deposit(100000.0);
        bob.setAccount(accBob);
        accountRepo.save(accBob);
    }

    @Test
    void testAdminBatchSettlePayaQueue() {
        // Enqueue Paya transfer of 50,000 from Alice to Bob (Fee 2,000)
        transferService.transferPaya(alice.getPhoneNumber(), accBob.getAccountNumber(), 50000.0);
        assertEquals(448000.0, accAlice.getBalance(), 0.001);
        assertEquals(100000.0, accBob.getBalance(), 0.001); // Not credited yet

        List<PayaRecord> queued = payaRepo.findQueued();
        assertEquals(1, queued.size());

        // Batch execution settles the queue
        int processed = batchService.settlePayaQueue(Instant.now());
        assertEquals(1, processed);
        assertEquals(150000.0, accBob.getBalance(), 0.001);
        assertEquals(PayaStatus.PROCESSED, queued.get(0).getStatus());
        assertTrue(payaRepo.findQueued().isEmpty());
    }

    @Test
    void testAdminBatchDistributeFundProfits() {
        BonusFund bonus = fundService.openBonusFund(alice.getPhoneNumber(), 100000.0, 30, 0.20);
        assertEquals(100000.0, bonus.getBalance());

        // Advance 31 days -> distribute matured profits
        Instant future = Calendar.now().plus(31, ChronoUnit.DAYS);
        int profitCount = batchService.distributeFundProfits(future);
        assertEquals(1, profitCount);
        assertTrue(bonus.isInterestPaid());
        assertEquals(120000.0, bonus.getBalance(), 0.001);
    }

    @Test
    void testAdminCustomerSearchMultiFilters() {
        List<Customer> results = adminCustomerService.searchCustomers("Miller", KycStatus.APPROVED, false);
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getFirstName());

        List<Customer> nonExistent = adminCustomerService.searchCustomers("Unknown", null, null);
        assertTrue(nonExistent.isEmpty());
    }

    @Test
    void testAdminBlockCustomerAndTransferRejection() {
        adminCustomerService.setCustomerBlocked(alice.getPhoneNumber(), true);
        assertTrue(alice.isBlocked());

        // Blocked customer cannot initiate transfers
        assertThrows(ValidationException.class, () ->
                transferService.transferFariToFari(alice.getPhoneNumber(), accBob.getAccountNumber(), 5000.0));

        // Unblock customer
        adminCustomerService.setCustomerBlocked(alice.getPhoneNumber(), false);
        assertFalse(alice.isBlocked());
        transferService.transferFariToFari(alice.getPhoneNumber(), accBob.getAccountNumber(), 5000.0);
        assertEquals(495000.0, accAlice.getBalance(), 0.001);
    }

    @Test
    void testAdminUpdateCustomerProfile() {
        adminCustomerService.updateCustomerProfile(bob.getPhoneNumber(), "Robert", "Vance-Pharma");
        assertEquals("Robert", bob.getFirstName());
        assertEquals("Vance-Pharma", bob.getLastName());
        assertEquals("Robert Vance-Pharma", bob.getFullName());
    }
}