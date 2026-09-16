package ir.ac.kntu.repository;

import ir.ac.kntu.domain.user.AdminUser;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.domain.user.SupportUser;
import ir.ac.kntu.domain.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe repository managing all customer, support, and admin profiles.
 */
public class UserRepository {
    private final Map<String, Customer> customersByPhone = new ConcurrentHashMap<>();
    private final Map<String, Customer> customersByNatId = new ConcurrentHashMap<>();
    private final Map<String, SupportUser> supportsByUser = new ConcurrentHashMap<>();
    private final Map<String, AdminUser> adminsByUser = new ConcurrentHashMap<>();

    public UserRepository() {
        AdminUser rootAdmin = new AdminUser("System", "Administrator", "admin", "Admin@1234", null);
        adminsByUser.put(rootAdmin.getUsername().toLowerCase(), rootAdmin);
    }

    public synchronized void saveCustomer(Customer customer) {
        customersByPhone.put(customer.getPhoneNumber(), customer);
        customersByNatId.put(customer.getNationalCode(), customer);
    }

    public Optional<Customer> findCustomerByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customersByPhone.get(phone.trim()));
    }

    public Optional<Customer> findCustomerByNationalCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customersByNatId.get(code.trim()));
    }

    public Optional<Customer> findCustomerByNationalId(String nationalId) {
        return findCustomerByNationalCode(nationalId);
    }

    public List<Customer> findCustomersByKyc(KycStatus status) {
        List<Customer> result = new ArrayList<>();
        for (Customer cust : customersByPhone.values()) {
            if (cust.getKycStatus() == status) {
                result.add(cust);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<Customer> getAllCustomers() {
        return List.copyOf(customersByPhone.values());
    }

    public List<Customer> findAllCustomers() {
        return getAllCustomers();
    }

    public synchronized void saveSupport(SupportUser support) {
        supportsByUser.put(support.getUsername().toLowerCase(), support);
    }

    public Optional<SupportUser> findSupportByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supportsByUser.get(username.trim().toLowerCase()));
    }

    public List<SupportUser> findAllSupports() {
        return List.copyOf(supportsByUser.values());
    }

    public synchronized void saveAdmin(AdminUser admin) {
        adminsByUser.put(admin.getUsername().toLowerCase(), admin);
    }

    public Optional<AdminUser> findAdminByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adminsByUser.get(username.trim().toLowerCase()));
    }

    public List<AdminUser> findAllAdmins() {
        return List.copyOf(adminsByUser.values());
    }

    public Optional<User> findAnyUser(String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        String cleanKey = identifier.trim().toLowerCase();
        if (adminsByUser.containsKey(cleanKey)) {
            return Optional.of(adminsByUser.get(cleanKey));
        }
        if (supportsByUser.containsKey(cleanKey)) {
            return Optional.of(supportsByUser.get(cleanKey));
        }
        if (customersByPhone.containsKey(identifier.trim())) {
            return Optional.of(customersByPhone.get(identifier.trim()));
        }
        return Optional.empty();
    }
}