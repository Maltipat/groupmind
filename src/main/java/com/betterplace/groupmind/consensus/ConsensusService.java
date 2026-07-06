package com.betterplace.groupmind.consensus;

import com.betterplace.groupmind.config.GroupMindProperties;
import com.betterplace.groupmind.retrieve.RetrievedMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consensus detection over a set of retrieved messages.
 *
 * <p>The problem: semantic retrieval brings back all the ways people phrased the
 * same recommendation, but the vendor's NAME is written inconsistently and often
 * lowercase ("ramesh", "cool care", "ramesh cool care"). We want to collapse those
 * into one entity and count how many <em>distinct</em> people vouched for it.
 *
 * <p>The approach (verified standalone against the synthetic dataset before this
 * class existed — AC repair surfaced 8 distinct backers, electrician 4, plumber 3):
 * <ol>
 *   <li>Tokenize each message case-insensitively; keep alphabetic tokens of length
 *       &ge; 4 that aren't English/chat glue or household-service CATEGORY words
 *       (we strip "repair", "cooling", "electrician"… so the NAME surfaces, not the
 *       category everyone shares).</li>
 *   <li>For each candidate token, count the DISTINCT senders who used it. Keep only
 *       tokens backed by &ge; {@code consensusMinSenders} distinct people.</li>
 *   <li>Merge tokens that co-occur in &ge; 2 of the same messages (union-find), so
 *       "ramesh" + "cool" + "care" collapse into a single entity.</li>
 *   <li>Rank entities by distinct-sender count.</li>
 * </ol>
 *
 * <p>This is deliberately a lightweight lexical clustering rather than a second
 * embedding pass: on short, name-bearing group-chat messages it's fast, explainable,
 * and — critically — attributes each mention to exactly one sender so the count is
 * trustworthy.
 */
@Service
public class ConsensusService {

    private final GroupMindProperties props;

    public ConsensusService(GroupMindProperties props) {
        this.props = props;
    }

    // English / chat glue + household-service CATEGORY words. Stripping the category
    // words is what lets the vendor NAME rise to the top instead of "repair"/"service".
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            // english / chat glue
            "the", "and", "for", "are", "was", "this", "that", "very", "good", "call", "yes", "not",
            "haan", "also", "did", "does", "who", "what", "when", "where", "how", "pls", "please",
            "need", "anyone", "someone", "from", "with", "said", "says", "recommend", "recommends",
            "recommendation", "recommendations", "reliable", "honest", "same", "will", "have", "has",
            "your", "our", "his", "her", "him", "she", "they", "them", "use", "used", "using", "get", "got",
            "can", "just", "imo", "btw", "today", "yesterday", "last", "month", "summer", "winter", "time",
            "number", "contact", "reasonable", "charges", "rate", "fair", "done", "fine", "nice", "great",
            "society", "greenwood", "wing", "floor", "pinned", "list", "give", "gives", "adding", "added",
            // household-service CATEGORY words (we want the NAME, not the category)
            "repair", "service", "servicing", "serviced", "technician", "mechanic", "guy", "wala",
            "bhai", "aircon", "cooling", "cool", "care", "refill", "split", "unit", "units",
            "wiring", "wireman", "electrician", "electrical", "switch", "switchboard", "switchboards",
            "regulator", "regulators", "socket", "spark", "sparking", "plumber", "plumbing", "pipeline",
            "drainage", "leak", "leakage", "sink", "blockage", "choked", "bathroom", "fitting",
            "carpenter", "painter", "painting", "noise", "work", "stopped", "working"));

    private static final Pattern WORD = Pattern.compile("[A-Za-z]+");

    /** Lowercase alphabetic tokens, length >= 4, not a stopword. */
    private static List<String> candidates(String text) {
        List<String> res = new ArrayList<>();
        Matcher m = WORD.matcher(text.toLowerCase());
        while (m.find()) {
            String t = m.group();
            if (t.length() < 4) continue;
            if (STOP.contains(t)) continue;
            res.add(t);
        }
        return res;
    }

    /**
     * Cluster the retrieved messages into consensus entities, ranked by the number
     * of distinct senders backing each. Only entities meeting the configured
     * {@code consensusMinSenders} threshold are returned.
     */
    public List<ConsensusCluster> detect(List<RetrievedMessage> retrieved) {
        int minSenders = props.getConsensusMinSenders();

        Map<String, Set<String>> tokSenders = new HashMap<>();
        Map<String, Set<Integer>> tokMsgs = new HashMap<>();
        for (RetrievedMessage msg : retrieved) {
            for (String t : new HashSet<>(candidates(msg.text()))) {
                tokSenders.computeIfAbsent(t, k -> new HashSet<>()).add(msg.sender());
                tokMsgs.computeIfAbsent(t, k -> new HashSet<>()).add(msg.id());
            }
        }

        // consensus candidates: tokens backed by >= minSenders distinct senders
        List<String> keys = new ArrayList<>();
        for (var e : tokSenders.entrySet()) {
            if (e.getValue().size() >= minSenders) {
                keys.add(e.getKey());
            }
        }

        // union-find: merge tokens co-occurring in >= 2 of the same messages
        Map<String, String> parent = new HashMap<>();
        for (String k : keys) parent.put(k, k);
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                String a = keys.get(i), b = keys.get(j);
                Set<Integer> inter = new HashSet<>(tokMsgs.get(a));
                inter.retainAll(tokMsgs.get(b));
                if (inter.size() >= 2) {
                    union(parent, a, b);
                }
            }
        }

        // collapse to entities; label each with its strongest (most-backed) token
        Map<String, Set<String>> groupSenders = new HashMap<>();
        Map<String, Set<Integer>> groupMsgs = new HashMap<>();
        Map<String, String> groupLabel = new HashMap<>();
        Map<String, Integer> groupBest = new HashMap<>();
        for (String k : keys) {
            String root = find(parent, k);
            groupSenders.computeIfAbsent(root, r -> new HashSet<>()).addAll(tokSenders.get(k));
            groupMsgs.computeIfAbsent(root, r -> new HashSet<>()).addAll(tokMsgs.get(k));
            int sc = tokSenders.get(k).size();
            if (sc > groupBest.getOrDefault(root, -1)) {
                groupBest.put(root, sc);
                groupLabel.put(root, k);
            }
        }

        List<ConsensusCluster> out = new ArrayList<>();
        for (String root : groupSenders.keySet()) {
            Set<String> senders = groupSenders.get(root);
            if (senders.size() < minSenders) continue;
            List<String> senderList = new ArrayList<>(senders);
            senderList.sort(String::compareTo);
            List<Integer> msgList = new ArrayList<>(groupMsgs.get(root));
            msgList.sort(Integer::compareTo);
            out.add(new ConsensusCluster(groupLabel.get(root), senders.size(), senderList, msgList));
        }
        out.sort((x, y) -> Integer.compare(y.distinctSenderCount(), x.distinctSenderCount()));
        return out;
    }

    // ---- union-find helpers ----
    private static String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        parent.put(find(parent, a), find(parent, b));
    }
}
