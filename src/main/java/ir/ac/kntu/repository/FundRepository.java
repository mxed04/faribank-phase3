package ir.ac.kntu.repository;

import ir.ac.kntu.domain.fund.Fund;
import ir.ac.kntu.domain.fund.FundType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository managing capital funds by ID and owner phone number.
 */
public class FundRepository {
    private final Map<String, Fund> fundsById = new ConcurrentHashMap<>();

    public synchronized void save(Fund fund) {
        if (fund != null) {
            fundsById.put(fund.getFundId(), fund);
        }
    }

    public Optional<Fund> findById(String fundId) {
        if (fundId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fundsById.get(fundId.trim()));
    }

    public List<Fund> findByOwnerPhone(String phone) {
        if (phone == null) {
            return List.of();
        }
        List<Fund> userFunds = new ArrayList<>();
        for (Fund fund : fundsById.values()) {
            if (fund.getOwnerPhone().equals(phone.trim())) {
                userFunds.add(fund);
            }
        }
        return List.copyOf(userFunds);
    }

    public Optional<Fund> findByOwnerAndType(String phone, FundType type) {
        if (phone == null || type == null) {
            return Optional.empty();
        }
        for (Fund fund : fundsById.values()) {
            if (fund.getOwnerPhone().equals(phone.trim()) && fund.getFundType() == type) {
                return Optional.of(fund);
            }
        }
        return Optional.empty();
    }

    public List<Fund> findAll() {
        return List.copyOf(fundsById.values());
    }
}