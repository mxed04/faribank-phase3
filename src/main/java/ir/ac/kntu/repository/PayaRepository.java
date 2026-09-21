package ir.ac.kntu.repository;

import ir.ac.kntu.domain.transfer.PayaRecord;
import ir.ac.kntu.domain.transfer.PayaStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository storing and indexing queued Paya payment records.
 */
public class PayaRepository {
    private final Map<String, PayaRecord> recordsById = new ConcurrentHashMap<>();

    public synchronized void save(PayaRecord record) {
        if (record != null) {
            recordsById.put(record.getPayaId(), record);
        }
    }

    public Optional<PayaRecord> findById(String payaId) {
        if (payaId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(recordsById.get(payaId.trim()));
    }

    public List<PayaRecord> findQueued() {
        List<PayaRecord> queued = new ArrayList<>();
        for (PayaRecord rec : recordsById.values()) {
            if (rec.getStatus() == PayaStatus.QUEUED) {
                queued.add(rec);
            }
        }
        return List.copyOf(queued);
    }

    public List<PayaRecord> findBySenderPhone(String phone) {
        if (phone == null) {
            return List.of();
        }
        List<PayaRecord> matches = new ArrayList<>();
        for (PayaRecord rec : recordsById.values()) {
            if (rec.getSenderPhone().equals(phone.trim())) {
                matches.add(rec);
            }
        }
        return List.copyOf(matches);
    }

    public List<PayaRecord> findAll() {
        return List.copyOf(recordsById.values());
    }
}