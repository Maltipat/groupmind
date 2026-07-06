import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Standalone verifier. Contains the EXACT parser regex and consensus-clustering
 * algorithm that get transcribed into WhatsAppParser.java and ConsensusService.java.
 * Runs with plain javac/java (no Spring, no embeddings) so the deterministic
 * logic can be proven against the real synthetic dataset in this container.
 */
public class Verify {

    // ---------------- model ----------------
    record Msg(int id, LocalDateTime ts, String sender, String text) {}

    // ---------------- PARSER (mirrors WhatsAppParser) ----------------
    // "DD/MM/YY, HH:MM - Sender: message"   (sender split on FIRST ": " after " - ")
    static final Pattern HEADER = Pattern.compile(
        "^(\\d{2}/\\d{2}/\\d{2}), (\\d{2}:\\d{2}) - (.*)$");
    static final DateTimeFormatter DTF =
        DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    static List<Msg> parse(List<String> lines) {
        List<Msg> out = new ArrayList<>();
        int[] idSeq = {0};
        StringBuilder cur = null;    // continuation buffer for multi-line messages
        LocalDateTime curTs = null;
        String curSender = null;

        Runnable flush = null; // (can't use lambda capturing mutable easily; inline below)

        for (String raw : lines) {
            Matcher m = HEADER.matcher(raw);
            if (m.matches()) {
                // flush previous accumulated message
                if (cur != null && curSender != null) {
                    out.add(new Msg(idSeq[0]++, curTs, curSender, cur.toString().strip()));
                }
                cur = null; curSender = null; curTs = null;

                String date = m.group(1), time = m.group(2), rest = m.group(3);
                LocalDateTime ts = LocalDateTime.parse(date + " " + time, DTF);

                int colon = rest.indexOf(": ");
                if (colon < 0) {
                    // system line (group created / added / encryption / joined) -> skip
                    continue;
                }
                String sender = rest.substring(0, colon);
                String body = rest.substring(colon + 2);
                // guard: a sender name shouldn't be absurdly long (avoids "X changed the subject: ..." style false positives)
                if (sender.length() > 40 || sender.contains(":")) {
                    continue;
                }
                cur = new StringBuilder(body);
                curSender = sender;
                curTs = ts;
            } else {
                // continuation of previous message (multi-line)
                if (cur != null) {
                    cur.append("\n").append(raw);
                }
            }
        }
        if (cur != null && curSender != null) {
            out.add(new Msg(idSeq[0]++, curTs, curSender, cur.toString().strip()));
        }
        return out;
    }

    // ---------------- CONSENSUS (mirrors ConsensusService) ----------------
    // Case-INSENSITIVE token clustering. Strip English stopwords + household-
    // service CATEGORY words so the vendor NAME/BRAND surfaces; count DISTINCT
    // senders per candidate token; merge co-occurring tokens into one entity.
    static final Set<String> STOP = new HashSet<>(Arrays.asList(
        // english / chat glue
        "the","and","for","are","was","this","that","very","good","call","yes","not",
        "haan","also","did","does","who","what","when","where","how","pls","please",
        "need","anyone","someone","from","with","said","says","recommend","recommends",
        "recommendation","recommendations","reliable","honest","same","will","have","has",
        "your","our","his","her","him","she","they","them","use","used","using","get","got",
        "can","just","imo","btw","today","yesterday","last","month","summer","winter","time",
        "number","contact","reasonable","charges","rate","fair","done","fine","nice","great",
        "society","greenwood","wing","floor","pinned","list","give","gives","adding","added",
        // household-service CATEGORY words (we want the NAME, not the category)
        "repair","service","servicing","serviced","technician","mechanic","guy","wala",
        "bhai","aircon","cooling","cool","care","refill","split","unit","units",
        "wiring","wireman","electrician","electrical","switch","switchboard","switchboards",
        "regulator","regulators","socket","spark","sparking","plumber","plumbing","pipeline",
        "drainage","leak","leakage","sink","blockage","choked","bathroom","fitting",
        "carpenter","painter","painting","noise","work","stopped","working"));

    // lowercase alphabetic tokens, length >= 4, not a stopword
    static List<String> candidates(String text) {
        List<String> res = new ArrayList<>();
        Matcher m = Pattern.compile("[A-Za-z]+").matcher(text.toLowerCase());
        while (m.find()) {
            String t = m.group();
            if (t.length() < 4) continue;
            if (STOP.contains(t)) continue;
            res.add(t);
        }
        return res;
    }

    record Entity(String label, Set<String> senders, Set<Integer> msgIds) {}

