package com.betterplace.groupmind.api.dto;

import com.betterplace.groupmind.consensus.ConsensusCluster;
import com.betterplace.groupmind.retrieve.RetrievedMessage;

import java.util.List;

/**
 * The full answer to a query: the synthesized prose, the consensus clusters that
 * drive the corroboration signal, the raw citations retrieval pulled, and whether
 * the answer was written by the LLM or the deterministic fallback.
 *
 * @param question   echo of the asked question
 * @param answer     synthesized, human-readable answer
 * @param consensus  ranked consensus clusters (may be empty)
 * @param citations  the retrieved messages the answer is grounded in
 * @param llmUsed    true if Claude generated the prose; false if the templated fallback did
 */
public record QueryResponse(
        String question,
        String answer,
        List<ConsensusCluster> consensus,
        List<RetrievedMessage> citations,
        boolean llmUsed) {
}
