package com.betterplace.groupmind.api.dto;

/**
 * Incoming query.
 *
 * @param question the natural-language question, e.g. "who do people recommend for AC repair?"
 * @param topK     optional override for how many messages to retrieve (null = use configured default)
 */
public record QueryRequest(String question, Integer topK) {
}
