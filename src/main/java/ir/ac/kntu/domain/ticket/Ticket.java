package ir.ac.kntu.domain.ticket;

import ir.ac.kntu.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Support ticket and KYC verification domain entity.
 */
public class Ticket {
    private final String ticketId;
    private final String userPhoneNumber;
    private final TicketSection section;
    private final String text;
    private final Instant createdAt;
    private TicketStatus status;
    private String supportReply;

    public Ticket(String ticketId, String userPhoneNumber, TicketSection section,
                  String text, Instant createdAt) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new ValidationException("Ticket ID cannot be empty.");
        }
        if (userPhoneNumber == null || userPhoneNumber.trim().isEmpty()) {
            throw new ValidationException("User phone cannot be empty.");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Ticket text cannot be empty.");
        }
        this.ticketId = ticketId.trim();
        this.userPhoneNumber = userPhoneNumber.trim();
        this.section = Objects.requireNonNull(section, "Ticket section cannot be null.");
        this.text = text.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp cannot be null.");
        this.status = TicketStatus.REGISTERED;
        this.supportReply = "";
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public String getUserPhone() {
        return userPhoneNumber;
    }

    public TicketSection getSection() {
        return section;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null.");
    }

    public String getSupportReply() {
        return supportReply;
    }

    public void setSupportReply(String reply) {
        this.supportReply = reply != null ? reply.trim() : "";
    }

    public String getReply() {
        return supportReply;
    }

    public void setReply(String reply) {
        setSupportReply(reply);
    }

    public boolean isKycRequest() {
        return section == TicketSection.AUTH;
    }
}