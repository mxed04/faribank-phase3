package ir.ac.kntu.ui;

import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.PayaRepository;
import ir.ac.kntu.repository.TicketRepository;
import ir.ac.kntu.repository.UserRepository;
import ir.ac.kntu.service.AccountService;
import ir.ac.kntu.service.AdminBatchService;
import ir.ac.kntu.service.AdminCustomerService;
import ir.ac.kntu.service.AuthService;
import ir.ac.kntu.service.ContactService;
import ir.ac.kntu.service.FinancialReportService;
import ir.ac.kntu.service.FundService;
import ir.ac.kntu.service.SearchService;
import ir.ac.kntu.service.SettingsService;
import ir.ac.kntu.service.SupportService;
import ir.ac.kntu.service.TicketService;
import ir.ac.kntu.service.TransferService;

/**
 * Service container wiring core infrastructure, multi-channel transfers, and financial analytics.
 */
public class BankServices {
    private final AuthService authService;
    private final AccountService accountService;
    private final TransferService transferService;
    private final ContactService contactService;
    private final TicketService ticketService;
    private final SupportService supportService;
    private final SettingsService settingsService;
    private final SearchService searchService;
    private final FundService fundService;
    private final AdminBatchService adminBatchService;
    private final AdminCustomerService adminCustService;
    private final FinancialReportService reportService;

    public BankServices() {
        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        ContactRepository contactRepo = new ContactRepository();
        TicketRepository ticketRepo = new TicketRepository();
        FundRepository fundRepo = new FundRepository();
        PayaRepository payaRepo = new PayaRepository();

        this.ticketService = new TicketService(ticketRepo, userRepo);
        this.authService = new AuthService(userRepo, accountRepo, ticketRepo);
        this.accountService = new AccountService(accountRepo, userRepo);
        this.fundService = new FundService(fundRepo, accountRepo, userRepo);

        this.transferService = new TransferService(accountRepo, userRepo, contactRepo);
        this.transferService.setFundService(this.fundService);
        this.transferService.setPayaRepository(payaRepo);

        this.contactService = new ContactService(contactRepo, userRepo);
        this.supportService = new SupportService(userRepo, accountRepo);
        this.settingsService = new SettingsService(userRepo);
        this.searchService = new SearchService(userRepo, contactRepo);
        this.adminBatchService = new AdminBatchService(payaRepo, accountRepo, this.fundService);
        this.adminCustService = new AdminCustomerService(userRepo);
        this.reportService = new FinancialReportService(accountRepo, userRepo);
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

        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        FundRepository fundRepo = new FundRepository();
        PayaRepository payaRepo = new PayaRepository();

        this.fundService = new FundService(fundRepo, accountRepo, userRepo);
        this.transferService.setFundService(this.fundService);
        this.transferService.setPayaRepository(payaRepo);
        this.searchService = new SearchService(userRepo, new ContactRepository());
        this.adminBatchService = new AdminBatchService(payaRepo, accountRepo, this.fundService);
        this.adminCustService = new AdminCustomerService(userRepo);
        this.reportService = new FinancialReportService(accountRepo, userRepo);
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

    public FinancialReportService getReportService() {
        return reportService;
    }
}