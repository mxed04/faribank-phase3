package ir.ac.kntu.ui;

import ir.ac.kntu.domain.config.SystemSettings;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.PayaRepository;
import ir.ac.kntu.repository.SimRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.service.AccountService;
import ir.ac.kntu.service.AdminBatchService;
import ir.ac.kntu.service.AdminCustomerService;
import ir.ac.kntu.service.AdminService;
import ir.ac.kntu.service.AuthService;
import ir.ac.kntu.service.ContactService;
import ir.ac.kntu.service.FinancialReportService;
import ir.ac.kntu.service.FundService;
import ir.ac.kntu.service.JsonStorageService;
import ir.ac.kntu.service.SearchService;
import ir.ac.kntu.service.SettingsService;
import ir.ac.kntu.service.SimService;
import ir.ac.kntu.service.SupportService;
import ir.ac.kntu.service.TicketService;
import ir.ac.kntu.service.TransferService;

/**
 * Service container wiring core infrastructure, SIM services, administrative hierarchy, and persistence.
 */
public class BankServices {
    private AuthService authService;
    private AccountService accountService;
    private TransferService transferService;
    private ContactService contactService;
    private TicketService ticketService;
    private SupportService supportService;
    private SettingsService settingsService;
    private SearchService searchService;
    private FundService fundService;
    private AdminBatchService adminBatchService;
    private AdminCustomerService adminCustService;
    private AdminService adminService;
    private SimService simService;
    private FinancialReportService reportService;
    private JsonStorageService storageService;

    public BankServices() {
        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        ContactRepository contactRepo = new ContactRepository();
        TicketRepository ticketRepo = new TicketRepository();
        FundRepository fundRepo = new FundRepository();
        PayaRepository payaRepo = new PayaRepository();

        initCoreServices(userRepo, accountRepo, ticketRepo);
        initTransferInfrastructure(accountRepo, userRepo, contactRepo, payaRepo);
        initAdministrativeServices(userRepo, accountRepo, fundRepo, payaRepo);
        this.storageService = new JsonStorageService(userRepo, accountRepo, fundRepo, ticketRepo);
    }

    public BankServices(AuthService authService, AccountService accountService,
                        TransferService transferService, ContactService contactService,
                        SettingsService settingsService, TicketService ticketService,
                        SupportService supportService) {
        this.authService = authService;
        this.accountService = accountService;
        this.transferService = transferService;
        this.contactService = contactService;
        this.settingsService = settingsService;
        this.ticketService = ticketService;
        this.supportService = supportService;

        initCustomAuxiliaryServices();
    }

    private void initCoreServices(UserRepository userRepo, AccountRepository accountRepo,
                                  TicketRepository ticketRepo) {
        this.authService = new AuthService(userRepo, accountRepo, ticketRepo);
        this.ticketService = new TicketService(ticketRepo, userRepo, this.authService);
        this.accountService = new AccountService(accountRepo, userRepo);
        this.settingsService = new SettingsService(userRepo);
        this.supportService = new SupportService(userRepo, accountRepo);
        this.adminService = new AdminService(userRepo);
    }

    private void initTransferInfrastructure(AccountRepository accountRepo, UserRepository userRepo,
                                            ContactRepository contactRepo, PayaRepository payaRepo) {
        this.contactService = new ContactService(contactRepo, userRepo);
        this.searchService = new SearchService(userRepo, contactRepo);
        this.transferService = new TransferService(accountRepo, userRepo, contactRepo);
        this.transferService.setPayaRepository(payaRepo);
    }

    private void initAdministrativeServices(UserRepository userRepo, AccountRepository accountRepo,
                                            FundRepository fundRepo, PayaRepository payaRepo) {
        this.fundService = new FundService(fundRepo, accountRepo, userRepo);
        this.transferService.setFundService(this.fundService);
        this.simService = new SimService(new SimRepository(), accountRepo, userRepo, new SystemSettings());
        this.adminBatchService = new AdminBatchService(payaRepo, accountRepo, this.fundService);
        this.adminCustService = new AdminCustomerService(userRepo);
        this.reportService = new FinancialReportService(accountRepo, userRepo);
    }

    private void initCustomAuxiliaryServices() {
        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        FundRepository fundRepo = new FundRepository();
        TicketRepository ticketRepo = new TicketRepository();
        PayaRepository payaRepo = new PayaRepository();

        this.fundService = new FundService(fundRepo, accountRepo, userRepo);
        this.adminService = new AdminService(userRepo);
        this.simService = new SimService(new SimRepository(), accountRepo, userRepo, new SystemSettings());
        this.transferService.setFundService(this.fundService);
        this.transferService.setPayaRepository(payaRepo);
        this.searchService = new SearchService(userRepo, new ContactRepository());
        this.adminBatchService = new AdminBatchService(payaRepo, accountRepo, this.fundService);
        this.adminCustService = new AdminCustomerService(userRepo);
        this.reportService = new FinancialReportService(accountRepo, userRepo);
        this.storageService = new JsonStorageService(userRepo, accountRepo, fundRepo, ticketRepo);
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransferService getTransferService() {
        return transferService;
    }

    public ContactService getContactService() {
        return contactService;
    }

    public TicketService getTicketService() {
        return ticketService;
    }

    public SupportService getSupportService() {
        return supportService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public FundService getFundService() {
        return fundService;
    }

    public AdminBatchService getAdminBatchService() {
        return adminBatchService;
    }

    public AdminCustomerService getAdminCustomerService() {
        return adminCustService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    public SimService getSimService() {
        return simService;
    }

    public FinancialReportService getReportService() {
        return reportService;
    }

    public JsonStorageService getStorageService() {
        return storageService;
    }
}