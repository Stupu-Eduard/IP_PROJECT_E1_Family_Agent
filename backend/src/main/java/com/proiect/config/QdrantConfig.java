package com.proiect.config;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Bean
    public QdrantEmbeddingStore embeddingStore() {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(6334) // gRPC port
                .collectionName("expenses")
                .build();
    }
}
