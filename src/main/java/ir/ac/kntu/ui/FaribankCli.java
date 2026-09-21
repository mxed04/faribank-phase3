package ir.ac.kntu.ui;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.FaribankException;

import java.util.Objects;

/**
 * Top-level application controller managing entry, login, registration, and administrative access.
 */
public class FaribankCli {
    private final BankServices services;
    private final ConsoleIo console;
    private final CustomerCli customerCli;
    private final SupportCli supportCli;

    public FaribankCli(BankServices services, ConsoleIo console) {
        this.services = Objects.requireNonNull(services);
        this.console = Objects.requireNonNull(console);
        this.customerCli = new CustomerCli(services, console);
        this.supportCli = new SupportCli(services, console);
    }

    public void start() {
        while (true) {
            console.printTitle("Welcome to Faribank - Neobank Simulation");
            console.printMenu("1", "Customer Login");
            console.printMenu("2", "Customer Registration");
            console.printMenu("3", "Support Operator Login");
            console.printMenu("4", "Administrator Login");
            console.printMenu("quit", "Exit Faribank");

            String choice = console.readLine("Select Option");
            if ("quit".equalsIgnoreCase(choice)) {
                console.printInfo("Thank you for using Faribank. Goodbye!");
                break;
            }

            try {
                processChoice(choice);
            } catch (FaribankException exception) {
                console.printError(exception.getMessage());
            }
        }
    }

    private void processChoice(String choice) {
        switch (choice) {
            case "1" -> loginCustomer();
            case "2" -> registerCustomer();
            case "3" -> loginSupport();
            case "4" -> loginAdmin();
            default -> console.printError("Invalid option. Please try again.");
        }
    }

    private void loginCustomer() {
        String phone = console.readLine("Enter Phone (09XXXXXXXXX)");
        String pass = console.readLine("Enter Password");
        Customer customer = services.getAuthService().authenticateCustomer(phone, pass);

        if (customer.getKycStatus() == KycStatus.PENDING) {
            console.printWarning("Your KYC is under review by support. Portal access is restricted.");
            return;
        }

        if (customer.getKycStatus() == KycStatus.REJECTED) {
            console.printError("KYC Verification Rejected! Reason: " + customer.getRejectionReason());
            String editChoice = console.readLine("Edit details and re-request verification? (Y/N)");
            if ("Y".equalsIgnoreCase(editChoice)) {
                String first = console.readLine("First Name");
                String last = console.readLine("Last Name");
                String nationalId = console.readLine("10-Digit National Code");
                services.getAuthService().updateCustomerKycData(phone, first, last, nationalId);
                console.printSuccess("Information updated and re-submitted for review.");
            }
            return;
        }

        customerCli.runCustomerMenu(customer);
    }

    private void registerCustomer() {
        console.printTitle("Faribank Registration");
        String first = console.readLine("First Name");
        String last = console.readLine("Last Name");
        String phone = console.readLine("Phone Number (09XXXXXXXXX)");
        String nationalId = console.readLine("10-Digit National Code");
        String pass = console.readLine("Strong Password (Upper, Lower, Digit, Special)");

        Customer customer = new Customer(first, last, phone, nationalId, pass);
        services.getAuthService().registerCustomer(customer);
        console.printSuccess("Registration submitted successfully! Please wait for KYC approval.");
    }

    private void loginSupport() {
        String username = console.readLine("Operator Username");
        String pass = console.readLine("Operator Password");
        SupportUser user = services.getAuthService().authenticateSupport(username, pass);
        console.printSuccess("Welcome, Operator " + user.getFullName());
        supportCli.runSupportMenu(user);
    }

    private void loginAdmin() {
        String username = console.readLine("Admin Username (default: admin)");
        String pass = console.readLine("Admin Password (default: Admin@1234)");
        AdminUser admin = services.getAuthService().authenticateAdmin(username, pass);
        console.printSuccess("Welcome, Administrator " + admin.getFullName());
        new AdminCli(services, console).start(admin);
    }
}