package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.config.SystemSettings;
import ir.ac.kntu.domain.sim.ChargeReceipt;
import ir.ac.kntu.domain.sim.SimCard;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.SimRepository;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimServiceTest {
    private SimRepository simRepo;
    private AccountRepository accountRepo;
    private UserRepository userRepo;
    private SystemSettings settings;
    private SimService simService;

    @BeforeEach
    void setUp() {
        simRepo = new SimRepository();
        accountRepo = new AccountRepository();
        userRepo = new UserRepository();
        settings = new SystemSettings();
        simService = new SimService(simRepo, accountRepo, userRepo, settings);
    }

    @Test
    void testBuyChargeSuccessAndTaxCalculation() {
        Customer cust = new Customer("Ali", "Rezaei", "09121111111", "1234567890", "Pass@1234");
        cust.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(cust);

        Account account = new Account("10001", "09121111111", new CreditCard("6037100011234"));
        account.deposit(50000.0);
        accountRepo.save(account);

        ChargeReceipt receipt = simService.buyCharge("09121111111", "09122222222", 10000.0);

        assertNotNull(receipt);
        assertEquals(10000.0, receipt.getPureAmount());
        assertEquals(900.0, receipt.getTaxAmount()); // 9% default tax
        assertEquals(10900.0, receipt.getTotalAmount());

        assertEquals(39100.0, account.getBalance());
        assertEquals(10000.0, simService.getSimBalance("09122222222"));
        assertEquals(TransactionType.SIM_CHARGE, account.getTransactions().get(0).getType());
    }

    @Test
    void testRechargeUnregisteredSimAndPersistBalance() {
        Customer cust = new Customer("Sara", "Rad", "09123333333", "9876543210", "Pass@1234");
        cust.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(cust);

        Account account = new Account("10002", "09123333333", new CreditCard("6037100021234"));
        account.deposit(30000.0);
        accountRepo.save(account);

        simService.buyCharge("09123333333", "09129990011", 15000.0);
        assertEquals(15000.0, simService.getSimBalance("09129990011"));

        Customer newOwner = new Customer("Omid", "Kavi", "09129990011", "1122334455", "Pass@1234");
        userRepo.saveCustomer(newOwner);

        assertEquals(15000.0, simService.getSimBalance(newOwner.getPhoneNumber()));
    }

    @Test
    void testInsufficientBalanceThrowsException() {
        Customer cust = new Customer("Reza", "Far", "09124444444", "5544332211", "Pass@1234");
        cust.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(cust);

        Account account = new Account("10003", "09124444444", new CreditCard("6037100031234"));
        account.deposit(5000.0);
        accountRepo.save(account);

        assertThrows(InsufficientFundsException.class, () ->
                simService.buyCharge("09124444444", "09124444444", 10000.0));
    }

    @Test
    void testInvalidTargetPhoneThrowsValidationException() {
        Customer cust = new Customer("Mina", "Alavi", "09125555555", "9988776655", "Pass@1234");
        cust.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(cust);

        Account account = new Account("10004", "09125555555", new CreditCard("6037100041234"));
        account.deposit(20000.0);
        accountRepo.save(account);

        assertThrows(ValidationException.class, () ->
                simService.buyCharge("09125555555", "02188776655", 5000.0));
    }

    @Test
    void testNegativeOrZeroAmountThrowsValidationException() {
        assertThrows(ValidationException.class, () ->
                simService.buyCharge("09121111111", "09122222222", 0.0));
        assertThrows(ValidationException.class, () ->
                simService.buyCharge("09121111111", "09122222222", -500.0));
    }

    @Test
    void testSimCardDomainMutations() {
        SimCard card = new SimCard("09127778899", 5000.0);
        assertEquals("09127778899", card.getPhoneNumber());
        assertEquals(5000.0, card.getBalance());

        card.recharge(2500.0);
        assertEquals(7500.0, card.getBalance());

        assertThrows(ValidationException.class, () -> card.recharge(-100.0));
        assertThrows(ValidationException.class, () -> new SimCard("invalid_num"));
        assertThrows(ValidationException.class, () -> new SimCard("09121112233", -10.0));
    }
}