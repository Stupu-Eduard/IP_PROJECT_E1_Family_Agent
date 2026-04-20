package com.proiect.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AnalyticsAssistant {

    @SystemMessage("""
        You are a family expense analytics assistant. 
        Use the provided tools to answer user questions about their spending habits.
        Always provide clear, concise answers based on the data returned by the tools.
        If the user's question is ambiguous, ask for clarification.
        Today's date is {{current_date}}.
        """)
    String chat(@UserMessage String userMessage);
}
