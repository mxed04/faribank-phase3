package ir.ac.kntu.repository;

import ir.ac.kntu.domain.sim.SimCard;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository managing SIM airtime ledgers independent of bank accounts.
 */
public class SimRepository {
    private final Map<String, SimCard> cardsByPhone = new ConcurrentHashMap<>();

    public SimCard getOrCreate(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String cleanPhone = phoneNumber.trim();
        return cardsByPhone.computeIfAbsent(cleanPhone, SimCard::new);
    }

    public Optional<SimCard> findByPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cardsByPhone.get(phoneNumber.trim()));
    }

    public void save(SimCard simCard) {
        if (simCard != null) {
            cardsByPhone.put(simCard.getPhoneNumber(), simCard);
        }
    }

    public List<SimCard> findAll() {
        return List.copyOf(cardsByPhone.values());
    }
}