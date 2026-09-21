package ir.ac.kntu.service;

import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service providing administrative oversight, multi-filter customer search, and profile updates.
 */
public class AdminCustomerService {
    private final UserRepository userRepo;

    public AdminCustomerService(UserRepository userRepo) {
        this.userRepo = Objects.requireNonNull(userRepo, "User repo cannot be null.");
    }

    public List<Customer> searchCustomers(String query, KycStatus kycFilter, Boolean blockFilter) {
        List<Customer> allCustomers = userRepo.findAllCustomers();
        List<Customer> matches = new ArrayList<>();

        for (Customer cust : allCustomers) {
            if (matchesCriteria(cust, query, kycFilter, blockFilter)) {
                matches.add(cust);
            }
        }
        return List.copyOf(matches);
    }

    public void setCustomerBlocked(String phone, boolean blocked) {
        Customer cust = getCustomer(phone);
        cust.setBlocked(blocked);
    }

    public void updateCustomerProfile(String phone, String firstName, String lastName) {
        Customer cust = getCustomer(phone);
        cust.setFirstName(firstName);
        cust.setLastName(lastName);
    }

    private boolean matchesCriteria(Customer cust, String query, KycStatus kyc, Boolean blocked) {
        if (kyc != null && cust.getKycStatus() != kyc) {
            return false;
        }
        if (blocked != null && cust.isBlocked() != blocked) {
            return false;
        }
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String cleanQuery = query.trim().toLowerCase();
        return cust.getPhoneNumber().contains(cleanQuery)
                || cust.getNationalCode().contains(cleanQuery)
                || cust.getFirstName().toLowerCase().contains(cleanQuery)
                || cust.getLastName().toLowerCase().contains(cleanQuery);
    }

    private Customer getCustomer(String phone) {
        return userRepo.findCustomerByPhone(phone)
                .orElseThrow(() -> new ValidationException("Customer not found: " + phone));
    }
}