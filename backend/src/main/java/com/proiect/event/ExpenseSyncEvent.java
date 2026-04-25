package com.proiect.event;

import com.proiect.model.ExpenseEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExpenseSyncEvent extends ApplicationEvent {
    private final ExpenseEntity expense;

    public ExpenseSyncEvent(Object source, ExpenseEntity expense) {
        super(source);
        this.expense = expense;
    }
}
