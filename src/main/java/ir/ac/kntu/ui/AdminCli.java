package ir.ac.kntu.ui;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.util.Calendar;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Interactive administrative console for batch settlements, user audit, and profile governance.
 */
public class AdminCli {
    private final BankServices services;
    private final ConsoleIo console;
    private final Scanner scanner;

    public AdminCli(BankServices services, ConsoleIo console) {
        this(services, console, null);
    }

    public AdminCli(BankServices services, ConsoleIo console, Scanner scanner) {
        this.services = Objects.requireNonNull(services);
        this.console = Objects.requireNonNull(console);
        this.scanner = scanner;
    }

    public void start(AdminUser admin) {
        boolean active = true;
        while (active) {
            console.printInfo("=== ADMINISTRATIVE CONSOLE (" + admin.getUsername() + ") ===");
            console.printInfo("1. Search Customers | 2. Block/Unblock | 3. Edit Profile");
            console.printInfo("4. Settle Paya Queue | 5. Distribute Profits | 6. Daily Clearing");
            console.printInfo("7. Backup System State to JSON");
            console.printInfo("8. Restore System State from JSON | 0. Logout");

            String opt = readLine("Choice: ");
            switch (opt) {
                case "1" -> handleCustomerSearch();
                case "2" -> handleBlockStatus();
                case "3" -> handleProfileEdit();
                case "4" -> handlePayaSettlement();
                case "5" -> handleInterestPayout();
                case "6" -> handleDailyClearing();
                case "7" -> handleDataBackup();
                case "8" -> handleDataRestore();
                case "0" -> active = false;
                default -> console.printError("Invalid option.");
            }
        }
    }

    private void handleCustomerSearch() {
        String query = readLine("Search Keyword (or empty): ");
        List<Customer> list = services.getAdminCustomerService().searchCustomers(query, null, null);
        paginate(list, c -> String.format("[%s] %s | Phone: %s | KYC: %s | Blocked: %b",
                c.getNationalCode(), c.getFullName(), c.getPhoneNumber(), c.getKycStatus(), c.isBlocked()));
    }

    private void handleBlockStatus() {
        try {
            String phone = readLine("Customer Phone: ");
            boolean block = "y".equalsIgnoreCase(readLine("Block Account? (y/n): "));
            services.getAdminCustomerService().setCustomerBlocked(phone, block);
            console.printSuccess("Customer block state updated.");
        } catch (Exception ex) {
            console.printError("Error: " + ex.getMessage());
        }
    }

    private void handleProfileEdit() {
        try {
            String phone = readLine("Customer Phone: ");
            String first = readLine("New First Name: ");
            String last = readLine("New Last Name: ");
            services.getAdminCustomerService().updateCustomerProfile(phone, first, last);
            console.printSuccess("Profile details updated.");
        } catch (Exception ex) {
            console.printError("Error: " + ex.getMessage());
        }
    }

    private void handlePayaSettlement() {
        int cleared = services.getAdminBatchService().settlePayaQueue(Calendar.now());
        console.printSuccess("Paya batch executed. Cleared records: " + cleared);
    }

    private void handleInterestPayout() {
        int matured = services.getAdminBatchService().distributeFundProfits(Calendar.now());
        console.printSuccess("Profit distribution completed. Funds yielded: " + matured);
    }

    private void handleDailyClearing() {
        int total = services.getAdminBatchService().executeDailySettlement(Calendar.now());
        console.printSuccess("Daily clearing routine finalized. Total actions: " + total);
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

    private String readLine(String prompt) {
        if (scanner != null && scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return console.readLine(prompt);
    }

    private void handleDataBackup() {
        try {
            String path = readLine("File path (e.g. data/backup.json): ");
            services.getStorageService().exportToJsonFile(path.isEmpty() ? "data/backup.json" : path);
            console.printSuccess("System state exported successfully to JSON.");
        } catch (Exception ex) {
            console.printError("Backup failed: " + ex.getMessage());
        }
    }

    private void handleDataRestore() {
        try {
            String path = readLine("File path (e.g. data/backup.json): ");
            services.getStorageService().importFromJsonFile(path.isEmpty() ? "data/backup.json" : path);
            console.printSuccess("System state restored successfully from JSON.");
        } catch (Exception ex) {
            console.printError("Restore failed: " + ex.getMessage());
        }
    }
}