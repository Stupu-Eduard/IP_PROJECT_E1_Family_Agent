package com.familie.cheltuieli_familie.event;

import com.familie.cheltuieli_familie.model.Expense;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExpenseSyncEvent extends ApplicationEvent {
    private final Expense expense;

    public ExpenseSyncEvent(Object source, Expense expense) {
        super(source);
        this.expense = expense;
    }
}
