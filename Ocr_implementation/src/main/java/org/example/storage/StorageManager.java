package org.example.storage;

import org.example.db.DatabaseManager;
import org.example.model.Transaction;

import java.util.List;

public class StorageManager implements StorageService
{

    private final DatabaseManager databaseManager;

    public StorageManager(DatabaseManager databaseManager)
    {
        this.databaseManager=databaseManager;
    }

    @Override
    public void save(List<Transaction> transactions)
    {
        databaseManager.connect();

        for (Transaction transaction : transactions)
        {
            String sql = buildInsertStatement(transaction);
            databaseManager.executeInsert(sql);
        }

        databaseManager.disconnect();
    }

    private String buildInsertStatement(Transaction transaction)
    {
       return "INSERT INTO Transactions values: " + transaction.getDate() + ","
               + transaction.getSum() + "," + transaction.getDescription() + ","
               + transaction.getType() + ","  + transaction.getDescription() + ","
               + transaction.getCurrency();

    }
}