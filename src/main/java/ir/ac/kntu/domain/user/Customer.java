package ir.ac.kntu.domain.user;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.util.PasswordValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customer domain model representing standard banking clients.
 */
public class Customer extends User {
    private final String phoneNumber;
    private String nationalCode;
    private KycStatus kycStatus;
    private String rejectionReason;
    private Account account;
    private boolean contactsEnabled;
    private final List<String> recentAccounts;

    public Customer(String firstName, String lastName, String phoneNumber,
                    String nationalCode, String password) {
        super(firstName, lastName, password);
        if (phoneNumber == null || !phoneNumber.matches("^09\\d{9}$")) {
            throw new ValidationException("Invalid phone number format. Must start with 09 and be 11 digits.");
        }
        if (!PasswordValidator.isValid(password)) {
            throw new ValidationException("Weak password: must contain uppercase, lowercase, digit, and special char.");
        }
        this.phoneNumber = phoneNumber.trim();
        setNationalCode(nationalCode);
        this.kycStatus = KycStatus.PENDING;
        this.rejectionReason = "";
        this.contactsEnabled = true;
        this.recentAccounts = new ArrayList<>();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getNationalCode() {
        return nationalCode;
    }

    public final void setNationalCode(String nationalCode) {
        if (nationalCode == null || !nationalCode.matches("^\\d{10}$")) {
            throw new ValidationException("National code must be exactly 10 digits.");
        }
        this.nationalCode = nationalCode.trim();
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason != null ? rejectionReason.trim() : "";
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public boolean isContactsEnabled() {
        return contactsEnabled;
    }

    public void setContactsEnabled(boolean enabled) {
        this.contactsEnabled = enabled;
    }

    public synchronized List<String> getRecentAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(recentAccounts));
    }

    public synchronized void addRecentAccount(String targetAccount) {
        if (targetAccount == null || targetAccount.isBlank()) {
            return;
        }
        recentAccounts.remove(targetAccount);
        recentAccounts.add(0, targetAccount);
        if (recentAccounts.size() > 10) {
            recentAccounts.remove(recentAccounts.size() - 1);
        }
    }
}