package ir.ac.kntu.repository;

import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository managing tickets and unified KYC requests.
 */
public class TicketRepository {
    private final Map<String, Ticket> ticketsById = new ConcurrentHashMap<>();

    public synchronized void save(Ticket ticket) {
        ticketsById.put(ticket.getTicketId(), ticket);
    }

    public Optional<Ticket> findById(String ticketId) {
        if (ticketId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ticketsById.get(ticketId.trim()));
    }

    public List<Ticket> findByPhone(String phone) {
        if (phone == null) {
            return Collections.emptyList();
        }
        List<Ticket> matches = new ArrayList<>();
        for (Ticket tick : ticketsById.values()) {
            if (tick.getUserPhone().equals(phone.trim())) {
                matches.add(tick);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public List<Ticket> findBySections(Set<TicketSection> allowedSections) {
        if (allowedSections == null || allowedSections.isEmpty()) {
            return Collections.emptyList();
        }
        List<Ticket> matches = new ArrayList<>();
        for (Ticket tick : ticketsById.values()) {
            if (allowedSections.contains(tick.getSection())) {
                matches.add(tick);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public List<Ticket> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(ticketsById.values()));
    }

    public Optional<Ticket> findPendingKycByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        for (Ticket tick : ticketsById.values()) {
            if (tick.isKycRequest() && tick.getUserPhone().equals(phone.trim())
                    && (tick.getStatus() == TicketStatus.REGISTERED
                    || tick.getStatus() == TicketStatus.IN_PROGRESS)) {
                return Optional.of(tick);
            }
        }
        return Optional.empty();
    }
}