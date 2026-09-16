package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.ticket.Ticket;
import ir.ac.kntu.domain.ticket.TicketSection;
import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.exception.AuthenticationException;
import ir.ac.kntu.exception.UserAlreadyExistsException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service orchestrating authentication, user verification, and automatic KYC ticket issuance.
 */
public class AuthService {
    private static final AtomicLong ACC_COUNTER = new AtomicLong(100001);
    private static final AtomicLong TICKET_COUNTER = new AtomicLong(1001);

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final TicketRepository ticketRepo;

    public AuthService(UserRepository userRepo, AccountRepository accountRepo) {
        this(userRepo, accountRepo, null);
    }

    public AuthService(UserRepository userRepo, AccountRepository accountRepo, TicketRepository ticketRepo) {
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.ticketRepo = ticketRepo;
    }

    public void registerCustomer(Customer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null.");
        if (userRepo.findCustomerByPhone(customer.getPhoneNumber()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already registered: " + customer.getPhoneNumber());
        }
        if (userRepo.findCustomerByNationalCode(customer.getNationalCode()).isPresent()) {
            throw new UserAlreadyExistsException("National code already registered: " + customer.getNationalCode());
        }
        userRepo.saveCustomer(customer);
        dispatchKycTicket(customer, "Initial registration KYC request.");
    }

    public void updateCustomerKycData(String phone, String firstName, String lastName, String nationalId) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setNationalCode(nationalId);
        customer.setKycStatus(KycStatus.PENDING);
        dispatchKycTicket(customer, "Profile update KYC verification request.");
    }

    private void dispatchKycTicket(Customer customer, String reason) {
        if (ticketRepo != null) {
            String tickId = "KYC-" + TICKET_COUNTER.getAndIncrement();
            String desc = reason + " Name: " + customer.getFullName()
                    + " | NationalCode: " + customer.getNationalCode();
            Ticket ticket = new Ticket(tickId, customer.getPhoneNumber(),
                    TicketSection.AUTH, desc, Instant.now());
            ticketRepo.save(ticket);
        }
    }

    public Customer authenticateCustomer(String phone, String password) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("Invalid phone number or password."));
        if (!customer.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid phone number or password.");
        }
        if (customer.isBlocked()) {
            throw new AuthenticationException("Customer account is blocked.");
        }
        return customer;
    }

    public SupportUser authenticateSupport(String username, String password) {
        SupportUser support = userRepo.findSupportByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid support username or password."));
        if (!support.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid support username or password.");
        }
        if (support.isBlocked()) {
            throw new AuthenticationException("Support account is blocked.");
        }
        return support;
    }

    public AdminUser authenticateAdmin(String username, String password) {
        AdminUser admin = userRepo.findAdminByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid admin username or password."));
        if (!admin.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid admin username or password.");
        }
        if (admin.isBlocked()) {
            throw new AuthenticationException("Admin account is blocked.");
        }
        return admin;
    }

    public void approveKyc(String phone) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        customer.setKycStatus(KycStatus.APPROVED);
        Account account = accountRepo.findByPhone(phone).orElseGet(() -> {
            String accNum = String.valueOf(ACC_COUNTER.getAndIncrement());
            String cardNum = "6037" + accNum + "1234";
            CreditCard card = new CreditCard(cardNum);
            Account newAcc = new Account(accNum, phone, card);
            accountRepo.save(newAcc);
            return newAcc;
        });
        customer.setAccount(account);
    }

    public void rejectKyc(String phone) {
        rejectKyc(phone, "Identity details could not be verified.");
    }

    public void rejectKyc(String phone, String reason) {
        Customer customer = userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
        customer.setKycStatus(KycStatus.REJECTED);
    }

    public List<Customer> getPendingKycRequests() {
        return userRepo.findCustomersByKyc(KycStatus.PENDING);
    }

    public List<Customer> getPendingKycCustomers() {
        return getPendingKycRequests();
    }
}