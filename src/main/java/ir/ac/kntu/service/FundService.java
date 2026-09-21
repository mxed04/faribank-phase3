package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.fund.Fund;
import ir.ac.kntu.domain.fund.FundType;
import ir.ac.kntu.domain.fund.RemainingFund;
import ir.ac.kntu.domain.fund.SavingsFund;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.AccountNotFoundException;
import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing fund provisioning, micro-savings, withdrawals, and interest payouts.
 */
public class FundService {
    private static final AtomicLong FUND_SEQ = new AtomicLong(1001);
    private static final AtomicLong TRX_SEQ = new AtomicLong(900001);

    private final FundRepository fundRepo;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;

    public FundService(FundRepository fundRepo, AccountRepository accountRepo, UserRepository userRepo) {
        this.fundRepo = Objects.requireNonNull(fundRepo, "Fund repository cannot be null.");
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repository cannot be null.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repository cannot be null.");
    }

    public SavingsFund openSavingsFund(String phone, double initialSum) {
        validateCustomerKyc(phone);
        debitAccountForFund(phone, initialSum, "Savings Fund Opening");
        String fundId = "SAV-" + FUND_SEQ.getAndIncrement();
        SavingsFund fund = new SavingsFund(fundId, phone, initialSum, Calendar.now());
        fundRepo.save(fund);
        return fund;
    }

    public RemainingFund openRemainingFund(String phone, double initialSum) {
        validateCustomerKyc(phone);
        if (fundRepo.findByOwnerAndType(phone, FundType.REMAINING).isPresent()) {
            throw new ValidationException("Customer already possesses an active Remaining Fund.");
        }
        debitAccountForFund(phone, initialSum, "Remaining Fund Opening");
        String fundId = "REM-" + FUND_SEQ.getAndIncrement();
        RemainingFund fund = new RemainingFund(fundId, phone, initialSum, Calendar.now());
        fundRepo.save(fund);
        return fund;
    }

    public BonusFund openBonusFund(String phone, double initialSum, int durationDays, double rate) {
        validateCustomerKyc(phone);
        if (durationDays <= 0) {
            throw new ValidationException("Term duration must be at least 1 day.");
        }
        debitAccountForFund(phone, initialSum, "Bonus Fund Investment");
        String fundId = "BON-" + FUND_SEQ.getAndIncrement();
        Instant maturity = Calendar.now().plus(durationDays, ChronoUnit.DAYS);
        BonusFund fund = new BonusFund(fundId, phone, initialSum, rate, maturity, Calendar.now());
        fundRepo.save(fund);
        return fund;
    }

    public void depositToFund(String fundId, double amount) {
        Fund fund = getFund(fundId);
        debitAccountForFund(fund.getOwnerPhone(), amount, "Fund Deposit " + fundId);
        fund.deposit(amount);
    }

    public void withdrawFromFund(String fundId, double amount) {
        Fund fund = getFund(fundId);
        fund.withdraw(amount);
        Account account = getAccount(fund.getOwnerPhone());
        String trxId = "TRX-" + TRX_SEQ.getAndIncrement();
        Transaction trx = new Transaction(trxId, TransactionType.TRANSFER, amount, 0.0,
                fundId, account.getAccountNumber(), "Fund Withdrawal", Calendar.now());
        account.credit(amount, trx);
    }

    public double autoSaveRemaining(String phone, double spentAmount) {
        double microSum = RemainingFund.computeRoundUp(spentAmount);
        if (microSum <= 0) {
            return 0.0;
        }
        var optFund = fundRepo.findByOwnerAndType(phone, FundType.REMAINING);
        if (optFund.isEmpty()) {
            return 0.0;
        }
        Account account = getAccount(phone);
        if (account.getBalance() < microSum) {
            return 0.0;
        }
        debitAccountForFund(phone, microSum, "Auto Micro-Savings Round-Up");
        optFund.get().deposit(microSum);
        return microSum;
    }

    public int payAllMaturedInterests(Instant currentInstant) {
        int count = 0;
        for (Fund fund : fundRepo.findAll()) {
            if (fund instanceof BonusFund bonus && bonus.hasMatured(currentInstant) && !bonus.isInterestPaid()) {
                bonus.claimInterest(currentInstant);
                count++;
            }
        }
        return count;
    }

    public Fund getFund(String fundId) {
        return fundRepo.findById(fundId)
                .orElseThrow(() -> new ValidationException("Fund not found: " + fundId));
    }

    public List<Fund> getCustomerFunds(String phone) {
        return fundRepo.findByOwnerPhone(phone);
    }

    private void validateCustomerKyc(String phone) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        if (customer.getKycStatus() != KycStatus.APPROVED) {
            throw new ValidationException("Customer is not KYC approved.");
        }
    }

    private void debitAccountForFund(String phone, double amount, String description) {
        if (amount <= 0) {
            return;
        }
        Account account = getAccount(phone);
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException("Insufficient funds for fund investment.");
        }
        String trxId = "TRX-" + TRX_SEQ.getAndIncrement();
        Transaction trx = new Transaction(trxId, TransactionType.TRANSFER, amount, 0.0,
                account.getAccountNumber(), "FUND", description, Calendar.now());
        account.debit(amount, trx);
    }

    private Account getAccount(String phone) {
        return accountRepo.findByPhone(phone)
                .orElseThrow(() -> new AccountNotFoundException("Active account not found for phone: " + phone));
    }
}