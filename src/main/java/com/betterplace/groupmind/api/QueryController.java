package com.betterplace.groupmind.api;

import com.betterplace.groupmind.api.dto.QueryRequest;
import com.betterplace.groupmind.api.dto.QueryResponse;
import com.betterplace.groupmind.consensus.ConsensusCluster;
import com.betterplace.groupmind.consensus.ConsensusService;
import com.betterplace.groupmind.ingest.IngestionService;
import com.betterplace.groupmind.retrieve.RetrievalService;
import com.betterplace.groupmind.retrieve.RetrievedMessage;
import com.betterplace.groupmind.synthesis.SynthesisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The single endpoint that wires the RAG pipeline together:
 * retrieve (semantic) → detect consensus → synthesize answer.
 */
@RestController
@RequestMapping("/api")
public class QueryController {

    private final RetrievalService retrieval;
    private final ConsensusService consensus;
    private final SynthesisService synthesis;
    private final IngestionService ingestion;

    public QueryController(RetrievalService retrieval, ConsensusService consensus,
                           SynthesisService synthesis, IngestionService ingestion) {
        this.retrieval = retrieval;
        this.consensus = consensus;
        this.synthesis = synthesis;
        this.ingestion = ingestion;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest req) {
        if (req == null || req.question() == null || req.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String question = req.question().strip();
        int topK = req.topK() != null && req.topK() > 0 ? req.topK() : 0;

        List<RetrievedMessage> retrieved = topK > 0
                ? retrieval.retrieve(question, topK)
                : retrieval.retrieve(question);

        List<ConsensusCluster> clusters = consensus.detect(retrieved);

        SynthesisService.Synthesis result = synthesis.synthesize(question, retrieved, clusters);

        return ResponseEntity.ok(new QueryResponse(
                question, result.answer(), clusters, retrieved, result.llmUsed()));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "messagesIngested", ingestion.ingestedCount());
    }

    /** Manual re-ingest (handy during development). */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() throws Exception {
        int count = ingestion.ingest();
        return Map.of("ingested", count);
    }
}
