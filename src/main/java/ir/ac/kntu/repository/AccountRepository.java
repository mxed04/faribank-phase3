package ir.ac.kntu.repository;

import ir.ac.kntu.domain.account.Account;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository managing bank accounts indexed by phone, account, and card.
 */
public class AccountRepository {
    private final Map<String, Account> accountsByPhone = new ConcurrentHashMap<>();
    private final Map<String, Account> accountsByNum = new ConcurrentHashMap<>();
    private final Map<String, Account> accountsByCard = new ConcurrentHashMap<>();

    public synchronized void save(Account account) {
        if (account != null) {
            accountsByPhone.put(account.getOwnerPhoneNumber(), account);
            accountsByNum.put(account.getAccountNumber(), account);
            if (account.getCreditCard() != null) {
                accountsByCard.put(account.getCreditCard().getCardNumber(), account);
            }
        }
    }

    public Optional<Account> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsByPhone.get(phone.trim()));
    }

    public Optional<Account> findByAccountNumber(String accNum) {
        if (accNum == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsByNum.get(accNum.trim()));
    }

    public Optional<Account> findByCardNumber(String cardNum) {
        if (cardNum == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsByCard.get(cardNum.trim()));
    }

    public Optional<Account> findAccountByNumber(String accNum) {
        return findByAccountNumber(accNum);
    }

    public Optional<Account> findByNumber(String accNum) {
        return findByAccountNumber(accNum);
    }

    public List<Account> findAll() {
        return List.copyOf(accountsByPhone.values());
    }
}