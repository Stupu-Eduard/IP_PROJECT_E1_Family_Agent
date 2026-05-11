package com.familie.cheltuieli_familie.config;

import com.familie.cheltuieli_familie.service.VisualIntentExtractor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmConfigTest {

    @InjectMocks
    private LlmConfig llmConfig;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Test
    void visualIntentExtractor_shouldCreateBean() {
        VisualIntentExtractor extractor = llmConfig.visualIntentExtractor(chatLanguageModel);
        assertNotNull(extractor);
    }

    @Test
    void visualIntentExtractor_shouldUseProvidedModel() {
        VisualIntentExtractor extractor = llmConfig.visualIntentExtractor(chatLanguageModel);
        assertNotNull(extractor);
        // The extractor is created with the model; we can't easily verify internals
        // but we can verify it doesn't throw and returns a non-null object
    }
}
