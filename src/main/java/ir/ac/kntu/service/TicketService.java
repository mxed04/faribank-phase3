package ir.ac.kntu.service;

import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;
import ir.ac.kntu.domain.user.SupportSection;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.AuthenticationException;
import ir.ac.kntu.exception.TicketNotFoundException;
import ir.ac.kntu.exception.UnauthorizedSectionAccessException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing ticket workflows, section routing, and KYC approvals.
 */
public class TicketService {
    private static final AtomicLong ID_GEN = new AtomicLong(5001);

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final AuthService authService;

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo) {
        this(ticketRepo, userRepo, null);
    }

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo, AuthService authService) {
        this.ticketRepo = Objects.requireNonNull(ticketRepo, "Ticket repo cannot be null.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.authService = authService;
    }

    public Ticket createTicket(String phone, TicketSection section, String text) {
        String ticketId = "TCK-" + ID_GEN.getAndIncrement();
        Ticket ticket = new Ticket(ticketId, phone, section, text, Instant.now());
        ticketRepo.save(ticket);
        return ticket;
    }

    public Ticket getTicket(String ticketId) {
        return ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));
    }

    public List<Ticket> getTicketsByPhone(String phone) {
        return ticketRepo.findByPhone(phone);
    }

    public List<Ticket> getOpenTickets(String phone) {
        List<Ticket> matches = new ArrayList<>();
        for (Ticket tick : ticketRepo.findByPhone(phone)) {
            if (tick.getStatus() == TicketStatus.REGISTERED || tick.getStatus() == TicketStatus.IN_PROGRESS) {
                matches.add(tick);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public List<Ticket> filterTickets(TicketStatus status, TicketSection section, String phone) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket tick : ticketRepo.findAll()) {
            if (status != null && tick.getStatus() != status) {
                continue;
            }
            if (section != null && tick.getSection() != section) {
                continue;
            }
            if (phone != null && !phone.trim().isEmpty() && !tick.getUserPhoneNumber().contains(phone.trim())) {
                continue;
            }
            result.add(tick);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Ticket> getTicketsForSupport(String supportUser) {
        SupportUser support = getActiveSupport(supportUser);
        Set<TicketSection> allowed = getMappedSections(support.getSections());
        return ticketRepo.findBySections(allowed);
    }

    public List<Ticket> filterTicketsForSupport(String supportUser, TicketStatus status,
                                                TicketSection section, String phone) {
        SupportUser support = getActiveSupport(supportUser);
        Set<TicketSection> allowed = getMappedSections(support.getSections());

        if (section != null && !allowed.contains(section)) {
            throw new UnauthorizedSectionAccessException("Operator lacks section access: " + section);
        }

        List<Ticket> result = new ArrayList<>();
        for (Ticket tick : ticketRepo.findAll()) {
            if (!allowed.contains(tick.getSection())) {
                continue;
            }
            if (status != null && tick.getStatus() != status) {
                continue;
            }
            if (section != null && tick.getSection() != section) {
                continue;
            }
            if (phone != null && !phone.trim().isEmpty() && !tick.getUserPhoneNumber().contains(phone.trim())) {
                continue;
            }
            result.add(tick);
        }
        return Collections.unmodifiableList(result);
    }

    public void approveKycTicket(String supportUser, String ticketId) {
        SupportUser support = getActiveSupport(supportUser);
        if (!support.hasSection(SupportSection.AUTH)) {
            throw new UnauthorizedSectionAccessException("Operator lacks AUTH section permission.");
        }
        Ticket ticket = getTicket(ticketId);
        if (!ticket.isKycRequest()) {
            throw new ValidationException("Ticket is not a KYC request: " + ticketId);
        }
        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setSupportReply("Identity verified by operator: " + supportUser);
        if (authService != null) {
            authService.approveKyc(ticket.getUserPhoneNumber());
        }
    }

    public void rejectKycTicket(String supportUser, String ticketId, String reason) {
        SupportUser support = getActiveSupport(supportUser);
        if (!support.hasSection(SupportSection.AUTH)) {
            throw new UnauthorizedSectionAccessException("Operator lacks AUTH section permission.");
        }
        Ticket ticket = getTicket(ticketId);
        if (!ticket.isKycRequest()) {
            throw new ValidationException("Ticket is not a KYC request: " + ticketId);
        }
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setSupportReply(reason);
        if (authService != null) {
            authService.rejectKyc(ticket.getUserPhoneNumber(), reason);
        }
    }

    public void replyTicket(String ticketId, String reply, TicketStatus newStatus) {
        Ticket ticket = getTicket(ticketId);
        ticket.setSupportReply(reply);
        ticket.setStatus(newStatus);
    }

    private SupportUser getActiveSupport(String username) {
        SupportUser support = userRepo.findSupportByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Support operator not found: " + username));
        if (support.isBlocked()) {
            throw new AuthenticationException("Support operator account is blocked.");
        }
        return support;
    }

    private Set<TicketSection> getMappedSections(Set<SupportSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptySet();
        }
        Set<TicketSection> mapped = EnumSet.noneOf(TicketSection.class);
        for (SupportSection sec : sections) {
            mapped.add(TicketSection.fromSupportSection(sec));
        }
        return mapped;
    }
}