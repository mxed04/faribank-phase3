package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.contact.Contact;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferConcurrencyTest {
    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private ContactRepository contactRepo;
    private TransferService transferService;

    private Customer alice;
    private Customer bob;
    private Account accAlice;
    private Account accBob;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        accountRepo = new AccountRepository();
        contactRepo = new ContactRepository();
        transferService = new TransferService(accountRepo, userRepo, contactRepo);

        alice = new Customer("Alice", "Smith", "09121110001", "1000000001", "Pass@1234");
        alice.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(alice);

        bob = new Customer("Bob", "Jones", "09121110002", "1000000002", "Pass@1234");
        bob.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(bob);

        accAlice = new Account("10001", alice.getPhoneNumber(), new CreditCard("603710001111"));
        accAlice.deposit(500000.0);
        alice.setAccount(accAlice);
        accountRepo.save(accAlice);

        accBob = new Account("10002", bob.getPhoneNumber(), new CreditCard("603710002222"));
        accBob.deposit(500000.0);
        bob.setAccount(accBob);
        accountRepo.save(accBob);

        contactRepo.saveContact(alice.getPhoneNumber(), new Contact("Bob", "Jones", bob.getPhoneNumber()));
        contactRepo.saveContact(bob.getPhoneNumber(), new Contact("Alice", "Smith", alice.getPhoneNumber()));
    }

    @Test
    void testConcurrentBidirectionalTransfersWithoutDeadlock() throws InterruptedException {
        int threadCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean aliceToBob = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (aliceToBob) {
                        transferService.transferByAccount(alice.getPhoneNumber(), accBob.getAccountNumber(), 1000.0);
                    } else {
                        transferService.transferByAccount(bob.getPhoneNumber(), accAlice.getAccountNumber(), 1000.0);
                    }
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // Ignored for concurrent stress check
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completedInTime = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completedInTime, "Concurrent execution deadlocked or timed out.");
        assertEquals(threadCount, successCount.get());

        // 20 outgoing transfers (1000 + 5 fee) and 20 incoming transfers (1000)
        // Net balance: 500,000 - (20 * 1005) + (20 * 1000) = 499,900.0
        assertEquals(499900.0, accAlice.getBalance(), 0.001);
        assertEquals(499900.0, accBob.getBalance(), 0.001);
    }

    @Test
    void testConcurrentDepositsAndWithdrawalsConsistency() throws InterruptedException {
        int operations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(operations * 2);

        for (int i = 0; i < operations; i++) {
            executor.submit(() -> {
                try {
                    accAlice.deposit(200.0);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    accAlice.withdraw(200.0);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        assertEquals(500000.0, accAlice.getBalance(), 0.001);
    }
}