    static List<Entity> cluster(List<Msg> retrieved) {
        Map<String, Set<String>> tokSenders = new HashMap<>();
        Map<String, Set<Integer>> tokMsgs = new HashMap<>();
        for (Msg msg : retrieved) {
            for (String t : new HashSet<>(candidates(msg.text()))) {
                tokSenders.computeIfAbsent(t, k -> new HashSet<>()).add(msg.sender());
                tokMsgs.computeIfAbsent(t, k -> new HashSet<>()).add(msg.id());
            }
        }
        // consensus candidates: token backed by >= 2 DISTINCT senders
        List<String> keys = tokSenders.keySet().stream()
                .filter(t -> tokSenders.get(t).size() >= 2)
                .collect(Collectors.toList());

        // union-find: merge tokens co-occurring in >= 2 of the same messages
        Map<String, String> parent = new HashMap<>();
        for (String k : keys) parent.put(k, k);
        java.util.function.Function<String, String> find = new java.util.function.Function<>() {
            public String apply(String x) {
                while (!parent.get(x).equals(x)) { parent.put(x, parent.get(parent.get(x))); x = parent.get(x); }
                return x;
            }
        };
        for (int i = 0; i < keys.size(); i++)
            for (int j = i + 1; j < keys.size(); j++) {
                String a = keys.get(i), b = keys.get(j);
                Set<Integer> inter = new HashSet<>(tokMsgs.get(a));
                inter.retainAll(tokMsgs.get(b));
                if (inter.size() >= 2) parent.put(find.apply(a), find.apply(b));
            }

        Map<String, Entity> groups = new HashMap<>();
        Map<String, Integer> bestTokScore = new HashMap<>();
        for (String k : keys) {
            String root = find.apply(k);
            Entity e = groups.computeIfAbsent(root,
                    r -> new Entity(k, new HashSet<>(), new HashSet<>()));
            e.senders().addAll(tokSenders.get(k));
            e.msgIds().addAll(tokMsgs.get(k));
            int sc = tokSenders.get(k).size();
            if (sc > bestTokScore.getOrDefault(root, -1)) {
                bestTokScore.put(root, sc);
                groups.put(root, new Entity(k, e.senders(), e.msgIds()));
            }
        }
        List<Entity> out = new ArrayList<>(groups.values());
        out.sort((x, y) -> Integer.compare(y.senders().size(), x.senders().size()));
        return out;
    }

    // crude keyword prefilter to SIMULATE retrieval (real app uses embeddings)
    static List<Msg> keywordRetrieve(List<Msg> all, String... anyOf) {
        return all.stream().filter(msg -> {
            String low = msg.text().toLowerCase();
            for (String kw : anyOf) if (low.contains(kw)) return true;
            return false;
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        List<Msg> all = parse(lines);

        System.out.println("PARSER");
        System.out.println("  parsed messages : " + all.size());
        System.out.println("  distinct senders: " + all.stream().map(Msg::sender).distinct().count());
        System.out.println("  first           : " + all.get(0).ts() + " | " + all.get(0).sender()
                + " | " + all.get(0).text());
        // show a multi-line message got joined (Vikram's phone-number follow-up is separate msg,
        // but the encryption/system lines must be skipped): confirm no system line leaked in
        boolean leaked = all.stream().anyMatch(x -> x.text().contains("end-to-end encrypted")
                || x.sender().contains("created group"));
        System.out.println("  system lines leaked as messages? " + leaked);

        System.out.println("\nCONSENSUS (simulated retrieval per query; real app uses embeddings)");
        String[][] queries = {
            {"AC repair",   "ac ", " ac", "aircon", "cooling", "cool care", "ramesh"},
            {"electrician", "electric", "wiring", "socket", "switch", "sunil", "wireman"},
            {"plumber",     "plumb", "leak", "tap", "sink", "drainage", "iqbal", "pipeline"},
        };
        for (String[] q : queries) {
            String label = q[0];
            String[] kws = Arrays.copyOfRange(q, 1, q.length);
            List<Msg> retrieved = keywordRetrieve(all, kws);
            List<Entity> ents = cluster(retrieved);
            System.out.println("\n  Query: \"who do people recommend for " + label + "?\"");
            System.out.println("    retrieved messages: " + retrieved.size());
            boolean any = false;
            for (Entity e : ents) {
                if (e.senders().size() < 2) continue;
                any = true;
                System.out.println("    -> '" + e.label() + "' backed by " + e.senders().size()
                        + " distinct residents: " + e.senders());
            }
            if (!any) System.out.println("    (no multi-sender consensus found)");
        }
    }
}
