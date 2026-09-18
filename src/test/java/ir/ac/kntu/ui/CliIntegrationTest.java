package ir.ac.kntu.ui;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CliIntegrationTest {
    private BankServices services;
    private ByteArrayOutputStream outputStream;
    private ConsoleIo consoleIo;

    @BeforeEach
    void setUp() {
        services = new BankServices();
        outputStream = new ByteArrayOutputStream();
        consoleIo = new ConsoleIo(new ByteArrayInputStream(new byte[0]), new PrintStream(outputStream));
    }

    @Test
    void testAdminCliBatchActionsExecution() {
        AdminUser admin = new AdminUser("Super", "Admin", "09120000001", "0000000001", "Admin@1234");
        String simulatedInput = "4\n5\n6\n0\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(simulatedInput.getBytes()));

        AdminCli adminCli = new AdminCli(services, consoleIo, scanner);
        adminCli.start(admin);

        String consoleOutput = outputStream.toString();
        assertTrue(consoleOutput.contains("Paya batch executed"));
        assertTrue(consoleOutput.contains("Profit distribution completed"));
        assertTrue(consoleOutput.contains("Daily clearing routine finalized"));
    }

    @Test
    void testCustomerCliDashboardOverviewAndLogout() {
        Customer cust = new Customer("Reza", "Abbasi", "09123334455", "0011223344", "Pass@1234");
        cust.setKycStatus(KycStatus.APPROVED);
        Account account = new Account("99001", cust.getPhoneNumber(), new CreditCard("6037990011112222"));
        cust.setAccount(account);

        String simulatedInput = "1\n0\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(simulatedInput.getBytes()));

        CustomerCli customerCli = new CustomerCli(services, consoleIo, scanner);
        customerCli.runCustomerMenu(cust);

        String consoleOutput = outputStream.toString();
        assertTrue(consoleOutput.contains("CUSTOMER DASHBOARD"));
        assertTrue(consoleOutput.contains("Account Number:"));
    }
}