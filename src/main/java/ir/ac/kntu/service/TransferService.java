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
 * Multi-channel transfer switch orchestrating instant and queued transactions.
 */
public class TransferService {
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
        Account destAcc = accountRepo.findByCardNumber(destCard)
                .orElseThrow(() -> new AccountNotFoundException("Destination card not found: " + destCard));
        double fee = TransferChannel.CARD_TO_CARD.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferPol(String senderPhone, String targetAccNum, double amount) {
        Account destAcc = getAccountByNumber(targetAccNum);
        double fee = TransferChannel.POL.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferFariToFari(String senderPhone, String targetAccNum, double amount) {
        Account destAcc = getAccountByNumber(targetAccNum);
        double fee = TransferChannel.FARI_TO_FARI.calculateFee(amount);
        return executeDirectTransfer(senderPhone, destAcc, amount, fee);
    }

    public TransferReceipt transferPaya(String senderPhone, String targetAccNum, double amount) {
        if (amount <= 0) {
            throw new ValidationException("Amount must be strictly positive.");
        }
        Customer sender = getApprovedCustomer(senderPhone);
        Account sourceAcc = getCustomerAccount(sender);
        Account destAcc = getAccountByNumber(targetAccNum);

        double fee = TransferChannel.PAYA.calculateFee(amount);
        double total = amount + fee;

        Transaction trxDebit;
        synchronized (sourceAcc) {
            if (sourceAcc.getBalance() < total) {
                throw new InsufficientFundsException("Insufficient balance for Paya transfer.");
            }
            String trxId = "TRX-" + TRX_COUNTER.getAndIncrement();
            String desc = "Paya Queued to " + destAcc.getAccountNumber();
            trxDebit = new Transaction(trxId, TransactionType.TRANSFER, amount, fee,
                    sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), desc, Calendar.now());
            sourceAcc.debit(total, trxDebit);
        }

        String payaId = "PAYA-" + PAYA_COUNTER.getAndIncrement();
        PayaRecord payaRec = new PayaRecord(payaId, sourceAcc.getAccountNumber(), destAcc.getAccountNumber(), amount);
        payaRec.setSenderPhone(senderPhone);
        payaRepo.save(payaRec);

        triggerMicroSavings(senderPhone, amount);
        sender.addRecentAccount(destAcc.getAccountNumber());
        return new TransferReceipt(trxDebit);
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

        if (sourceAcc.getAccountNumber().equals(destAcc.getAccountNumber())) {
            throw new ValidationException("Self-transfers are not allowed.");
        }
        if (!sender.isContactsEnabled() || !recipient.isContactsEnabled()) {
            throw new ValidationException("Contacts functionality is disabled.");
        }
        if (!contactRepo.isMutual(senderPhone, targetPhone)) {
            throw new ValidationException("Transfers require a mutual contact relationship.");
        }

        double fee = amount * 0.005;
        sender.addRecentAccount(destAcc.getAccountNumber());
        TransferReceipt receipt = executeOrderedLock(sourceAcc, destAcc, amount, fee);
        triggerMicroSavings(senderPhone, amount);
        return receipt;
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