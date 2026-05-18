package com.familie.cheltuieli_familie.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BankingDictionaryCorrector {

    private static final Set<String> DICTIONARY = new HashSet<>(Arrays.asList(
            "EXTRAS", "CONT", "NUMARUL", "VALUTA", "IBAN", "BIC", "INFORMATII", "SWIFT",
            "DATA", "DESCRIERE", "DEBIT", "CREDIT", "SOLD", "ANTERIOR",
            "FINAL", "TOTAL", "DISPONIBIL", "RULAJ", "ZI",
            "TRANSFER", "PLATA", "INCASARE", "COMISION", "DOBANDA",
            "RETRAGERE", "DEPUNERE", "VIRAMENT", "ORDIN", "CHITANTA",
            "TRANZACTIE", "OPERATIUNE", "BENEFICIAR", "ORDONATOR",
            "REFERINTA", "CARD", "ATM", "POS",
            "PENTRU", "CATRE", "DELA", "PRIN", "CU", "IN", "LA", "DE",
            "SI", "SAU", "DIN", "SPRE", "PANA", "DUPA", "INAINTE",
            "FONDURI", "PROPRII", "NEUTILIZAT", "GARANTARE", "DEPOZIT",
            "BANCARE", "BANCA", "CLIENTI", "PERSOANE", "FIZICE",
            "JURIDICE", "INFORMATII", "GENERALE", "CONDITII",
            "RON", "EUR", "USD", "CHF", "EMAG"
    ));

    private int levenshtein(String a, String b) {
        return com.familie.cheltuieli_familie.util.StringSimilarityUtil.levenshtein(a, b);
    }

    private String findClosest(String word) {
        String closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (String dictWord : DICTIONARY) {
            int distance = levenshtein(word, dictWord);
            int maxAllowed = word.length() <= 5 ? 1 : 2;
            if (distance < minDistance && distance <= maxAllowed) {
                minDistance = distance;
                closest = dictWord;
            }
        }
        return closest;
    }

    private String matchCase(String original, String dictWord) {
        if (original.equals(original.toUpperCase())) return dictWord.toUpperCase();
        if (original.equals(original.toLowerCase())) return dictWord.toLowerCase();
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(dictWord.charAt(0)) +
                    dictWord.substring(1).toLowerCase();
        }
        return dictWord.toLowerCase();
    }

    private String correctWord(String word) {
        if (word == null || word.isEmpty()) return word;
        if (word.matches("\\d++(?:[.,]\\d++)*+")) return word;
        if (word.matches("\\d{2}/\\d{2}/\\d{4}")) return word;
        if (word.matches("[A-Z]{2}\\d{2}[A-Z\\d]+")) return word;
        if (word.length() <= 2) return word;
        String upperWord = word.toUpperCase();
        if (DICTIONARY.contains(upperWord)) return word;
        String closest = findClosest(upperWord);
        if (closest == null) return word;
        return matchCase(word, closest);
    }

    private String correctLine(String line) {
        String[] words = line.split(" ");
        StringBuilder correctedLine = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String correctedWord = correctWord(word);
            correctedLine.append(correctedWord);
            if (i < words.length - 1) correctedLine.append(" ");
        }
        return correctedLine.toString();
    }

    public String correctText(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder corrected = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            corrected.append(correctLine(line)).append("\n");
        }
        return corrected.toString();
    }
}