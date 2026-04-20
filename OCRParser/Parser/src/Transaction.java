import java.time.LocalDate;

public class Transaction {
    private LocalDate date;//reprezinta o data calendaristica
    private String description;//reprezinta descrierea unei tranzactii
    private double amount;//reprezinta o suma de bani in main

    public Transaction(LocalDate date, String description, double amount) {
        this.date = date;
        this.description = description;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + date +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                '}';
    }
}