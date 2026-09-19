package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.report.FinancialSummary;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialReportServiceTest {
    private AccountRepository accountRepo;
    private UserRepository userRepo;
    private FinancialReportService reportService;
    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        accountRepo = new AccountRepository();
        userRepo = new UserRepository();
        reportService = new FinancialReportService(accountRepo, userRepo);

        customer = new Customer("Hossein", "Rezayi", "09127778899", "1234567890", "Pass@1234");
        customer.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(customer);

        account = new Account("77001", customer.getPhoneNumber(), new CreditCard("6037770011112222"));
        accountRepo.save(account);
        customer.setAccount(account);
    }

    @Test
    void testCalculateSummaryAccurateCashFlow() {
        // 1. Inflow via Charge (100,000)
        Transaction chargeTx = new Transaction("TRX-101", TransactionType.CHARGE, 100000.0, 0.0,
                "FARIBANK", account.getAccountNumber(), "Deposit", Calendar.now());
        account.credit(100000.0, chargeTx);

        // 2. Outflow via Transfer (30,000 + 300 fee)
        Transaction debitTx = new Transaction("TRX-102", TransactionType.TRANSFER, 30000.0, 300.0,
                account.getAccountNumber(), "88002", "Transfer Out", Calendar.now());
        account.debit(30300.0, debitTx);

        // 3. Inflow via Incoming Transfer (20,000)
        Transaction creditTx = new Transaction("TRX-103", TransactionType.TRANSFER, 20000.0, 0.0,
                "88002", account.getAccountNumber(), "Transfer In", Calendar.now());
        account.credit(20000.0, creditTx);

        FinancialSummary summary = reportService.calculateSummary(customer.getPhoneNumber(), null, null);
        assertEquals(120000.0, summary.getTotalInflow(), 0.001);
        assertEquals(30000.0, summary.getTotalOutflow(), 0.001);
        assertEquals(300.0, summary.getTotalFees(), 0.001);
        assertEquals(89700.0, summary.getNetCashFlow(), 0.001); // 120,000 - 30,000 - 300
        assertEquals(3, summary.getRecordCount());
    }

    @Test
    void testFilterTransactionsByDateInterval() {
        Instant now = Calendar.now();
        Instant past = now.minus(5, ChronoUnit.DAYS);
        Instant future = now.plus(5, ChronoUnit.DAYS);

        Transaction oldTx = new Transaction("TRX-201", TransactionType.CHARGE, 50000.0, 0.0,
                "BANK", account.getAccountNumber(), "Old Charge", past);
        Transaction newTx = new Transaction("TRX-202", TransactionType.CHARGE, 70000.0, 0.0,
                "BANK", account.getAccountNumber(), "Recent Charge", now);
        account.credit(50000.0, oldTx);
        account.credit(70000.0, newTx);

        // Filter for transactions from 1 day ago to future -> only newTx matches
        List<Transaction> filtered = reportService.filterTransactions(
                customer.getPhoneNumber(), now.minus(1, ChronoUnit.DAYS), future, null);
        assertEquals(1, filtered.size());
        assertEquals("TRX-202", filtered.get(0).getTrackingNumber());
    }

    @Test
    void testGenerateHtmlStatementContent() {
        Transaction chargeTx = new Transaction("TRX-301", TransactionType.CHARGE, 150000.0, 0.0,
                "FARIBANK", account.getAccountNumber(), "Initial Load", Calendar.now());
        account.credit(150000.0, chargeTx);

        String html = reportService.generateHtmlStatement(customer.getPhoneNumber(), null, null);
        assertTrue(html.contains("Faribank Account Statement"));
        assertTrue(html.contains("Hossein Rezayi"));
        assertTrue(html.contains("77001"));
        assertTrue(html.contains("TRX-301"));
        assertFalse(html.isBlank());
    }
}