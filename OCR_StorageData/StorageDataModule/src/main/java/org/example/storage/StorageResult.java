package org.example.storage;

public class StorageResult {
    private int totalTransactions;
    private int savedTransactions;
    private int failedTransactions;

    public StorageResult(int totalTransactions, int savedTransactions, int failedTransactions) {
        this.totalTransactions = totalTransactions;
        this.savedTransactions = savedTransactions;
        this.failedTransactions = failedTransactions;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public int getSavedTransactions() {
        return savedTransactions;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    @Override
    public String toString() {
        return "StorageResult{" + "total Transactions=" + totalTransactions +
                ", saved Transactions=" + savedTransactions + ", failed Transactions=" + failedTransactions + '}';
    }
}