package ir.ac.kntu.service;

import ir.ac.kntu.domain.account.Account;
import ir.ac.kntu.domain.account.Transaction;
import ir.ac.kntu.domain.account.TransactionType;
import ir.ac.kntu.domain.transfer.PayaRecord;
import ir.ac.kntu.repository.AccountRepository;
import ir.ac.kntu.repository.PayaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service orchestrating admin batch operations: queued Paya settlement and fund profit payouts.
 */
public class AdminBatchService {
    private static final AtomicLong TRX_COUNTER = new AtomicLong(950001);

    private final PayaRepository payaRepo;
    private final AccountRepository accountRepo;
    private final FundService fundService;

    public AdminBatchService(PayaRepository payaRepo, AccountRepository accountRepo, FundService fundService) {
        this.payaRepo = Objects.requireNonNull(payaRepo, "Paya repo cannot be null.");
        this.accountRepo = Objects.requireNonNull(accountRepo, "Account repo cannot be null.");
        this.fundService = Objects.requireNonNull(fundService, "Fund service cannot be null.");
    }

    public int settlePayaQueue(Instant executionInstant) {
        Instant time = executionInstant != null ? executionInstant : Instant.now();
        List<PayaRecord> queued = payaRepo.findQueued();
        int settled = 0;
        for (PayaRecord rec : queued) {
            if (processSinglePaya(rec, time)) {
                settled++;
            }
        }
        return settled;
    }

    public int distributeFundProfits(Instant executionInstant) {
        Instant time = executionInstant != null ? executionInstant : Instant.now();
        return fundService.payAllMaturedInterests(time);
    }

    public int executeDailySettlement(Instant executionInstant) {
        Instant time = executionInstant != null ? executionInstant : Instant.now();
        int payaCount = settlePayaQueue(time);
        int profitCount = distributeFundProfits(time);
        return payaCount + profitCount;
    }

    private boolean processSinglePaya(PayaRecord record, Instant executionTime) {
        Optional<Account> optDest = accountRepo.findByAccountNumber(record.getDestAcc());
        if (optDest.isEmpty()) {
            record.markRejected(executionTime);
            return false;
        }

        Account destAcc = optDest.get();
        String trxId = "TRX-" + TRX_COUNTER.getAndIncrement();
        String desc = "Paya Batch Settlement (" + record.getPayaId() + ")";
        Transaction creditTrx = new Transaction(trxId, TransactionType.TRANSFER, record.getAmount(),
                0.0, record.getSourceAcc(), destAcc.getAccountNumber(), desc, executionTime);

        destAcc.credit(record.getAmount(), creditTrx);
        record.markProcessed(executionTime);
        return true;
    }
}