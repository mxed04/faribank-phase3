package ir.ac.kntu.ui;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.SupportSection;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.util.Calendar;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

/**
 * Interactive administrative console for role creation, ancestor hierarchy governance, and batch settlements.
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
            console.printInfo("7. Backup System State to JSON | 8. Restore System State from JSON");
            console.printInfo("9. Create Sub-Admin | 10. Create Support | 11. Assign Support Sections | 0. Logout");

            String opt = readLine("Choice: ");
            switch (opt) {
                case "1" -> handleCustomerSearch();
                case "2" -> handleBlockStatus(admin);
                case "3" -> handleProfileEdit();
                case "4" -> handlePayaSettlement();
                case "5" -> handleInterestPayout();
                case "6" -> handleDailyClearing();
                case "7" -> handleDataBackup();
                case "8" -> handleDataRestore();
                case "9" -> handleCreateAdmin(admin);
                case "10" -> handleCreateSupport(admin);
                case "11" -> handleAssignSections(admin);
                case "0" -> active = false;
                default -> console.printError("Invalid option.");
            }
        }
    }

    private void handleCustomerSearch() {
        String query = readLine("Search Keyword (or empty): ");
        List<Customer> list = services.getAdminCustomerService().searchCustomers(query, null, null);
        paginate(list, item -> String.format("[%s] %s | Phone: %s | KYC: %s | Blocked: %b",
                item.getNationalCode(), item.getFullName(), item.getPhoneNumber(), item.getKycStatus(), item.isBlocked()));
    }

    private void handleBlockStatus(AdminUser admin) {
        try {
            String target = readLine("User Identifier (Phone or Username): ");
            boolean block = "y".equalsIgnoreCase(readLine("Block Account? (y/n): "));
            if (block) {
                services.getAdminService().blockUser(admin.getUsername(), target);
                console.printSuccess("User blocked successfully.");
            } else {
                services.getAdminService().unblockUser(admin.getUsername(), target);
                console.printSuccess("User unblocked successfully.");
            }
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

    private void handleCreateAdmin(AdminUser admin) {
        try {
            console.printInfo("=== CREATE SUB-ADMIN ===");
            String first = readLine("First Name: ");
            String last = readLine("Last Name: ");
            String username = readLine("Username: ");
            String pass = readLine("Password: ");
            AdminUser newAdmin = new AdminUser(first, last, username, pass, admin.getUsername());
            services.getAdminService().createAdmin(admin.getUsername(), newAdmin);
            console.printSuccess("Sub-Admin created successfully: " + username);
        } catch (Exception ex) {
            console.printError("Creation failed: " + ex.getMessage());
        }
    }

    private void handleCreateSupport(AdminUser admin) {
        try {
            console.printInfo("=== CREATE SUPPORT OPERATOR ===");
            String first = readLine("First Name: ");
            String last = readLine("Last Name: ");
            String username = readLine("Username: ");
            String pass = readLine("Password: ");
            SupportUser support = new SupportUser(first, last, username, pass);
            services.getAdminService().createSupport(admin.getUsername(), support);
            console.printSuccess("Support operator created: " + username);
        } catch (Exception ex) {
            console.printError("Creation failed: " + ex.getMessage());
        }
    }

    private void handleAssignSections(AdminUser admin) {
        try {
            console.printInfo("=== ASSIGN SUPPORT SECTIONS ===");
            String target = readLine("Support Username: ");
            console.printInfo("Available: AUTH, REPORT, FUNDS, CONTACTS, TRANSFER, CHARGE, CARD, SETTINGS");
            String sectionStr = readLine("Section to add: ").toUpperCase();
            SupportSection sec = SupportSection.valueOf(sectionStr);
            services.getAdminService().assignSupportSections(admin.getUsername(), target, Set.of(sec));
            console.printSuccess("Section assigned to: " + target);
        } catch (Exception ex) {
            console.printError("Failed to assign section: " + ex.getMessage());
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
            for (int index = 0; index < pageItems.size(); index++) {
                int number = (pager.getCurrentPage() - 1) * pager.getPageSize() + index + 1;
                console.printInfo(number + ". " + formatter.apply(pageItems.get(index)));
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