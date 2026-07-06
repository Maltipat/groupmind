package com.betterplace.groupmind.retrieve;

/**
 * A single message returned by semantic retrieval, flattened from the vector
 * store's {@code Document} + metadata into the shape the rest of the app uses.
 *
 * @param id        the message id (stable, assigned at parse time)
 * @param sender    who sent it
 * @param timestamp ISO-8601 timestamp string
 * @param text      the message body
 * @param score     similarity score from the vector store (higher = closer)
 */
public record RetrievedMessage(
        int id,
        String sender,
        String timestamp,
        String text,
        double score) {
}
