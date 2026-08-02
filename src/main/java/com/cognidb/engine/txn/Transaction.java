package com.cognidb.engine.txn;

import java.util.*;

public class Transaction {
    public enum TxnState {
        ACTIVE,
        COMMITTED,
        ABORTED
    }

    private final long txnId;
    private final long readTimestamp;
    private volatile TxnState state;
    private final Set<String> writeKeys = new HashSet<>();
    private final Map<String, Object> uncommittedModifications = new HashMap<>();

    public Transaction(long txnId, long readTimestamp) {
        this.txnId = txnId;
        this.readTimestamp = readTimestamp;
        this.state = TxnState.ACTIVE;
    }

    public void recordWrite(String key, Object value) {
        writeKeys.add(key);
        uncommittedModifications.put(key, value);
    }

    public long getTxnId() { return txnId; }
    public long getReadTimestamp() { return readTimestamp; }
    public TxnState getState() { return state; }
    public void setState(TxnState state) { this.state = state; }
    public Set<String> getWriteKeys() { return writeKeys; }
    public Map<String, Object> getUncommittedModifications() { return uncommittedModifications; }
}
