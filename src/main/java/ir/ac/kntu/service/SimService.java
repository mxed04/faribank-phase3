package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.config.SystemSettings;
import ir.ac.kntu.domain.sim.ChargeReceipt;
import ir.ac.kntu.domain.sim.SimCard;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.AccountNotFoundException;
import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.SimRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing universal SIM card airtime recharges, tax calculations, and balance queries.
 */
public class SimService {
    private static final AtomicLong REC_COUNTER = new AtomicLong(1001);
    private static final AtomicLong TRX_COUNTER = new AtomicLong(700001);

    private final SimRepository simRepo;
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;
    private final SystemSettings settings;

    public SimService(SimRepository simRepo, AccountRepository accountRepo,
                      UserRepository userRepo, SystemSettings settings) {
        this.simRepo = Objects.requireNonNull(simRepo, "SIM repo cannot be null.");
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.settings = Objects.requireNonNull(settings, "System settings cannot be null.");
    }

    public ChargeReceipt buyCharge(String requesterPhone, String targetPhone, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Recharge amount must be strictly positive.");
        }
        if (targetPhone == null || !targetPhone.trim().matches("^09\\d{9}$")) {
            throw new ValidationException("Invalid target mobile phone number format.");
        }

        Customer customer = userRepo.findCustomerByPhone(requesterPhone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + requesterPhone));

        if (customer.getKycStatus() != KycStatus.APPROVED) {
            throw new ValidationException("Customer must be KYC-approved to buy SIM recharge.");
        }

        Account account = accountRepo.findByPhone(requesterPhone)
                .orElseThrow(() -> new AccountNotFoundException("Active account not found for customer."));

        double taxRate = settings.getChargeTaxRate();
        double taxAmount = amount * taxRate;
        double totalDeduction = amount + taxAmount;

        if (account.getBalance() < totalDeduction) {
            throw new InsufficientFundsException("Insufficient balance for charge and tax. Required: "
                    + totalDeduction + ", Available: " + account.getBalance());
        }

        String trxId = "TRX-" + TRX_COUNTER.getAndIncrement();
        Transaction trx = new Transaction(trxId, TransactionType.SIM_CHARGE, amount, taxAmount,
                account.getAccountNumber(), targetPhone.trim(), "SIM Recharge", Calendar.now());
        account.debit(totalDeduction, trx);

        SimCard targetSim = simRepo.getOrCreate(targetPhone.trim());
        targetSim.recharge(amount);

        String recId = "REC-" + REC_COUNTER.getAndIncrement();
        return new ChargeReceipt(recId, requesterPhone.trim(), targetPhone.trim(),
                amount, taxAmount, totalDeduction, Calendar.now());
    }

    public double getSimBalance(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.trim().matches("^09\\d{9}$")) {
            throw new ValidationException("Invalid phone number format.");
        }
        return simRepo.getOrCreate(phoneNumber.trim()).getBalance();
    }
}