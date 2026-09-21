package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.account.TransferReceipt;
import ir.ac.kntu.domain.contact.Contact;
import ir.ac.kntu.domain.transfer.PayaRecord;
import ir.ac.kntu.domain.transfer.TransferChannel;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.AccountNotFoundException;
import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.PayaRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.util.Calendar;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-channel transfer switch enforcing channel-specific caps, fee schedules, and atomic transfers.
 */
public class TransferService {
    public static final double CARD_TO_CARD_MAX = 100000.0;
    public static final double POL_MAX = 5000000.0;
    public static final double PAYA_MAX = 5000000.0;
    public static final double FARI_TO_FARI_MAX = 8000000.0;

    private static final AtomicLong TRX_COUNTER = new AtomicLong(800001);
    private static final AtomicLong PAYA_COUNTER = new AtomicLong(70001);

    private final AccountRepository accountRepo;
    private final UserRepository userRepo;
    private final ContactRepository contactRepo;
    private FundService fundService;
    private PayaRepository payaRepo;

    public TransferService(AccountRepository accountRepo, UserRepository userRepo, ContactRepository contactRepo) {
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.contactRepo = Objects.requireNonNull(contactRepo, "Contact repo cannot be null.");
        this.payaRepo = new PayaRepository();
    }

    public void setFundService(FundService fundService) {
        this.fundService = fundService;
    }

    public void setPayaRepository(PayaRepository payaRepo) {
        if (payaRepo != null) {
            this.payaRepo = payaRepo;
        }
    }

    public TransferReceipt transferCardToCard(String senderPhone, String destCard, double amount) {
        if (amount > CARD_TO_CARD_MAX) {
            throw new ValidationException("Amount exceeds Card-to-Card limit of " + CARD_TO_CARD_MAX + " IRR.");
        }
        Account destAcc = accountRepo.findByCardNumber(destCard)
                .orElseThrow(() -> new AccountNotFoundException("Destination card not found: " + destCard));
        double fee = TransferChannel.CARD_TO_CARD.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferPol(String senderPhone, String targetAccNum, double amount) {
        if (amount > POL_MAX) {
            throw new ValidationException("Amount exceeds POL limit of " + POL_MAX + " IRR.");
        }
        Account destAcc = getAccountByNumber(targetAccNum);
        double fee = TransferChannel.POL.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferFariToFari(String senderPhone, String targetAccNum, double amount) {
        if (amount > FARI_TO_FARI_MAX) {
            throw new ValidationException("Amount exceeds Fari-to-Fari limit of " + FARI_TO_FARI_MAX + " IRR.");
        }
        Account destAcc = getAccountByNumber(targetAccNum);
        double fee = TransferChannel.FARI_TO_FARI.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferPaya(String senderPhone, String targetAccNum, double amount) {
        validatePayaAmount(amount);
        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);
        Account destAcc = getAccountByNumber(targetAccNum);

        double fee = TransferChannel.PAYA.calculateFee(amount);
        Transaction trxDebit = debitPayaAccount(sourceAcc, destAcc, amount, fee);

        enqueuePayaRecord(sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), amount, senderPhone);
        triggerMicroSavings(senderPhone, amount);
        sender.addRecentAccount(destAcc.getAccountNumber());
        return new TransferReceipt(trxDebit);
    }

    private void validatePayaAmount(double amount) {
        if (amount <= 0 || amount > PAYA_MAX) {
            throw new ValidationException("Invalid Paya amount: must be positive and <= " + PAYA_MAX);
        }
    }

    private Transaction debitPayaAccount(Account sourceAcc, Account destAcc, double amount, double fee) {
        double total = amount + fee;
        synchronized (sourceAcc) {
            if (sourceAcc.getBalance() < total) {
                throw new InsufficientFundsException("Insufficient balance for Paya transfer.");
            }
            String trxId = "TRX-" + TRX_COUNTER.getAndIncrement();
            String desc = "Paya Queued to " + destAcc.getAccountNumber();
            Transaction trx = new Transaction(trxId, TransactionType.TRANSFER, amount, fee,
                    sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), desc, Calendar.now());
            sourceAcc.debit(total, trx);
            return trx;
        }
    }

    private void enqueuePayaRecord(String srcNum, String dstNum, double amount, String senderPhone) {
        String payaId = "PAYA-" + PAYA_COUNTER.getAndIncrement();
        PayaRecord payaRec = new PayaRecord(payaId, srcNum, dstNum, amount);
        payaRec.setSenderPhone(senderPhone);
        payaRepo.save(payaRec);
    }

