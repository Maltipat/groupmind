package com.betterplace.groupmind.retrieve;

import com.betterplace.groupmind.config.GroupMindProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Semantic retrieval over the ingested messages.
 *
 * This is the "R" in RAG and the reason the whole thing works: WhatsApp's own
 * keyword search can't connect "aircon technician" to "cooling repair guy",
 * but an embedding search matches on <em>meaning</em>, so all the ways people
 * phrased the same recommendation come back together — which is precisely what
 * the consensus step needs to count distinct backers.
 */
@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final GroupMindProperties props;

    public RetrievalService(VectorStore vectorStore, GroupMindProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    /** Retrieve the top-k semantically similar messages for a query. */
    public List<RetrievedMessage> retrieve(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);
        if (docs == null) {
            return List.of();
        }

        List<RetrievedMessage> out = new ArrayList<>(docs.size());
        for (Document d : docs) {
            Map<String, Object> md = d.getMetadata();
            out.add(new RetrievedMessage(
                    asInt(md.get("msgId")),
                    asString(md.get("sender")),
                    asString(md.get("timestamp")),
                    d.getText(),
                    d.getScore() == null ? 0.0 : d.getScore()));
        }
        return out;
    }

    /** Convenience overload using the configured default top-k. */
    public List<RetrievedMessage> retrieve(String query) {
        return retrieve(query, props.getTopK());
    }

    private static int asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o == null) return -1;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String asString(Object o) {
        return o == null ? "" : o.toString();
    }
}
