package com.familie.cheltuieli_familie.event;

import com.familie.cheltuieli_familie.model.ExpenseEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExpenseSyncEvent extends ApplicationEvent {
    private final transient ExpenseEntity expense;

    public ExpenseSyncEvent(Object source, ExpenseEntity expense) {
        super(source);
        this.expense = expense;
    }
}
