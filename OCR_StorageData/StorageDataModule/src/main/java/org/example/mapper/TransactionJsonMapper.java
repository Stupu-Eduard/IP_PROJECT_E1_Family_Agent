package org.example.mapper;

import org.example.model.Transaction;

public class TransactionJsonMapper {

    public String toJson(Transaction transaction) {
        if (transaction == null) {
            return "{}";
        }

        return "{"
                + "\"date\":\"" + escape(transaction.getDate()) + "\","
                + "\"amount\":" + transaction.getAmount() + ","
                + "\"description\":\"" + escape(transaction.getDescription()) + "\","
                + "\"type\":\"" + escape(transaction.getType()) + "\","
                + "\"currency\":\"" + escape(transaction.getCurrency()) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
    }
}