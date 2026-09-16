package ir.ac.kntu.domain.ticket;

import ir.ac.kntu.domain.user.SupportSection;

/**
 * Functional departments handling system tickets and verification requests.
 */
public enum TicketSection {
    AUTH,
    REPORT,
    FUNDS,
    CONTACTS,
    TRANSFER,
    CHARGE,
    CARD,
    SETTINGS;

    public SupportSection toSupportSection() {
        return SupportSection.valueOf(this.name());
    }

    public static TicketSection fromSupportSection(SupportSection section) {
        if (section == null) {
            return null;
        }
        return TicketSection.valueOf(section.name());
    }
}