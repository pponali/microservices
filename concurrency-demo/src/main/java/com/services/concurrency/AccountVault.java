package com.services.concurrency;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pattern #6 (Mutex) + #11 (Try-Lock).
 *
 * <p>Bank account vault with debits guarded by a {@link ReentrantLock}.
 * The trickier debit method uses {@link ReentrantLock#tryLock(long, TimeUnit)}
 * to fail fast under contention rather than queuing forever.
 */
public class AccountVault {

    private final Map<Long, BigDecimal> balances = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public AccountVault credit(long accountId, BigDecimal amount) {
        lock.lock();
        try {
            balances.merge(accountId, amount, BigDecimal::add);
            return this;
        } finally {
            lock.unlock();
        }
    }

    public BigDecimal balance(long accountId) {
        lock.lock();
        try {
            return balances.getOrDefault(accountId, BigDecimal.ZERO);
        } finally {
            lock.unlock();
        }
    }

    /** Blocking debit — waits forever for the lock. */
    public BigDecimal debit(long accountId, BigDecimal amount) {
        lock.lock();
        try {
            BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
            if (current.compareTo(amount) < 0) throw new IllegalStateException("insufficient funds");
            BigDecimal next = current.subtract(amount);
            balances.put(accountId, next);
            return next;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Try-lock debit — gives up after {@code timeoutMs} and returns {@code null}
     * instead of holding up the caller thread.
     */
    public BigDecimal tryDebit(long accountId, BigDecimal amount, long timeoutMs)
            throws InterruptedException {
        if (!lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            return null;   // contention — caller picks fallback path
        }
        try {
            BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
            if (current.compareTo(amount) < 0) throw new IllegalStateException("insufficient funds");
            BigDecimal next = current.subtract(amount);
            balances.put(accountId, next);
            return next;
        } finally {
            lock.unlock();
        }
    }
}
