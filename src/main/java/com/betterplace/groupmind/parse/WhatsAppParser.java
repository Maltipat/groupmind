package com.betterplace.groupmind.parse;

import com.betterplace.groupmind.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a native WhatsApp export ("DD/MM/YY, HH:MM - Sender: message").
 *
 * Handles the two real-world wrinkles that break naive line-by-line parsing:
 *   1. Multi-line messages — continuation lines have no header and are appended
 *      to the message in progress.
 *   2. System lines — "… created group", "… added X", the E2E-encryption notice,
 *      "… joined using this group's invite link" have no "Sender: " and are skipped.
 *
 * This logic is byte-for-byte the algorithm verified standalone against the
 * synthetic dataset before this class was written.
 */
@Component
public class WhatsAppParser {

    private static final Pattern HEADER =
            Pattern.compile("^(\\d{2}/\\d{2}/\\d{2}), (\\d{2}:\\d{2}) - (.*)$");
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    public List<ChatMessage> parse(InputStream in) throws IOException {
        List<ChatMessage> out = new ArrayList<>();
        int idSeq = 0;

        StringBuilder body = null;      // message-in-progress buffer
        LocalDateTime curTs = null;
        String curSender = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                Matcher m = HEADER.matcher(raw);
                if (m.matches()) {
                    // a new header — flush whatever message was in progress
                    if (body != null && curSender != null) {
                        out.add(new ChatMessage(idSeq++, curTs, curSender, body.toString().strip()));
                    }
                    body = null; curSender = null; curTs = null;

                    LocalDateTime ts = LocalDateTime.parse(m.group(1) + " " + m.group(2), DTF);
                    String rest = m.group(3);

                    int colon = rest.indexOf(": ");
                    if (colon < 0) {
                        continue; // system line (no "Sender: ") — skip
                    }
                    String sender = rest.substring(0, colon);
                    // guard against pathological "sender" strings
                    if (sender.length() > 40 || sender.contains(":")) {
                        continue;
                    }
                    curSender = sender;
                    curTs = ts;
                    body = new StringBuilder(rest.substring(colon + 2));
                } else if (body != null) {
                    // continuation line of a multi-line message
                    body.append('\n').append(raw);
                }
            }
        }
        if (body != null && curSender != null) {
            out.add(new ChatMessage(idSeq, curTs, curSender, body.toString().strip()));
        }
        return out;
    }
}
