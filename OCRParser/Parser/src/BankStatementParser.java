import java.util.*;
import java.util.regex.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BankStatementParser {

    // Metoda principala: primeste text brut de la OCR si returneaza tranzactii structurate
    public List<Transaction> parseText(String ocrText) {

        List<Transaction> transactions = new ArrayList<>();

        // format standard pentru data din extrasul bancar
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // imparțesc textul pe linii (fiecare linie poate fi o tranzactie)
        String[] lines = ocrText.split("\\r?\\n");

        System.out.println("=== START PARSARE OCR ===");

        for (String line : lines) {
            line = line.trim();

            // ignor liniile goale
            if (line.isEmpty()) {
                continue;
            }

            // corectie simpla erori OCR (ex: O → 0)
            line = line.replace("O", "0");

            // verific daca linia contine o dată (probabil tranzactie valida)
            if (!line.matches(".*\\d{1,2}/\\d{1,2}/\\d{4}.*")) {
                continue;
            }
            try {
                // extrag data
                String dateStr = line.substring(0, 10);
                LocalDate date = LocalDate.parse(dateStr, formatter);

                // extrag suma
                Matcher matcher = Pattern.compile("(\\d+[.,]?\\d*)\\s*$").matcher(line);

                if (!matcher.find()) {
                    continue; // dacă nu gasim suma, ignoram linia
                }

                String amountStr = matcher.group(1);
                double amount = Double.parseDouble(amountStr.replace(",", "."));

                // validez suma
                if (amount <= 0) {
                    continue;
                }

                // extrag descrierea
                String description = line
                        .substring(10, line.lastIndexOf(amountStr))
                        .trim();

                System.out.println("Tranzactie: " + date + " | " + description + " | " + amount);

                // creez obiectul Transaction
                transactions.add(new Transaction(date, description, amount));

            } catch (Exception e) {
                // dacă linia e corupta, o ignoram fara sa oprim programul
                System.out.println("Linie ignorata (eroare OCR): " + line);
            }
        }

        System.out.println("=== FINAL PARSARE. Total tranzactii: " + transactions.size() + " ===");

        return transactions;
    }
}
