package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.fund.BonusFund;
import ir.ac.kntu.domain.fund.Fund;
import ir.ac.kntu.domain.fund.RemainingFund;
import ir.ac.kntu.domain.fund.SavingsFund;
import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.ticket.TicketStatus;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service providing full JSON serialization and deserialization for data persistence.
 */
public class JsonStorageService {
    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final FundRepository fundRepo;
    private final TicketRepository ticketRepo;

    public JsonStorageService(UserRepository userRepo, AccountRepository accountRepo,
                              FundRepository fundRepo, TicketRepository ticketRepo) {
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.fundRepo = Objects.requireNonNull(fundRepo, "Fund repo cannot be null.");
        this.ticketRepo = Objects.requireNonNull(ticketRepo, "Ticket repo cannot be null.");
    }

    public void exportToJsonFile(String filePath) throws IOException {
        StringBuilder builder = new StringBuilder(2048);
        builder.append("{\n");
        appendCustomersJson(builder);
        builder.append(",\n");
        appendAccountsJson(builder);
        builder.append(",\n");
        appendFundsJson(builder);
        builder.append(",\n");
        appendTicketsJson(builder);
        builder.append("\n}");

        Path destPath = Paths.get(filePath);
        if (destPath.getParent() != null) {
            Files.createDirectories(destPath.getParent());
        }
        Files.writeString(destPath, builder.toString(), StandardCharsets.UTF_8);
    }

    public void importFromJsonFile(String filePath) throws IOException {
        Path sourcePath = Paths.get(filePath);
        if (!Files.exists(sourcePath)) {
            return;
        }
        String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
        restoreCustomers(content);
        restoreAccounts(content);
        restoreFunds(content);
        restoreTickets(content);
    }

