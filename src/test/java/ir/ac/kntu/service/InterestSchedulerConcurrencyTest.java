package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterestSchedulerConcurrencyTest {
    private AccountRepository accountRepo;
    private UserRepository userRepo;
    private FundRepository fundRepo;
    private FundService fundService;
    private InterestSchedulerService scheduler;

    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        accountRepo = new AccountRepository();
        userRepo = new UserRepository();
        fundRepo = new FundRepository();
        fundService = new FundService(fundRepo, accountRepo, userRepo);

        customer = new Customer("Sina", "Moradi", "09128889900", "0019988776", "Pass@1234");
        customer.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(customer);

        account = new Account("66001", customer.getPhoneNumber(), new CreditCard("6037660011112222"));
        account.deposit(200000.0);
        customer.setAccount(account);
        accountRepo.save(account);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    @Test
    void testBackgroundSchedulerExecutesInterestPayout() throws InterruptedException {
        BonusFund bonusFund = fundService.openBonusFund(customer.getPhoneNumber(), 100000.0, 30, 0.15);
        assertEquals(100000.0, bonusFund.getBalance(), 0.001);
        assertFalse(bonusFund.isInterestPaid());

        Instant futureTime = Calendar.now().plus(35, ChronoUnit.DAYS);
        CountDownLatch latch = new CountDownLatch(1);

        scheduler = new InterestSchedulerService(fundService, () -> {
            latch.countDown();
            return futureTime;
        });

        scheduler.start(10, 50);
        assertTrue(scheduler.isActive());

        boolean triggered = latch.await(2, TimeUnit.SECONDS);
        assertTrue(triggered);

        Thread.sleep(100);
        assertTrue(bonusFund.isInterestPaid());
        assertEquals(115000.0, bonusFund.getBalance(), 0.001);
        assertTrue(scheduler.getCycleCount() >= 1);

        scheduler.stop();
        assertFalse(scheduler.isActive());
    }

    @Test
    void testManualRunCycleExecution() {
        BonusFund bonusFund = fundService.openBonusFund(customer.getPhoneNumber(), 50000.0, 10, 0.20);
        Instant futureTime = Calendar.now().plus(12, ChronoUnit.DAYS);

        scheduler = new InterestSchedulerService(fundService, () -> futureTime);
        int payoutCount = scheduler.runCycle();

        assertEquals(1, payoutCount);
        assertTrue(bonusFund.isInterestPaid());
        assertEquals(60000.0, bonusFund.getBalance(), 0.001);
        assertEquals(1, scheduler.getCycleCount());
    }
}