    public TransferReceipt transferByAccount(String senderPhone, String targetAccNum, double amount) {
        Account destAcc = getAccountByNumber(targetAccNum);
        double fee = amount * 0.005;
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferByContact(String senderPhone, String targetPhone, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly positive.");
        }
        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);
        Customer recipient = getApprovedCustomer(targetPhone);
        Account destAcc = getCustomerAccount(recipient);

        validateMutualContactTransfer(sender, recipient, sourceAcc, destAcc);

        double fee = amount * 0.005;
        sender.addRecentAccount(destAcc.getAccountNumber());
        TransferReceipt receipt = executeOrderedLock(sourceAcc, destAcc, amount, fee);
        triggerMicroSavings(senderPhone, amount);
        return receipt;
    }

    private void validateMutualContactTransfer(Customer sender, Customer recipient,
                                               Account sourceAcc, Account destAcc) {
        if (sourceAcc.getAccountNumber().equals(destAcc.getAccountNumber())) {
            throw new ValidationException("Self-transfers are not allowed.");
        }
        if (!sender.isContactsEnabled() || !recipient.isContactsEnabled()) {
            throw new ValidationException("Contacts functionality is disabled.");
        }
        if (!contactRepo.isMutual(sender.getPhoneNumber(), recipient.getPhoneNumber())) {
            throw new ValidationException("Transfers require a mutual contact relationship.");
        }
    }

    public PayaRepository getPayaRepository() {
        return payaRepo;
    }

    private TransferReceipt executeDirectTransfer(String senderPhone, Account destAcc,
                                                  double amount, double fee) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly positive.");
        }
        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);

        if (sourceAcc.getAccountNumber().equals(destAcc.getAccountNumber())) {
            throw new ValidationException("Self-transfers are not allowed.");
        }

        userRepo.findCustomerByPhone(destAcc.getOwnerPhoneNumber())
                .orElseThrow(() -> new ValidationException("Destination account owner not found."));

        sender.addRecentAccount(destAcc.getAccountNumber());
        TransferReceipt receipt = executeOrderedLock(sourceAcc, destAcc, amount, fee);
        triggerMicroSavings(senderPhone, amount);
        return receipt;
    }

    private TransferReceipt executeOrderedLock(Account sourceAcc, Account destAcc,
                                               double amount, double fee) {
        Account firstLock = sourceAcc.getAccountNumber().compareTo(destAcc.getAccountNumber()) < 0
                ? sourceAcc : destAcc;
        Account secondLock = firstLock == sourceAcc ? destAcc : sourceAcc;

        synchronized (firstLock) {
            synchronized (secondLock) {
                return doTransferInsideLock(sourceAcc, destAcc, amount, fee);
            }
        }
    }

    private TransferReceipt doTransferInsideLock(Account sourceAcc, Account destAcc,
                                                 double amount, double fee) {
        double total = amount + fee;
        if (sourceAcc.getBalance() < total) {
            throw new InsufficientFundsException("Insufficient balance.");
        }

        String destName = getRecipientName(sourceAcc.getOwnerPhoneNumber(), destAcc);
        String trxOut = "TRX-" + TRX_COUNTER.getAndIncrement();
        Transaction debit = new Transaction(trxOut, TransactionType.TRANSFER, amount, fee,
                sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), destName, Calendar.now());
        sourceAcc.debit(total, debit);

        String trxIn = "TRX-" + TRX_COUNTER.getAndIncrement();
        String senderName = getOwnerName(sourceAcc);
        Transaction credit = new Transaction(trxIn, TransactionType.TRANSFER, amount, 0.0,
                sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), senderName, Calendar.now());
        destAcc.credit(amount, credit);

        return new TransferReceipt(debit);
    }

    private String getRecipientName(String senderPhone, Account destAcc) {
        return contactRepo.findContact(senderPhone, destAcc.getOwnerPhoneNumber())
                .map(Contact::getFullName)
                .orElseGet(() -> getOwnerName(destAcc));
    }

    private void triggerMicroSavings(String phone, double amount) {
        if (fundService != null) {
            try {
                fundService.autoSaveRemaining(phone, amount);
            } catch (Exception ignored) {
                // Micro-savings failure must not block transfer
            }
        }
    }

    private Account getAccountByNumber(String accNum) {
        return accountRepo.findByAccountNumber(accNum)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accNum));
    }

    private String getOwnerName(Account account) {
        return userRepo.findCustomerByPhone(account.getOwnerPhoneNumber())
                .map(Customer::getFullName)
                .orElse("Sender");
    }

    private Customer getApprovedCustomer(String phone) {
        Customer cust = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        if (cust.isBlocked()) {
            throw new ValidationException("Customer account is blocked: " + phone);
        }
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