    private void appendCustomersJson(StringBuilder builder) {
        builder.append("  \"customers\": [\n");
        List<Customer> customers = userRepo.findAllCustomers();
        for (int index = 0; index < customers.size(); index++) {
            Customer item = customers.get(index);
            builder.append("    {")
                    .append("\"phone\":\"").append(item.getPhoneNumber()).append("\",")
                    .append("\"first\":\"").append(item.getFirstName()).append("\",")
                    .append("\"last\":\"").append(item.getLastName()).append("\",")
                    .append("\"natId\":\"").append(item.getNationalCode()).append("\",")
                    .append("\"pass\":\"").append(item.getPassword()).append("\",")
                    .append("\"kyc\":\"").append(item.getKycStatus().name()).append("\",")
                    .append("\"blocked\":").append(item.isBlocked())
                    .append("}");
            if (index < customers.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
    }

    private void appendAccountsJson(StringBuilder builder) {
        builder.append("  \"accounts\": [\n");
        List<Account> accounts = accountRepo.findAll();
        for (int index = 0; index < accounts.size(); index++) {
            Account account = accounts.get(index);
            builder.append("    {")
                    .append("\"accNum\":\"").append(account.getAccountNumber()).append("\",")
                    .append("\"phone\":\"").append(account.getOwnerPhoneNumber()).append("\",")
                    .append("\"card\":\"").append(account.getCreditCard().getCardNumber()).append("\",")
                    .append("\"balance\":").append(account.getBalance()).append(",")
                    .append("\"txs\":[");
            appendTransactionsJson(builder, account.getTransactions());
            builder.append("]}");
            if (index < accounts.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
    }

    private void appendTransactionsJson(StringBuilder builder, List<Transaction> transactions) {
        for (int index = 0; index < transactions.size(); index++) {
            Transaction item = transactions.get(index);
            builder.append("{\"id\":\"").append(item.getTrackingNumber()).append("\",")
                    .append("\"type\":\"").append(item.getType().name()).append("\",")
                    .append("\"amt\":").append(item.getAmount()).append(",")
                    .append("\"fee\":").append(item.getFee()).append(",")
                    .append("\"src\":\"").append(item.getSourceAccount()).append("\",")
                    .append("\"dst\":\"").append(item.getDestAccount()).append("\",")
                    .append("\"desc\":\"").append(item.getDestOwnerName()).append("\",")
                    .append("\"time\":\"").append(item.getTimestamp().toString()).append("\"}");
            if (index < transactions.size() - 1) {
                builder.append(",");
            }
        }
    }

    private void appendFundsJson(StringBuilder builder) {
        builder.append("  \"funds\": [\n");
        List<Fund> funds = fundRepo.findAll();
        for (int index = 0; index < funds.size(); index++) {
            Fund fund = funds.get(index);
            builder.append("    {")
                    .append("\"fundId\":\"").append(fund.getFundId()).append("\",")
                    .append("\"phone\":\"").append(fund.getOwnerPhone()).append("\",")
                    .append("\"type\":\"").append(fund.getFundType().name()).append("\",")
                    .append("\"balance\":").append(fund.getBalance()).append(",")
                    .append("\"created\":\"").append(fund.getCreatedAt().toString()).append("\"");
            if (fund instanceof BonusFund bonus) {
                builder.append(",\"rate\":").append(bonus.getInterestRate())
                        .append(",\"maturity\":\"").append(bonus.getMaturityDate().toString()).append("\"")
                        .append(",\"paid\":").append(bonus.isInterestPaid());
            }
            builder.append("}");
            if (index < funds.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
    }

    private void appendTicketsJson(StringBuilder builder) {
        builder.append("  \"tickets\": [\n");
        List<Ticket> tickets = ticketRepo.findAll();
        for (int index = 0; index < tickets.size(); index++) {
            Ticket ticket = tickets.get(index);
            builder.append("    {")
                    .append("\"ticketId\":\"").append(ticket.getTicketId()).append("\",")
                    .append("\"phone\":\"").append(ticket.getUserPhone()).append("\",")
                    .append("\"section\":\"").append(ticket.getSection().name()).append("\",")
                    .append("\"status\":\"").append(ticket.getStatus().name()).append("\",")
                    .append("\"desc\":\"").append(escapeJson(ticket.getDescription())).append("\",")
                    .append("\"created\":\"").append(ticket.getCreatedAt().toString()).append("\"")
                    .append("}");
            if (index < tickets.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
    }

    private void restoreCustomers(String json) {
        String arrayPart = extractArray(json, "customers");
        if (arrayPart.isEmpty()) {
            return;
        }
        for (String row : splitObjects(arrayPart)) {
            String phone = extractField(row, "phone");
            String first = extractField(row, "first");
            String last = extractField(row, "last");
            String natId = extractField(row, "natId");
            String pass = extractField(row, "pass");
            Customer customer = new Customer(first, last, phone, natId, pass);
            customer.setKycStatus(KycStatus.valueOf(extractField(row, "kyc")));
            customer.setBlocked(Boolean.parseBoolean(extractField(row, "blocked")));
            userRepo.saveCustomer(customer);
        }
    }

    private void restoreAccounts(String json) {
        String arrayPart = extractArray(json, "accounts");
        if (arrayPart.isEmpty()) {
            return;
        }
        for (String row : splitObjects(arrayPart)) {
            String accNum = extractField(row, "accNum");
            String phone = extractField(row, "phone");
            String card = extractField(row, "card");
            double balance = Double.parseDouble(extractField(row, "balance"));

            Account account = new Account(accNum, phone, new CreditCard(card));
            if (balance > 0.0) {
                account.deposit(balance);
            }

            userRepo.findCustomerByPhone(phone).ifPresent(customer -> customer.setAccount(account));
            accountRepo.save(account);
        }
    }

    private void restoreFunds(String json) {
        String arrayPart = extractArray(json, "funds");
        if (arrayPart.isEmpty()) {
            return;
        }
        for (String row : splitObjects(arrayPart)) {
            Fund fund = parseFund(row);
            fundRepo.save(fund);
        }
    }

    private Fund parseFund(String row) {
        String fundId = extractField(row, "fundId");
        String phone = extractField(row, "phone");
        String type = extractField(row, "type");
        double balance = Double.parseDouble(extractField(row, "balance"));
        Instant created = Instant.parse(extractField(row, "created"));

        if ("SAVINGS".equalsIgnoreCase(type)) {
            return new SavingsFund(fundId, phone, balance, created);
        }
        if ("REMAINING".equalsIgnoreCase(type)) {
            return new RemainingFund(fundId, phone, balance, created);
        }
        double rate = Double.parseDouble(extractField(row, "rate"));
        Instant maturity = Instant.parse(extractField(row, "maturity"));
        boolean paid = Boolean.parseBoolean(extractField(row, "paid"));

        BonusFund bonusFund = new BonusFund(fundId, phone, balance, rate, maturity, created);
        if (paid) {
            bonusFund.setInterestPaid(true);
        }
        return bonusFund;
    }

    private void restoreTickets(String json) {
        String arrayPart = extractArray(json, "tickets");
        if (arrayPart.isEmpty()) {
            return;
        }
        for (String row : splitObjects(arrayPart)) {
            String ticketId = extractField(row, "ticketId");
            String phone = extractField(row, "phone");
            TicketSection section = TicketSection.valueOf(extractField(row, "section"));
            String description = extractField(row, "desc");
            Instant created = row.contains("\"created\":")
                    ? Instant.parse(extractField(row, "created")) : Instant.now();

            Ticket ticket = new Ticket(ticketId, phone, section, description, created);
            ticket.setStatus(TicketStatus.valueOf(extractField(row, "status")));
            ticketRepo.save(ticket);
        }
    }

    private String extractArray(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) {
            return "";
        }
        int startPos = json.indexOf('[', keyIndex);
        if (startPos == -1) {
            return "";
        }
        int depth = 0;
        for (int pos = startPos; pos < json.length(); pos++) {
            char current = json.charAt(pos);
            if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(startPos + 1, pos).trim();
                }
            }
        }
        return "";
    }

    private List<String> splitObjects(String arrayBody) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int startIndex = -1;
        for (int pos = 0; pos < arrayBody.length(); pos++) {
            char current = arrayBody.charAt(pos);
            if (current == '{') {
                if (depth == 0) {
                    startIndex = pos;
                }
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && startIndex != -1) {
                    objects.add(arrayBody.substring(startIndex, pos + 1));
                    startIndex = -1;
                }
            }
        }
        return objects;
    }

    private String extractField(String jsonObject, String field) {
        String search = "\"" + field + "\":";
        int fieldIndex = jsonObject.indexOf(search);
        if (fieldIndex == -1) {
            return "";
        }
        int startPos = fieldIndex + search.length();
        while (startPos < jsonObject.length() && Character.isWhitespace(jsonObject.charAt(startPos))) {
            startPos++;
        }
        if (startPos < jsonObject.length() && jsonObject.charAt(startPos) == '\"') {
            int endPos = jsonObject.indexOf('\"', startPos + 1);
            return endPos != -1 ? jsonObject.substring(startPos + 1, endPos) : "";
        }
        int endPos = startPos;
        while (endPos < jsonObject.length()
                && jsonObject.charAt(endPos) != ','
                && jsonObject.charAt(endPos) != '}'
                && jsonObject.charAt(endPos) != ']') {
            endPos++;
        }
        return jsonObject.substring(startPos, endPos).trim();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\"", "\\\"").replace("\n", " ");
    }
}