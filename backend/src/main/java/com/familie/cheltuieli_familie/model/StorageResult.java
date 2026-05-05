package com.familie.cheltuieli_familie.model;

public class StorageResult {
    private int totalTransactions;
    private int savedTransactions;
    private int failedTransactions;

    public StorageResult() {
    }

    public StorageResult(int totalTransactions, int savedTransactions, int failedTransactions) {
        this.totalTransactions = totalTransactions;
        this.savedTransactions = savedTransactions;
        this.failedTransactions = failedTransactions;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getSavedTransactions() {
        return savedTransactions;
    }

    public void setSavedTransactions(int savedTransactions) {
        this.savedTransactions = savedTransactions;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(int failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    @Override
    public String toString() {
        return "StorageResult{" + "totalTransactions=" + totalTransactions +
                ", savedTransactions=" + savedTransactions +
                ", failedTransactions=" + failedTransactions + '}';
    }
}
