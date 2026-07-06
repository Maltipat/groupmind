package com.betterplace.groupmind.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Vector store wiring.
 *
 * Default (no "pgvector" profile): an in-memory {@link SimpleVectorStore}. Zero
 * external infrastructure — ideal for a hackathon demo. Embeddings come from the
 * local ONNX model provided by spring-ai-starter-model-transformers.
 *
 * With the "pgvector" profile active (and the Maven -Ppgvector build), Spring AI's
 * pgvector starter auto-configures a PgVectorStore from application-pgvector.yml,
 * and the bean below is switched off. Nothing else in the app changes: everything
 * depends only on the {@link VectorStore} interface.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    @Profile("!pgvector")
    public VectorStore inMemoryVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
