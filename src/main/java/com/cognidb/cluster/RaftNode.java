package com.cognidb.cluster;

import java.util.*;

public class RaftNode {
    public enum NodeRole {
        LEADER,
        CANDIDATE,
        FOLLOWER
    }

    private final String nodeId;
    private final String host;
    private final int port;
    private volatile NodeRole role;
    private volatile long currentTerm;
    private volatile String votedFor;
    private final List<String> logEntries = new ArrayList<>();
    private volatile long lastHeartbeat;

    public RaftNode(String nodeId, String host, int port, NodeRole initialRole) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.role = initialRole;
        this.currentTerm = 1;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void receiveHeartbeat(long term, String leaderId) {
        this.lastHeartbeat = System.currentTimeMillis();
        if (term >= currentTerm) {
            this.currentTerm = term;
            this.role = NodeRole.FOLLOWER;
        }
    }

    public void appendLog(String entry) {
        logEntries.add(entry);
    }

    public String getNodeId() { return nodeId; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public NodeRole getRole() { return role; }
    public void setRole(NodeRole role) { this.role = role; }
    public long getCurrentTerm() { return currentTerm; }
    public void setTerm(long term) { this.currentTerm = term; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public int getLogCount() { return logEntries.size(); }
}
