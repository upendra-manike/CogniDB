package com.syntricdb.engine.txn;

import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionManager {
    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    private final AtomicLong txnIdCounter = new AtomicLong(1000);
    private final AtomicLong globalTimestamp = new AtomicLong(1);
    private final Map<Long, Transaction> activeTransactions = new ConcurrentHashMap<>();
    private final Map<String, Long> keyLocks = new ConcurrentHashMap<>();

    public Transaction beginTransaction() {
        long id = txnIdCounter.incrementAndGet();
        long ts = globalTimestamp.get();
        Transaction txn = new Transaction(id, ts);
        activeTransactions.put(id, txn);
        log.info("Began Transaction [TxnID={}] at ReadTimestamp={}", id, ts);
        return txn;
    }

    public synchronized boolean commitTransaction(Transaction txn, StorageEngine storageEngine, String tableName) {
        if (txn.getState() != Transaction.TxnState.ACTIVE) {
            return false;
        }

        // Check for Write-Write conflict
        for (String key : txn.getWriteKeys()) {
            Long lockingTxn = keyLocks.get(key);
            if (lockingTxn != null && !lockingTxn.equals(txn.getTxnId())) {
                log.warn("Write-Write Conflict detected on key '{}' for TxnID={}. Aborting transaction.", key, txn.getTxnId());
                abortTransaction(txn);
                return false;
            }
        }

        // Lock & Apply writes
        try {
            for (Map.Entry<String, Object> entry : txn.getUncommittedModifications().entrySet()) {
                keyLocks.put(entry.getKey(), txn.getTxnId());
                if (entry.getValue() instanceof Tuple) {
                    storageEngine.insert(tableName, (Tuple) entry.getValue());
                }
            }

            txn.setState(Transaction.TxnState.COMMITTED);
            globalTimestamp.incrementAndGet();
            log.info("Committed Transaction [TxnID={}]", txn.getTxnId());
            return true;

        } catch (Exception e) {
            log.error("Commit failed for TxnID=" + txn.getTxnId(), e);
            abortTransaction(txn);
            return false;

        } finally {
            for (String key : txn.getWriteKeys()) {
                keyLocks.remove(key);
            }
            activeTransactions.remove(txn.getTxnId());
        }
    }

    public synchronized void abortTransaction(Transaction txn) {
        txn.setState(Transaction.TxnState.ABORTED);
        txn.getUncommittedModifications().clear();
        for (String key : txn.getWriteKeys()) {
            keyLocks.remove(key);
        }
        activeTransactions.remove(txn.getTxnId());
        log.info("Aborted Transaction [TxnID={}]", txn.getTxnId());
    }

    public Transaction getTransaction(long txnId) {
        return activeTransactions.get(txnId);
    }
}
