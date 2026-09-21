package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.report.FinancialSummary;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.exception.AccountNotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Analytics service delivering transaction filtration, cash-flow metrics, and HTML exports.
 */
public class FinancialReportService {
    private final AccountRepository accountRepo;
    private final UserRepository userRepo;

    public FinancialReportService(AccountRepository accountRepo, UserRepository userRepo) {
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo is required.");
        this.userRepo = Objects.requireNonNull(userRepo, "User repo is required.");
    }

    public List<Transaction> filterTransactions(String phone, Instant start, Instant end, TransactionType filterType) {
        Account account = getAccountByPhone(phone);
        List<Transaction> matches = new ArrayList<>();

        for (Transaction transaction : account.getTransactions()) {
            Instant time = transaction.getTimestamp();
            boolean afterStart = start == null || !time.isBefore(start);
            boolean beforeEnd = end == null || !time.isAfter(end);
            boolean typeMatch = filterType == null || transaction.getType() == filterType;

            if (afterStart && beforeEnd && typeMatch) {
                matches.add(transaction);
            }
        }
        return List.copyOf(matches);
    }

    public FinancialSummary calculateSummary(String phone, Instant start, Instant end) {
        Account account = getAccountByPhone(phone);
        String accNum = account.getAccountNumber();

        double inflow = 0.0;
        double outflow = 0.0;
        double totalFees = 0.0;
        int count = 0;

        for (Transaction item : account.getTransactions()) {
            Instant time = item.getTimestamp();
            if ((start != null && time.isBefore(start)) || (end != null && time.isAfter(end))) {
                continue;
            }
            count++;
            totalFees += item.getFee();
            if (accNum.equals(item.getDestAccount()) || item.getType() == TransactionType.CHARGE) {
                inflow += item.getAmount();
            } else if (accNum.equals(item.getSourceAccount())) {
                outflow += item.getAmount();
            }
        }
        return new FinancialSummary(inflow, outflow, totalFees, count);
    }

    public String generateHtmlStatement(String phone, Instant start, Instant end) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        Account account = getAccountByPhone(phone);
        FinancialSummary summary = calculateSummary(phone, start, end);
        List<Transaction> records = filterTransactions(phone, start, end, null);

        StringBuilder builder = new StringBuilder(1024);
        builder.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Statement</title>")
                .append("<style>body{font-family:sans-serif;margin:20px;color:#222;}")
                .append(".card{border:1px solid #ddd;padding:15px;border-radius:6px;margin-bottom:15px;}")
                .append("table{width:100%;border-collapse:collapse;}th,td{padding:8px;border:1px solid #ddd;}")
                .append("th{background:#f5f5f5;text-align:left;}</style></head><body>")
                .append("<h2>Faribank Account Statement</h2>")
                .append("<div class='card'><p><b>Customer:</b> ").append(customer.getFullName())
                .append(" | <b>Account:</b> ").append(account.getAccountNumber())
                .append(" | <b>Current Balance:</b> ").append(account.getBalance()).append(" IRR</p></div>")
                .append("<div class='card'><h4>Financial Analytics</h4>")
                .append("<p><b>Total Inflow:</b> ").append(summary.getTotalInflow()).append(" IRR</p>")
                .append("<p><b>Total Outflow:</b> ").append(summary.getTotalOutflow()).append(" IRR</p>")
                .append("<p><b>Fees Paid:</b> ").append(summary.getTotalFees()).append(" IRR</p>")
                .append("<p><b>Net Flow:</b> ").append(summary.getNetCashFlow()).append(" IRR</p></div>")
                .append("<h3>Transaction Ledger (").append(summary.getRecordCount()).append(" Records)</h3>")
                .append("<table><tr><th>Tracking ID</th><th>Type</th><th>Amount (IRR)</th><th>Fee</th><th>Counterparty</th><th>Date</th></tr>");

        for (Transaction record : records) {
            builder.append("<tr><td>").append(record.getTrackingNumber())
                    .append("</td><td>").append(record.getType())
                    .append("</td><td>").append(record.getAmount())
                    .append("</td><td>").append(record.getFee())
                    .append("</td><td>").append(record.getDestOwnerName())
                    .append("</td><td>").append(record.getTimestamp())
                    .append("</td></tr>");
        }
        builder.append("</table></body></html>");
        return builder.toString();
    }

    private Account getAccountByPhone(String phone) {
        return accountRepo.findByPhone(phone)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for: " + phone));
    }
}