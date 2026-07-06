package com.betterplace.groupmind.model;

import java.time.LocalDateTime;

/**
 * One parsed WhatsApp message.
 *
 * @param id     stable sequence id assigned at parse time (also used as vector-store doc id)
 * @param ts     message timestamp
 * @param sender display name of the sender
 * @param text   message body (multi-line messages are joined with '\n')
 */
public
record ChatMessage(int id, LocalDateTime ts, String sender, String text) {}
