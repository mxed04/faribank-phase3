package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.fund.FundType;
import ir.ac.kntu.domain.fund.RemainingFund;
import ir.ac.kntu.domain.fund.SavingsFund;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundServiceTest {
    private FundRepository fundRepo;
    private AccountRepository accountRepo;
    private UserRepository userRepo;
    private FundService fundService;
    private Customer customer;
    private Account account;

    @BeforeEach
    void setUp() {
        fundRepo = new FundRepository();
        accountRepo = new AccountRepository();
        userRepo = new UserRepository();
        fundService = new FundService(fundRepo, accountRepo, userRepo);

        customer = new Customer("Reza", "Pashaei", "09123456789", "1122334455", "Pass@1234");
        customer.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(customer);

        account = new Account("10099", customer.getPhoneNumber(), new CreditCard("603710009999"));
        account.deposit(200000.0);
        customer.setAccount(account);
        accountRepo.save(account);
    }

    @Test
    void testRemainingRoundUpFormulaCalculation() {
        // Nearest power to 12,340 is 10,000 (diff 2,340) -> 75% of 2,340 = 1,755
        assertEquals(1755.0, RemainingFund.computeRoundUp(12340.0), 0.001);

        // Nearest power to 9,200 is 10,000 (diff 800) -> 75% of 800 = 600
        assertEquals(600.0, RemainingFund.computeRoundUp(9200.0), 0.001);

        // Exact power of 10 -> diff 0 -> 0.0
        assertEquals(0.0, RemainingFund.computeRoundUp(10000.0), 0.001);
    }

    @Test
    void testOpenSavingsFundAndWithdrawal() {
        SavingsFund fund = fundService.openSavingsFund("09123456789", 50000.0);
        assertNotNull(fund);
        assertEquals(50000.0, fund.getBalance());
        assertEquals(150000.0, account.getBalance());

        fundService.withdrawFromFund(fund.getFundId(), 20000.0);
        assertEquals(30000.0, fund.getBalance());
        assertEquals(170000.0, account.getBalance());
    }

    @Test
    void testAutoSaveRemainingDeductsAndDeposits() {
        fundService.openRemainingFund("09123456789", 10000.0);
        assertEquals(190000.0, account.getBalance());

        // Spend 12,340 -> round-up savings is 1,755
        double saved = fundService.autoSaveRemaining("09123456789", 12340.0);
        assertEquals(1755.0, saved, 0.001);

        RemainingFund fund = (RemainingFund) fundRepo.findByOwnerAndType("09123456789", FundType.REMAINING).orElseThrow();
        assertEquals(11755.0, fund.getBalance(), 0.001);
        assertEquals(188245.0, account.getBalance(), 0.001);
    }

    @Test
    void testBonusFundMaturityAndPrematureWithdrawalProtection() {
        // 30 days term with 15% fixed interest
        BonusFund bonus = fundService.openBonusFund("09123456789", 100000.0, 30, 0.15);
        assertEquals(100000.0, bonus.getBalance());

        // Premature withdrawal must fail
        assertThrows(ValidationException.class, () -> fundService.withdrawFromFund(bonus.getFundId(), 10000.0));

        // Before maturity, profit payout is 0
        int count = fundService.payAllMaturedInterests(Calendar.now());
        assertEquals(0, count);
        assertFalse(bonus.isInterestPaid());

        // Advance 31 days into future -> mature and claim 15,000 profit
        Instant future = Calendar.now().plus(31, ChronoUnit.DAYS);
        count = fundService.payAllMaturedInterests(future);
        assertEquals(1, count);
        assertTrue(bonus.isInterestPaid());
        assertEquals(115000.0, bonus.getBalance(), 0.001);
    }
}