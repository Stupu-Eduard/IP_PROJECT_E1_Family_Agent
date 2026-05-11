package com.familie.cheltuieli_familie.config;

import com.familie.cheltuieli_familie.service.VisualIntentExtractor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LlmConfigTest {

    @InjectMocks
    private LlmConfig llmConfig;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Test
    void visualIntentExtractor_shouldCreateBeanWithProvidedModel() {
        VisualIntentExtractor extractor = llmConfig.visualIntentExtractor(chatLanguageModel);
        assertNotNull(extractor);
    }
}
