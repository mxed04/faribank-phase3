package ir.ac.kntu.service;

import ir.ac.kntu.util.Calendar;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Background concurrency scheduler for automated fund interest distribution.
 */
public class InterestSchedulerService {
    private final FundService fundService;
    private final Supplier<Instant> timeSource;
    private final AtomicInteger cycleCount;
    private ScheduledExecutorService scheduler;
    private boolean active;

    public InterestSchedulerService(FundService fundService) {
        this(fundService, Calendar::now);
    }

    public InterestSchedulerService(FundService fundService, Supplier<Instant> timeSource) {
        this.fundService = Objects.requireNonNull(fundService, "Fund service is required.");
        this.timeSource = timeSource != null ? timeSource : Calendar::now;
        this.cycleCount = new AtomicInteger(0);
        this.active = false;
    }

    public synchronized void start(long delayMs, long periodMs) {
        if (active) {
            return;
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.active = true;
        this.scheduler.scheduleAtFixedRate(this::executeCycleSafely, delayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public int runCycle() {
        Instant currentTime = timeSource.get();
        int distributed = fundService.payAllMaturedInterests(currentTime);
        cycleCount.incrementAndGet();
        return distributed;
    }

    public synchronized void stop() {
        if (!active || scheduler == null) {
            return;
        }
        active = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean isActive() {
        return active;
    }

    public int getCycleCount() {
        return cycleCount.get();
    }

    private void executeCycleSafely() {
        try {
            runCycle();
        } catch (Exception exception) {
            // Protect background worker from unexpected errors
        }
    }
}