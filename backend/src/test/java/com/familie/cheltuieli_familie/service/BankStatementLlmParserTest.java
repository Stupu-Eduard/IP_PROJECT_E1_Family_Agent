package com.familie.cheltuieli_familie.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankStatementLlmParserTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private BankStatementLlmParser parser;

    @Test
    void shouldReturnEmptyListForBlankText() {
        List<BankStatementLlmParser.ParsedTransaction> result = parser.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNullText() {
        List<BankStatementLlmParser.ParsedTransaction> result = parser.parse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldParseValidJsonResponse() {
        String json = """
            [
              {"date": "10/03/2025", "description": "Lidl", "amount": 100.50, "currency": "RON", "type": "EXPENSE"},
              {"date": "11/03/2025", "description": "Salary", "amount": 5000.00, "currency": "RON", "type": "INCOME"}
            ]
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        List<BankStatementLlmParser.ParsedTransaction> result = parser.parse("some ocr text");
        assertEquals(2, result.size());
        assertEquals("Lidl", result.get(0).getDescription());
        assertEquals(100.50, result.get(0).getAmount());
        assertEquals("EXPENSE", result.get(0).getType());
        assertEquals("Salary", result.get(1).getDescription());
        assertEquals("INCOME", result.get(1).getType());
    }

    @Test
    void shouldStripMarkdownFences() {
        String json = """
            ```json
            [
              {"date": "10/03/2025", "description": "Lidl", "amount": 100.50, "currency": "RON", "type": "EXPENSE"}
            ]
            ```
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        List<BankStatementLlmParser.ParsedTransaction> result = parser.parse("some ocr text");
        assertEquals(1, result.size());
        assertEquals("Lidl", result.get(0).getDescription());
    }

    @Test
    void shouldReturnEmptyListOnInvalidJson() {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("not json")));

        List<BankStatementLlmParser.ParsedTransaction> result = parser.parse("some ocr text");
        assertTrue(result.isEmpty());
    }
}
