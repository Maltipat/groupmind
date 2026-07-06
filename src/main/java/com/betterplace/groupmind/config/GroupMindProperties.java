package com.betterplace.groupmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for GroupMind, bound from the "groupmind.*" section of application.yml.
 */
@ConfigurationProperties(prefix = "groupmind")
public class GroupMindProperties {

    /** Location of the WhatsApp .txt export to ingest on startup. */
    private String dataPath = "classpath:data/greenwood_residency_chat.txt";

    /** How many semantically-similar messages retrieval returns per query. */
    private int topK = 10;

    /** A recommendation is treated as "consensus" once this many DISTINCT people back it. */
    private int consensusMinSenders = 2;

    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public int getConsensusMinSenders() { return consensusMinSenders; }
    public void setConsensusMinSenders(int consensusMinSenders) { this.consensusMinSenders = consensusMinSenders; }
}
