package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.account.TransferReceipt;
import ir.ac.kntu.domain.contact.Contact;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.AccountNotFoundException;
import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe service executing fund transfers with ordered locking to prevent deadlocks.
 */
public class TransferService {
    private static final double TRANSFER_FEE_RATE = 0.005;
    private static final AtomicLong TRX_COUNTER = new AtomicLong(800001);

    private final AccountRepository accountRepo;
    private final UserRepository userRepo;
    private final ContactRepository contactRepo;

    public TransferService(AccountRepository accountRepo, UserRepository userRepo, ContactRepository contactRepo) {
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.contactRepo = Objects.requireNonNull(contactRepo, "Contact repo cannot be null.");
    }

    public TransferReceipt transferByAccount(String senderPhone, String targetAccNum, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly positive.");
        }

        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);

        Account destAcc = accountRepo.findByAccountNumber(targetAccNum)
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + targetAccNum));

        if (sourceAcc.getAccountNumber().equals(destAcc.getAccountNumber())) {
            throw new ValidationException("Self-transfers are not allowed.");
        }

        Customer destOwner = userRepo.findCustomerByPhone(destAcc.getOwnerPhoneNumber())
                .orElseThrow(() -> new ValidationException("Destination account owner not found."));

        sender.addRecentAccount(destAcc.getAccountNumber());
        return executeOrderedTransfer(sourceAcc, destAcc, amount, destOwner.getFullName());
    }

    public TransferReceipt transferByContact(String senderPhone, String targetPhone, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly positive.");
        }

        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);

        Customer recipient = getApprovedCustomer(targetPhone);
        Account destAcc = getCustomerAccount(recipient);

        if (sourceAcc.getAccountNumber().equals(destAcc.getAccountNumber())) {
            throw new ValidationException("Self-transfers are not allowed.");
        }

        if (!sender.isContactsEnabled() || !recipient.isContactsEnabled()) {
            throw new ValidationException("Contacts functionality is disabled.");
        }

        if (!contactRepo.isMutual(senderPhone, targetPhone)) {
            throw new ValidationException("Transfers require a mutual contact relationship.");
        }

        String recipientName = contactRepo.findContact(senderPhone, targetPhone)
                .map(Contact::getFullName)
                .orElse(recipient.getFullName());

        sender.addRecentAccount(destAcc.getAccountNumber());
        return executeOrderedTransfer(sourceAcc, destAcc, amount, recipientName);
    }

    private TransferReceipt executeOrderedTransfer(Account sourceAcc, Account destAcc,
                                                   double amount, String recipientName) {
        Account firstLock = getFirstLock(sourceAcc, destAcc);
        Account secondLock = firstLock == sourceAcc ? destAcc : sourceAcc;

        synchronized (firstLock) {
            synchronized (secondLock) {
                Transaction trxDebit = doAtomicTransfer(sourceAcc, destAcc, amount, recipientName);
                return new TransferReceipt(trxDebit);
            }
        }
    }

    private Transaction doAtomicTransfer(Account sourceAcc, Account destAcc,
                                         double amount, String recipientName) {
        double fee = amount * TRANSFER_FEE_RATE;
        double totalRequired = amount + fee;

        if (sourceAcc.getBalance() < totalRequired) {
            throw new InsufficientFundsException("Insufficient funds. Required: "
                    + totalRequired + ", Available: " + sourceAcc.getBalance());
        }

        String trxIdOut = "TRX-" + TRX_COUNTER.getAndIncrement();
        Transaction trxDebit = new Transaction(trxIdOut, TransactionType.TRANSFER, amount, fee,
                sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), recipientName, Calendar.now());
        sourceAcc.debit(totalRequired, trxDebit);

        String trxIdIn = "TRX-" + TRX_COUNTER.getAndIncrement();
        String senderName = getOwnerName(sourceAcc);
        Transaction trxCredit = new Transaction(trxIdIn, TransactionType.TRANSFER, amount, 0.0,
                sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), senderName, Calendar.now());
        destAcc.credit(amount, trxCredit);

        return trxDebit;
    }

    private Account getFirstLock(Account sourceAcc, Account destAcc) {
        if (sourceAcc.getAccountNumber().compareTo(destAcc.getAccountNumber()) < 0) {
            return sourceAcc;
        }
        return destAcc;
    }

    private String getOwnerName(Account account) {
        return userRepo.findCustomerByPhone(account.getOwnerPhoneNumber())
                .map(Customer::getFullName)
                .orElse("Sender");
    }

    private Customer getApprovedCustomer(String phone) {
        Customer cust = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        if (cust.getKycStatus() != KycStatus.APPROVED) {
            throw new ValidationException("Customer must be KYC-approved: " + phone);
        }
        return cust;
    }

    private Account getCustomerAccount(Customer customer) {
        Account account = customer.getAccount();
        if (account == null) {
            account = accountRepo.findByPhone(customer.getPhoneNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Sender bank account not found."));
            customer.setAccount(account);
        }
        return account;
    }
}