package ir.ac.kntu.ui;

import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.CustomerSummary;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.FaribankException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Console user interface managing support operator privileges, scoped tickets, and directory search.
 */
public class SupportCli {
    private final BankServices services;
    private final ConsoleIo console;
    private SupportUser currentOperator;

    public SupportCli(BankServices services, ConsoleIo console) {
        this.services = Objects.requireNonNull(services);
        this.console = Objects.requireNonNull(console);
    }

    public void runSupportMenu() {
        runSupportMenu(null);
    }

    public void runSupportMenu(SupportUser operator) {
        this.currentOperator = operator;
        while (true) {
            console.printTitle("Faribank - Operator Portal ("
                    + (currentOperator != null ? currentOperator.getUsername() : "Operator") + ")");
            console.printMenu("1", "KYC Identity Verifications");
            console.printMenu("2", "Manage Customer Tickets");
            console.printMenu("3", "Search & Inspect Users");
            console.printMenu("back", "Logout & Return");

            String choice = console.readLine("Select Option");
            if ("back".equalsIgnoreCase(choice) || "quit".equalsIgnoreCase(choice)) {
                break;
            }

            try {
                handleSupportChoice(choice);
            } catch (FaribankException exception) {
                console.printError(exception.getMessage());
            }
        }
    }

    private void handleSupportChoice(String choice) {
        switch (choice) {
            case "1" -> handleKyc();
            case "2" -> handleTickets();
            case "3" -> handleUserSearch();
            default -> console.printError("Invalid option. Please try again.");
        }
    }

    private void handleKyc() {
        console.printTitle("Pending KYC Verifications");
        List<Customer> pending = services.getAuthService().getPendingKycRequests();
        if (pending.isEmpty()) {
            console.printInfo("No pending KYC verification requests.");
            return;
        }
        for (Customer customer : pending) {
            console.printInfo("User: " + customer.getFullName() + " | Phone: " + customer.getPhoneNumber()
                    + " | National Code: " + customer.getNationalCode());
        }
        String phone = console.readLine("Enter phone to inspect or 'back'");
        if (!"back".equalsIgnoreCase(phone)) {
            processKycDecision(phone);
        }
    }

    private void processKycDecision(String phone) {
        console.printMenu("A", "Approve Application");
        console.printMenu("R", "Reject Application");
        String decision = console.readLine("Decision");
        String operatorName = currentOperator != null ? currentOperator.getUsername() : "admin";

        if ("A".equalsIgnoreCase(decision)) {
            executeKycApproval(phone, operatorName);
        } else if ("R".equalsIgnoreCase(decision)) {
            executeKycRejection(phone, operatorName);
        }
    }

    private void executeKycApproval(String phone, String operatorName) {
        List<Ticket> tickets = services.getTicketService().filterTickets(
                TicketStatus.REGISTERED, TicketSection.AUTH, phone);
        if (!tickets.isEmpty()) {
            try {
                services.getTicketService().approveKycTicket(operatorName, tickets.get(0).getTicketId());
            } catch (Exception exception) {
                services.getAuthService().approveKyc(phone);
            }
        } else {
            services.getAuthService().approveKyc(phone);
        }
        console.printSuccess("KYC Approved. Account & card generated for: " + phone);
    }

    private void executeKycRejection(String phone, String operatorName) {
        String reason = console.readLine("Rejection Reason");
        List<Ticket> tickets = services.getTicketService().filterTickets(
                TicketStatus.REGISTERED, TicketSection.AUTH, phone);
        if (!tickets.isEmpty()) {
            try {
                services.getTicketService().rejectKycTicket(operatorName, tickets.get(0).getTicketId(), reason);
            } catch (Exception exception) {
                services.getAuthService().rejectKyc(phone, reason);
            }
        } else {
            services.getAuthService().rejectKyc(phone, reason);
        }
        console.printWarning("KYC Rejected for user: " + phone);
    }

    private void handleTickets() {
        console.printTitle("Customer Tickets");
        List<Ticket> list = currentOperator != null
                ? services.getTicketService().getTicketsForSupport(currentOperator.getUsername())
                : services.getTicketService().filterTickets(null, null, null);

        if (list.isEmpty()) {
            console.printInfo("No support tickets found for your assigned sections.");
            return;
        }

        for (Ticket ticket : list) {
            console.printInfo("[" + ticket.getStatus() + "] ID: " + ticket.getTicketId()
                    + " | Phone: " + ticket.getUserPhoneNumber() + " | Text: " + ticket.getText());
        }

        String ticketId = console.readLine("Enter ticket ID to reply or 'back'");
        if (!"back".equalsIgnoreCase(ticketId)) {
            processTicketReply(ticketId);
        }
    }

    private void processTicketReply(String ticketId) {
        String reply = console.readLine("Enter Reply Message");
        console.printMenu("1", "Mark IN_PROGRESS");
        console.printMenu("2", "Mark CLOSED");
        String statusChoice = console.readLine("Status Choice");
        TicketStatus status = "2".equals(statusChoice) ? TicketStatus.CLOSED : TicketStatus.IN_PROGRESS;

        services.getTicketService().replyTicket(ticketId, reply, status);
        console.printSuccess("Ticket reply saved and updated successfully.");
    }

    private void handleUserSearch() {
        console.printTitle("User Directory Inspection");
        String query = console.readLine("Enter Phone or Name query");
        if (query == null || query.isBlank()) {
            console.printInfo("Query cannot be empty.");
            return;
        }
        Map<String, CustomerSummary> results = findMatchingUsers(query.trim());
        if (results.isEmpty()) {
            console.printInfo("No users found matching query: " + query);
            return;
        }
        for (CustomerSummary summary : results.values()) {
            console.printInfo("Name: " + summary.getFullName() + " | Phone: " + summary.getPhone()
                    + " | Acc: " + summary.getAccountNum() + " | Total Tx: " + summary.getTransactions().size());
        }
    }

    private Map<String, CustomerSummary> findMatchingUsers(String cleanQuery) {
        Map<String, CustomerSummary> combined = new LinkedHashMap<>();
        services.getSupportService().searchCustomers(cleanQuery, null, null)
                .forEach(summary -> combined.put(summary.getPhone(), summary));
        services.getSupportService().searchCustomers(null, cleanQuery, null)
                .forEach(summary -> combined.put(summary.getPhone(), summary));
        services.getSupportService().searchCustomers(null, null, cleanQuery)
                .forEach(summary -> combined.put(summary.getPhone(), summary));
        return combined;
    }
}