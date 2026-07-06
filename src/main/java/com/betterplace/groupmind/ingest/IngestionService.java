package com.betterplace.groupmind.ingest;

import com.betterplace.groupmind.config.GroupMindProperties;
import com.betterplace.groupmind.model.ChatMessage;
import com.betterplace.groupmind.parse.WhatsAppParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads the WhatsApp export into the vector store once, on startup.
 *
 * Chunking strategy: one message = one chunk. Group-chat messages are short and
 * self-contained, so per-message embedding keeps each recommendation attributable
 * to a single sender — which is exactly what the consensus step later needs.
 * (For long-form sources you would split into overlapping windows; here that would
 * only blur sender attribution.)
 */
@Service
public class IngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WhatsAppParser parser;
    private final VectorStore vectorStore;
    private final GroupMindProperties props;
    private final ResourceLoader resourceLoader;
    private final AtomicInteger ingested = new AtomicInteger(0);

    public IngestionService(WhatsAppParser parser, VectorStore vectorStore,
                            GroupMindProperties props, ResourceLoader resourceLoader) {
        this.parser = parser;
        this.vectorStore = vectorStore;
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ingest();
    }

    /** Parse + embed + store. Returns the number of messages ingested. */
    public synchronized int ingest() throws Exception {
        log.info("Ingesting WhatsApp export from {}", props.getDataPath());
        List<ChatMessage> messages;
        try (InputStream in = resourceLoader.getResource(props.getDataPath()).getInputStream()) {
            messages = parser.parse(in);
        }
        log.info("Parsed {} messages; embedding + storing…", messages.size());

        List<Document> docs = new ArrayList<>(messages.size());
        for (ChatMessage msg : messages) {
            docs.add(Document.builder()
                    .id(String.valueOf(msg.id()))
                    .text(msg.text())
                    .metadata(Map.of(
                            "msgId", msg.id(),
                            "sender", msg.sender(),
                            "timestamp", msg.ts().format(ISO)))
                    .build());
        }
        vectorStore.add(docs);
        ingested.set(messages.size());
        log.info("Ingestion complete: {} messages embedded into the vector store.", messages.size());
        return messages.size();
    }

    public int ingestedCount() {
        return ingested.get();
    }
}
