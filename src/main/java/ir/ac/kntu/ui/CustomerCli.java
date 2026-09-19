package ir.ac.kntu.ui;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransferReceipt;
import ir.ac.kntu.domain.contact.Contact;
import ir.ac.kntu.domain.fund.Fund;
import ir.ac.kntu.domain.report.FinancialSummary;
import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.user.Customer;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Interactive customer CLI providing dashboard, transfers, funds, and financial analytics.
 */
public class CustomerCli {
    private final BankServices services;
    private final ConsoleIo console;
    private final Scanner scanner;

    public CustomerCli(BankServices services, ConsoleIo console) {
        this(services, console, null);
    }

    public CustomerCli(BankServices services, ConsoleIo console, Scanner scanner) {
        this.services = Objects.requireNonNull(services);
        this.console = Objects.requireNonNull(console);
        this.scanner = scanner;
    }

    public void runCustomerMenu(Customer customer) {
        start(customer);
    }

    public void start(Customer customer) {
        boolean active = true;
        while (active) {
            printCustomerMenu(customer);
            String opt = readLine("Choice: ");
            switch (opt) {
                case "1" -> showAccountOverview(customer);
                case "2" -> handleTransfersMenu(customer);
                case "3" -> handleFundsMenu(customer);
                case "4" -> handleAnalyticsMenu(customer);
                case "5" -> handleContactsMenu(customer);
                case "6" -> handleTicketsMenu(customer);
                case "7" -> handleSettingsMenu(customer);
                case "0" -> active = false;
                default -> console.printError("Invalid menu choice.");
            }
        }
    }

    private void printCustomerMenu(Customer customer) {
        console.printInfo("=== CUSTOMER DASHBOARD (" + customer.getFullName() + ") ===");
        console.printInfo("1. Account Overview & Ledger");
        console.printInfo("2. Multi-Channel Fund Transfer");
        console.printInfo("3. Capital Investment Funds");
        console.printInfo("4. Financial Analytics & Statement");
        console.printInfo("5. Contact Address Book");
        console.printInfo("6. Support Tickets");
        console.printInfo("7. Account Settings");
        console.printInfo("0. Sign Out");
    }

    private void showAccountOverview(Customer cust) {
        Account acc = cust.getAccount();
        console.printInfo("Account Number: " + acc.getAccountNumber());
        console.printInfo("Card Number:    " + acc.getCreditCard().getCardNumber());
        console.printInfo("Balance:        " + acc.getBalance() + " IRR");

        List<Transaction> records = acc.getTransactions();
        paginate(records, record -> String.format("[%s] Amt: %.1f | Fee: %.1f | %s",
                record.getTimestamp(), record.getAmount(), record.getFee(), record.getDestOwnerName()));
    }

    private void handleTransfersMenu(Customer cust) {
        console.printInfo("=== MULTI-CHANNEL TRANSFER SWITCH ===");
        console.printInfo("1. Card-to-Card (300 Fee) | 2. POL (2%) | 3. PAYA (2000 Fee) | 4. Fari-to-Fari (0 Fee)");
        String choice = readLine("Select Channel (0 to cancel): ");
        if ("0".equals(choice)) {
            return;
        }
        try {
            double amount = console.readDouble("Enter Amount (IRR): ");
            TransferReceipt receipt = switch (choice) {
                case "1" -> services.getTransferService().transferCardToCard(
                        cust.getPhoneNumber(), readLine("Dest Card: "), amount);
                case "2" -> services.getTransferService().transferPol(
                        cust.getPhoneNumber(), readLine("Dest Account: "), amount);
                case "3" -> services.getTransferService().transferPaya(
                        cust.getPhoneNumber(), readLine("Dest Account: "), amount);
                case "4" -> services.getTransferService().transferFariToFari(
                        cust.getPhoneNumber(), readLine("Dest Account: "), amount);
                default -> throw new IllegalArgumentException("Unknown channel selected.");
            };
            displayReceipt(receipt);
        } catch (Exception ex) {
            console.printError("Transfer Failed: " + ex.getMessage());
        }
    }

    private void handleFundsMenu(Customer cust) {
        console.printInfo("=== CAPITAL FUNDS ===");
        console.printInfo("1. Open Savings | 2. Open Remaining | 3. Open Bonus | 4. Deposit | 5. Withdraw | 6. List");
        String opt = readLine("Choice: ");
        try {
            executeFundChoice(cust, opt);
        } catch (Exception ex) {
            console.printError("Fund Error: " + ex.getMessage());
        }
    }

    private void executeFundChoice(Customer cust, String opt) {
        switch (opt) {
            case "1" -> {
                double deposit = console.readDouble("Deposit: ");
                var savingsFund = services.getFundService().openSavingsFund(cust.getPhoneNumber(), deposit);
                console.printSuccess("Savings Fund created: " + savingsFund.getFundId());
            }
            case "2" -> {
                double deposit = console.readDouble("Deposit: ");
                var remainingFund = services.getFundService().openRemainingFund(cust.getPhoneNumber(), deposit);
                console.printSuccess("Remaining Fund created: " + remainingFund.getFundId());
            }
            case "3" -> {
                double sum = console.readDouble("Capital: ");
                int days = (int) console.readDouble("Days: ");
                double rate = console.readDouble("Rate: ");
                var bonusFund = services.getFundService().openBonusFund(cust.getPhoneNumber(), sum, days, rate);
                console.printSuccess("Bonus Fund created: " + bonusFund.getFundId());
            }
            case "4" -> services.getFundService().depositToFund(readLine("Fund ID: "), console.readDouble("Amount: "));
            case "5" -> services.getFundService().withdrawFromFund(readLine("Fund ID: "), console.readDouble("Amount: "));
            case "6" -> {
                List<Fund> funds = services.getFundService().getCustomerFunds(cust.getPhoneNumber());
                paginate(funds, fund -> String.format("[%s] %s | Balance: %.1f",
                        fund.getFundId(), fund.getFundType(), fund.getBalance()));
            }
            default -> console.printInfo("Exited fund menu.");
        }
    }

