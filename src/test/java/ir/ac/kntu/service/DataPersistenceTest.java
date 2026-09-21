package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.fund.SavingsFund;
import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPersistenceTest {
    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private FundRepository fundRepo;
    private TicketRepository ticketRepo;
    private JsonStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        accountRepo = new AccountRepository();
        fundRepo = new FundRepository();
        ticketRepo = new TicketRepository();
        storageService = new JsonStorageService(userRepo, accountRepo, fundRepo, ticketRepo);
    }

    @Test
    void testSaveAndRestoreSystemState() throws IOException {
        Customer customer = new Customer("Ali", "Alavi", "09121112233", "0011223344", "Pass@1234");
        customer.setKycStatus(KycStatus.APPROVED);
        customer.setBlocked(false);
        userRepo.saveCustomer(customer);

        Account account = new Account("55001", customer.getPhoneNumber(), new CreditCard("6037550011112222"));
        Transaction transaction = new Transaction("TRX-99", TransactionType.CHARGE, 150000.0, 0.0,
                "SYSTEM", account.getAccountNumber(), "Initial Load", Calendar.now());
        account.credit(150000.0, transaction);
        customer.setAccount(account);
        accountRepo.save(account);

        SavingsFund fund = new SavingsFund("FND-01", customer.getPhoneNumber(), 50000.0, Calendar.now());
        fundRepo.save(fund);

        Ticket ticket = new Ticket("TCK-01", customer.getPhoneNumber(), TicketSection.TRANSFER, "Test ticket", Calendar.now());
        ticketRepo.save(ticket);

        Path jsonFile = tempDir.resolve("faribank_state.json");
        storageService.exportToJsonFile(jsonFile.toString());
        assertTrue(jsonFile.toFile().exists());

        UserRepository freshUserRepo = new UserRepository();
        AccountRepository freshAccountRepo = new AccountRepository();
        FundRepository freshFundRepo = new FundRepository();
        TicketRepository freshTicketRepo = new TicketRepository();

        JsonStorageService freshStorage = new JsonStorageService(
                freshUserRepo, freshAccountRepo, freshFundRepo, freshTicketRepo);
        freshStorage.importFromJsonFile(jsonFile.toString());

        Optional<Customer> restoredCustomer = freshUserRepo.findCustomerByPhone("09121112233");
        assertTrue(restoredCustomer.isPresent());
        assertEquals("Ali", restoredCustomer.get().getFirstName());
        assertEquals("Alavi", restoredCustomer.get().getLastName());
        assertEquals(KycStatus.APPROVED, restoredCustomer.get().getKycStatus());

        Optional<Account> restoredAccount = freshAccountRepo.findByAccountNumber("55001");
        assertTrue(restoredAccount.isPresent());
        assertEquals(150000.0, restoredAccount.get().getBalance(), 0.001);

        assertEquals(1, freshFundRepo.findAll().size());
        assertEquals(50000.0, freshFundRepo.findAll().get(0).getBalance(), 0.001);

        assertEquals(1, freshTicketRepo.findAll().size());
        assertEquals("TCK-01", freshTicketRepo.findAll().get(0).getTicketId());
    }
}