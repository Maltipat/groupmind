package com.betterplace.groupmind.synthesis;

import com.betterplace.groupmind.consensus.ConsensusCluster;
import com.betterplace.groupmind.retrieve.RetrievedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * Turns retrieved messages + consensus clusters into a final, human-readable answer.
 *
 * <p>Two modes, chosen automatically:
 * <ul>
 *   <li><b>LLM mode</b> — active when a {@link ChatModel} bean exists (i.e. the
 *       Anthropic starter is configured with an API key and
 *       {@code spring.ai.model.chat=anthropic}). The model is handed ONLY the
 *       retrieved messages and the consensus tally, and is instructed to ground its
 *       answer in them and cite by message id and count backers. This keeps the
 *       answer faithful to the group's actual history — no outside knowledge.</li>
 *   <li><b>Fallback mode</b> — active with no chat model configured (the default, so
 *       the app runs with zero API keys). A deterministic template states the
 *       top consensus recommendation, its distinct-backer count, and cites the
 *       supporting messages. Not as fluent, but fully functional for a demo and
 *       provably grounded.</li>
 * </ul>
 *
 * The {@link ObjectProvider} means we don't hard-depend on a ChatModel being present;
 * the app starts either way.
 */
@Service
public class SynthesisService {

    private static final Logger log = LoggerFactory.getLogger(SynthesisService.class);

    private final ObjectProvider<ChatModel> chatModelProvider;

    public SynthesisService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    public record Synthesis(String answer, boolean llmUsed) {}

    private static final String SYSTEM_PROMPT = """
            You are GroupMind, an assistant that answers questions about a residential
            society's WhatsApp group using ONLY the messages provided to you.

            Rules:
            - Answer strictly from the supplied messages. Never invent vendors, names,
              phone numbers, or facts that are not in the messages.
            - When a recommendation is backed by multiple distinct people, lead with that
              corroboration explicitly, e.g. "8 residents independently recommend ...".
            - Cite the specific messages you used by their [msgId] and sender.
            - If the messages don't contain an answer, say so plainly.
            - Keep it concise and practical — this is for a busy resident, not an essay.
            """;

    public Synthesis synthesize(String question,
                                List<RetrievedMessage> retrieved,
                                List<ConsensusCluster> clusters) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel != null) {
            try {
                String answer = ChatClient.create(chatModel)
                        .prompt()
                        .system(SYSTEM_PROMPT)
                        .user(buildUserPrompt(question, retrieved, clusters))
                        .call()
                        .content();
                return new Synthesis(answer, true);
            } catch (Exception e) {
                log.warn("Chat model call failed ({}); falling back to templated answer.", e.toString());
            }
        }
        return new Synthesis(templatedAnswer(question, retrieved, clusters), false);
    }

    private String buildUserPrompt(String question,
                                   List<RetrievedMessage> retrieved,
                                   List<ConsensusCluster> clusters) {
        StringBuilder sb = new StringBuilder();
        sb.append("QUESTION:\n").append(question).append("\n\n");

        sb.append("CONSENSUS SIGNAL (distinct people independently backing the same thing):\n");
        if (clusters.isEmpty()) {
            sb.append("  (no multi-person consensus detected among the retrieved messages)\n");
        } else {
            for (ConsensusCluster c : clusters) {
                sb.append("  - \"").append(c.label()).append("\": ")
                        .append(c.distinctSenderCount()).append(" distinct people (")
                        .append(String.join(", ", c.senders())).append(")\n");
            }
        }

        sb.append("\nRETRIEVED MESSAGES (your only source of truth):\n");
        for (RetrievedMessage m : retrieved) {
            sb.append("  [").append(m.id()).append("] ")
                    .append(m.sender()).append(" (").append(m.timestamp()).append("): ")
                    .append(m.text().replace("\n", " ")).append("\n");
        }
        return sb.toString();
    }

    /** Deterministic, dependency-free answer used when no chat model is configured. */
    private String templatedAnswer(String question,
                                   List<RetrievedMessage> retrieved,
                                   List<ConsensusCluster> clusters) {
        if (retrieved.isEmpty()) {
            return "I couldn't find anything in the group chat about \"" + question + "\".";
        }
        StringBuilder sb = new StringBuilder();
        if (!clusters.isEmpty()) {
            ConsensusCluster top = clusters.get(0);
            sb.append(top.distinctSenderCount())
                    .append(top.distinctSenderCount() == 1 ? " resident" : " residents")
                    .append(" independently point to \"").append(top.label()).append("\"");
            sb.append(" (").append(String.join(", ", top.senders())).append(").");

            if (clusters.size() > 1) {
                StringJoiner others = new StringJoiner(", ");
                for (int i = 1; i < clusters.size(); i++) {
                    ConsensusCluster c = clusters.get(i);
                    others.add("\"" + c.label() + "\" (" + c.distinctSenderCount() + ")");
                }
                sb.append(" Other mentions with some support: ").append(others).append(".");
            }
            sb.append("\n\nBased on these messages:\n");
        } else {
            sb.append("No single recommendation was backed by multiple people, "
                    + "but here are the most relevant messages:\n");
        }

        int shown = 0;
        for (RetrievedMessage m : retrieved) {
            sb.append("  • [").append(m.id()).append("] ").append(m.sender())
                    .append(": ").append(m.text().replace("\n", " ")).append("\n");
            if (++shown >= 6) break;
        }
        return sb.toString().strip();
    }
}
