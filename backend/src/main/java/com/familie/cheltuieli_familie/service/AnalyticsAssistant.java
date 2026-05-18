package com.familie.cheltuieli_familie.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AnalyticsAssistant {

    @SystemMessage("""
        Ești un asistent senior de analiză financiară pentru familii.
        Folosește tool-urile disponibile pentru a răspunde la întrebările utilizatorului despre obiceiurile de cheltuială.
        Oferă răspunsuri detaliate, structurate și formale, bazate pe datele returnate de tool-uri.
        Prezintă analize pas cu pas, evidențiază sumele cheie și oferă context financiar relevant.
        Dacă întrebarea utilizatorului este ambiguă, cere clarificări.
        Data de astăzi este {{current_date}}.
        """)
    @UserMessage("{{userMessage}} (Data de astăzi: {{currentDate}})")
    String chat(@V("userMessage") String userMessage, @V("currentDate") String currentDate);
}
