package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.CreditCard;
import ir.ac.kntu.domain.account.TransferReceipt;
import ir.ac.kntu.domain.fund.FundType;
import ir.ac.kntu.domain.fund.RemainingFund;
import ir.ac.kntu.domain.transfer.PayaRecord;
import ir.ac.kntu.domain.transfer.PayaStatus;
import ir.ac.kntu.domain.user.Customer;
import ir.ac.kntu.domain.user.KycStatus;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.ContactRepository;
import ir.ac.kntu.repository.FundRepository;
import ir.ac.kntu.repository.PayaRepository;
import ir.ac.kntu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MultiChannelTransferTest {
    private AccountRepository accountRepo;
    private UserRepository userRepo;
    private ContactRepository contactRepo;
    private FundRepository fundRepo;
    private FundService fundService;
    private PayaRepository payaRepo;
    private TransferService transferService;

    private Customer alice;
    private Customer bob;
    private Account accAlice;
    private Account accBob;

    @BeforeEach
    void setUp() {
        accountRepo = new AccountRepository();
        userRepo = new UserRepository();
        contactRepo = new ContactRepository();
        fundRepo = new FundRepository();
        payaRepo = new PayaRepository();

        fundService = new FundService(fundRepo, accountRepo, userRepo);
        transferService = new TransferService(accountRepo, userRepo, contactRepo);
        transferService.setFundService(fundService);
        transferService.setPayaRepository(payaRepo);

        alice = new Customer("Alice", "Brown", "09121110001", "1000000001", "Pass@1234");
        alice.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(alice);

        bob = new Customer("Bob", "Green", "09121110002", "1000000002", "Pass@1234");
        bob.setKycStatus(KycStatus.APPROVED);
        userRepo.saveCustomer(bob);

        accAlice = new Account("10011", alice.getPhoneNumber(), new CreditCard("6037100011112222"));
        accAlice.deposit(500000.0);
        alice.setAccount(accAlice);
        accountRepo.save(accAlice);

        accBob = new Account("10022", bob.getPhoneNumber(), new CreditCard("6037200033334444"));
        accBob.deposit(100000.0);
        bob.setAccount(accBob);
        accountRepo.save(accBob);
    }

    @Test
    void testCardToCardWithFixed300Fee() {
        TransferReceipt receipt = transferService.transferCardToCard(alice.getPhoneNumber(), "6037200033334444", 10000.0);
        assertNotNull(receipt);
        assertEquals(300.0, receipt.getFee(), 0.001);
        assertEquals(489700.0, accAlice.getBalance(), 0.001);
        assertEquals(110000.0, accBob.getBalance(), 0.001);
    }

    @Test
    void testPolTransferWithTwoPercentFee() {
        TransferReceipt receipt = transferService.transferPol(alice.getPhoneNumber(), accBob.getAccountNumber(), 50000.0);
        assertNotNull(receipt);
        assertEquals(1000.0, receipt.getFee(), 0.001);
        assertEquals(449000.0, accAlice.getBalance(), 0.001);
        assertEquals(150000.0, accBob.getBalance(), 0.001);
    }

    @Test
    void testFariToFariTransferFreeFee() {
        TransferReceipt receipt = transferService.transferFariToFari(alice.getPhoneNumber(), accBob.getAccountNumber(), 25000.0);
        assertNotNull(receipt);
        assertEquals(0.0, receipt.getFee(), 0.001);
        assertEquals(475000.0, accAlice.getBalance(), 0.001);
        assertEquals(125000.0, accBob.getBalance(), 0.001);
    }

    @Test
    void testPayaEnqueuesTransactionAndDeductsFixed2000Fee() {
        TransferReceipt receipt = transferService.transferPaya(alice.getPhoneNumber(), accBob.getAccountNumber(), 40000.0);
        assertNotNull(receipt);
        assertEquals(2000.0, receipt.getFee(), 0.001);

        assertEquals(458000.0, accAlice.getBalance(), 0.001);
        assertEquals(100000.0, accBob.getBalance(), 0.001);

        List<PayaRecord> queued = payaRepo.findQueued();
        assertEquals(1, queued.size());
        assertEquals(PayaStatus.QUEUED, queued.get(0).getStatus());
        assertEquals(40000.0, queued.get(0).getAmount());
    }

    @Test
    void testMicroSavingsAutoTriggeredOnTransfer() {
        fundService.openRemainingFund(alice.getPhoneNumber(), 5000.0);

        transferService.transferFariToFari(alice.getPhoneNumber(), accBob.getAccountNumber(), 12340.0);

        RemainingFund remFund = (RemainingFund) fundRepo.findByOwnerAndType(alice.getPhoneNumber(), FundType.REMAINING).orElseThrow();
        assertEquals(6755.0, remFund.getBalance(), 0.001);
    }
}