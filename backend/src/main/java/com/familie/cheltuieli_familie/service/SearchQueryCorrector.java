package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class SearchQueryCorrector {

    private final JdbcTemplate jdbcTemplate;

    public SearchQueryCorrector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Corrects a user query by checking each word against the existing expense vocabulary
     * (categories, locations, persons, raw_input words) using Levenshtein distance.
     */
    public String correctQuery(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        Set<String> dictionary = loadDictionary();
        if (dictionary.isEmpty()) {
            return query;
        }

        String[] words = query.split("\\s+");
        StringBuilder corrected = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String fixed = correctWord(word, dictionary);
            corrected.append(fixed);
            if (i < words.length - 1) {
                corrected.append(" ");
            }
        }

        String result = corrected.toString();
        if (!result.equals(query)) {
            log.info("Corrected query from '{}' to '{}'", query, result);
        }
        return result;
    }

    private Set<String> loadDictionary() {
        Set<String> dict = new HashSet<>();
        try {
            List<String> categories = jdbcTemplate.queryForList(
                    "SELECT DISTINCT name FROM categories WHERE name IS NOT NULL", String.class);
            List<String> locations = jdbcTemplate.queryForList(
                    "SELECT DISTINCT store FROM locations WHERE store IS NOT NULL", String.class);
            List<String> persons = jdbcTemplate.queryForList(
                    "SELECT DISTINCT name FROM users WHERE name IS NOT NULL", String.class);

            dict.addAll(categories);
            dict.addAll(locations);
            dict.addAll(persons);

            // Also add common expense-related Romanian words
            dict.addAll(Set.of(
                    "mâncare", "mancare", "transport", "benzina", "utilitati", "utilități",
                    "diverse", "divertisment", "sanatate", "sănătate", "haine", "educatie",
                    "educație", "locuinta", "locuință", "telefon", "internet", "vacanta",
                    "vacanță", "cadouri", "alimente", "supermarket", "kaufland", "lidl",
                    "mega image", "auchan", "carrefour", "penny", "profi", "omv", "petrom",
                    "rompetrol", "mol", "shell", "benzinarie", "restaurant", "cafea",
                    "farmacia", "catena", "sensiblu", "dona", "dm", "sephora", "zara",
                    "h&m", "bershka", "stradivarius", "pull and bear", "electrocasnice",
                    "emag", "altex", "media galaxy", "flanco", "dedeman", "bricostore",
                    "praktiker", "leroy merlin", "ikea", "jysk", "decathlon", "intersport",
                    "carturesti", "librarie", "librărie", "cinema", "teatru", "bilet",
                    "abonament", "rate", "credit", "chirie", "intretinere", "întreținere"
            ));
        } catch (Exception e) {
            log.warn("Failed to load expense dictionary for query correction: {}", e.getMessage());
        }
        return dict;
    }

    private String correctWord(String word, Set<String> dictionary) {
        if (word == null || word.length() <= 2) {
            return word;
        }
        String lower = word.toLowerCase();
        if (dictionary.contains(lower) || dictionary.contains(word)) {
            return word;
        }

        String closest = null;
        int minDistance = Integer.MAX_VALUE;
        int maxAllowed = word.length() <= 5 ? 1 : 2;

        for (String dictWord : dictionary) {
            int distance = levenshtein(lower, dictWord.toLowerCase());
            if (distance < minDistance && distance <= maxAllowed) {
                minDistance = distance;
                closest = dictWord;
            }
        }

        if (closest != null) {
            // Preserve original case pattern
            return matchCase(word, closest);
        }
        return word;
    }

    private String matchCase(String original, String dictWord) {
        if (original.equals(original.toUpperCase())) {
            return dictWord.toUpperCase();
        }
        if (original.equals(original.toLowerCase())) {
            return dictWord.toLowerCase();
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(dictWord.charAt(0)) + dictWord.substring(1).toLowerCase();
        }
        return dictWord.toLowerCase();
    }

    private int levenshtein(String a, String b) {
        int[] dp = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) dp[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = dp[j];
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? prev
                        : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                prev = temp;
            }
        }
        return dp[b.length()];
    }
}
