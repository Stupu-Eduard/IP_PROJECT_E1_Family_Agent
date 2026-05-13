package com.familie.cheltuieli_familie.config;

import com.familie.cheltuieli_familie.service.AnalyticsAssistant;
import com.familie.cheltuieli_familie.service.ExpenseTools;
import com.familie.cheltuieli_familie.service.QdrantContentRetriever;
import com.familie.cheltuieli_familie.service.VisualIntentExtractor;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LlmConfigTest {

    private LlmConfig llmConfig;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private QdrantContentRetriever qdrantContentRetriever;

    @Mock
    private ExpenseTools expenseTools;

    @BeforeEach
    void setUp() {
        llmConfig = new LlmConfig();
    }

    @Test
    void visualIntentExtractor_shouldCreateBeanWithProvidedModel() {
        ReflectionTestUtils.setField(llmConfig, "intentMaxRetries", 3);
        ReflectionTestUtils.setField(llmConfig, "retryDelaysMsStr", "2000,4000");
        ReflectionTestUtils.setField(llmConfig, "defaultGroupBy", "category");
        ReflectionTestUtils.setField(llmConfig, "defaultSeriesBy", "person");
        VisualIntentExtractor extractor = llmConfig.visualIntentExtractor(chatLanguageModel);
        assertNotNull(extractor);
    }

    @Test
    void deepseekModel_shouldCreateBeanWithDeepseekKey() {
        ReflectionTestUtils.setField(llmConfig, "deepseekApiKey", "sk-test-deepseek");
        ReflectionTestUtils.setField(llmConfig, "deepseekBaseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(llmConfig, "deepseekModelName", "deepseek-chat");
        ReflectionTestUtils.setField(llmConfig, "temperature", 0.1);
        ReflectionTestUtils.setField(llmConfig, "timeoutSeconds", 60L);
        ChatLanguageModel model = llmConfig.deepseekModel();
        assertNotNull(model);
    }

    @Test
    void deepseekModel_shouldCreateBeanWithOpenRouterKey() {
        ReflectionTestUtils.setField(llmConfig, "deepseekApiKey", "");
        ReflectionTestUtils.setField(llmConfig, "openRouterApiKey", "sk-test-openrouter");
        ReflectionTestUtils.setField(llmConfig, "openRouterBaseUrl", "https://openrouter.ai/api/v1");
        ReflectionTestUtils.setField(llmConfig, "openRouterModelName", "deepseek/deepseek-chat");
        ReflectionTestUtils.setField(llmConfig, "temperature", 0.1);
        ReflectionTestUtils.setField(llmConfig, "timeoutSeconds", 60L);
        ChatLanguageModel model = llmConfig.deepseekModel();
        assertNotNull(model);
    }

    @Test
    void deepseekModel_shouldThrowWhenNoKeyConfigured() {
        ReflectionTestUtils.setField(llmConfig, "deepseekApiKey", "");
        ReflectionTestUtils.setField(llmConfig, "openRouterApiKey", "");
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> llmConfig.deepseekModel());
        assertEquals("DEEPSEEK_API_KEY or OPENROUTER_API_KEY is required.", exception.getMessage());
    }

    @Test
    void retrievalAugmentor_shouldCreateBean() {
        RetrievalAugmentor augmentor = llmConfig.retrievalAugmentor(qdrantContentRetriever);
        assertNotNull(augmentor);
    }

    @Test
    void ragAssistant_shouldCreateBean() {
        LlmConfig.RagAssistant assistant = llmConfig.ragAssistant(chatLanguageModel, llmConfig.retrievalAugmentor(qdrantContentRetriever));
        assertNotNull(assistant);
    }

    @Test
    void analyticsAssistant_shouldCreateBean() {
        AnalyticsAssistant assistant = llmConfig.analyticsAssistant(chatLanguageModel, expenseTools);
        assertNotNull(assistant);
    }

    @Test
    void reportAssistant_shouldCreateBean() {
        LlmConfig.ReportAssistant assistant = llmConfig.reportAssistant(chatLanguageModel);
        assertNotNull(assistant);
    }
}