    private void handleAnalyticsMenu(Customer cust) {
        console.printInfo("=== FINANCIAL ANALYTICS ===");
        FinancialSummary metrics = services.getReportService().calculateSummary(cust.getPhoneNumber(), null, null);
        console.printInfo("Total Inflow:     " + metrics.getTotalInflow() + " IRR");
        console.printInfo("Total Outflow:    " + metrics.getTotalOutflow() + " IRR");
        console.printInfo("Total Fees Paid:  " + metrics.getTotalFees() + " IRR");
        console.printInfo("Net Cash Flow:    " + metrics.getNetCashFlow() + " IRR");
        console.printInfo("Total Records:    " + metrics.getRecordCount());

        String opt = readLine("Export HTML Statement? (y/n): ");
        if ("y".equalsIgnoreCase(opt)) {
            String html = services.getReportService().generateHtmlStatement(cust.getPhoneNumber(), null, null);
            console.printSuccess("HTML Statement compiled (" + html.length() + " bytes).");
        }
    }

    private void handleContactsMenu(Customer cust) {
        List<Contact> contacts = services.getContactService().getContacts(cust.getPhoneNumber());
        paginate(contacts, contact -> String.format("%s %s (%s)",
                contact.getFirstName(), contact.getLastName(), contact.getPhoneNumber()));
    }

    private void handleTicketsMenu(Customer cust) {
        console.printInfo("=== SUPPORT TICKETS ===");
        console.printInfo("1. Open Ticket | 2. View History");
        if ("1".equals(readLine("Choice: "))) {
            TicketSection sec = TicketSection.valueOf(readLine("Section: "));
            Ticket ticket = services.getTicketService().createTicket(
                    cust.getPhoneNumber(), sec, readLine("Description: "));
            console.printSuccess("Ticket created: " + ticket.getTicketId());
        } else {
            List<Ticket> tickets = services.getTicketService().getOpenTickets(cust.getPhoneNumber());
            paginate(tickets, ticket -> String.format("[%s] %s | %s",
                    ticket.getTicketId(), ticket.getSection(), ticket.getStatus()));
        }
    }

    private void handleSettingsMenu(Customer cust) {
        console.printInfo("=== SETTINGS ===");
        console.printInfo("1. Change Password | 2. Set Card PIN | 3. Toggle Contacts");
        String opt = readLine("Choice: ");
        try {
            if ("1".equals(opt)) {
                services.getSettingsService().changePassword(cust.getPhoneNumber(), readLine("Old: "), readLine("New: "));
                console.printSuccess("Password updated.");
            } else if ("2".equals(opt)) {
                services.getSettingsService().setCardPin(cust.getPhoneNumber(), readLine("4-Digit PIN: "));
                console.printSuccess("PIN updated.");
            } else if ("3".equals(opt)) {
                services.getSettingsService().toggleContacts(cust.getPhoneNumber(), !cust.isContactsEnabled());
                console.printSuccess("Contact transfers toggled.");
            }
        } catch (Exception ex) {
            console.printError("Action Failed: " + ex.getMessage());
        }
    }

    private <T> void paginate(List<T> items, Function<T, String> formatter) {
        if (items == null || items.isEmpty()) {
            console.printInfo("No records available.");
            return;
        }
        PaginationHelper<T> pager = new PaginationHelper<>(items, 10);
        while (true) {
            console.printInfo(String.format("--- Page %d of %d ---", pager.getCurrentPage(), pager.getTotalPages()));
            List<T> pageItems = pager.getPageItems();
            for (int i = 0; i < pageItems.size(); i++) {
                int index = (pager.getCurrentPage() - 1) * pager.getPageSize() + i + 1;
                console.printInfo(index + ". " + formatter.apply(pageItems.get(i)));
            }
            if (pager.getTotalPages() <= 1) {
                break;
            }
            String cmd = readLine("[n] Next | [p] Prev | [q] Quit: ").toLowerCase();
            if ("n".equals(cmd)) {
                pager.nextPage();
            } else if ("p".equals(cmd)) {
                pager.previousPage();
            } else if ("q".equals(cmd)) {
                break;
            }
        }
    }

    private void displayReceipt(TransferReceipt receipt) {
        console.printSuccess("Transfer Completed!");
        console.printInfo("Tracking Ref:    " + receipt.getTrackingNumber());
        console.printInfo("From Account:    " + receipt.getSourceAccount());
        console.printInfo("To Account:      " + receipt.getDestAccount());
        console.printInfo("Recipient Name:  " + receipt.getDestOwnerName());
        console.printInfo("Amount:          " + receipt.getAmount() + " IRR");
        console.printInfo("Fee:             " + receipt.getFee() + " IRR");
        console.printInfo("Total Deducted:  " + receipt.getTotalDeduction() + " IRR");
    }

    private String readLine(String prompt) {
        if (scanner != null && scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return console.readLine(prompt);
    }
}