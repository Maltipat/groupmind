package com.betterplace.groupmind.consensus;

import java.util.List;

/**
 * A cluster of retrieved messages that independently point at the same thing
 * (typically a vendor / person), together with how many DISTINCT people backed it.
 *
 * The {@code distinctSenderCount} is the corroboration signal that turns this
 * from search into something that feels smart: "8 residents agree" carries a
 * confidence a single top cosine-similarity hit never could.
 *
 * @param label              the representative token for the entity (e.g. "ramesh")
 * @param distinctSenderCount number of distinct senders who mentioned it
 * @param senders            the distinct sender names
 * @param messageIds         ids of the messages that mention this entity
 */
public record ConsensusCluster(
        String label,
        int distinctSenderCount,
        List<String> senders,
        List<Integer> messageIds) {
}
