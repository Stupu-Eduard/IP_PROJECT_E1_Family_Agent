import java.util.List;

public class Main {
    public static void main(String[] args) {

        // simulare text venit de la OCR
        String ocrText =
                "30/03/2026 TRANSFER ION POPESCU 250.00\n" +
                        "29/03/2026 RETRAGERE ATM 120.50\n" +
                        "28/03/2026 PLATA FACTURA 350.75";

        // creez parserul
        BankStatementParser parser = new BankStatementParser();

        // apelez metoda mea
        List<Transaction> transactions = parser.parseText(ocrText);

        // afisez rezultatul
        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }
}