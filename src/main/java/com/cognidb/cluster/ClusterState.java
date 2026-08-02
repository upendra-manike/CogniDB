package com.cognidb.cluster;

import java.util.*;

public class ClusterState {
    private final Map<String, RaftNode> nodes = new LinkedHashMap<>();
    private final ShardManager shardManager;

    public ClusterState() {
        this.shardManager = new ShardManager(5);
        // Initialize 4 simulated distributed nodes as per vision
        addNode(new RaftNode("node-1", "127.0.0.1", 9091, RaftNode.NodeRole.LEADER));
        addNode(new RaftNode("node-2", "127.0.0.1", 9092, RaftNode.NodeRole.FOLLOWER));
        addNode(new RaftNode("node-3", "127.0.0.1", 9093, RaftNode.NodeRole.FOLLOWER));
        addNode(new RaftNode("node-4", "127.0.0.1", 9094, RaftNode.NodeRole.FOLLOWER));
    }

    public void addNode(RaftNode node) {
        nodes.put(node.getNodeId(), node);
        shardManager.addNode(node.getNodeId());
    }

    public RaftNode getLeader() {
        for (RaftNode node : nodes.values()) {
            if (node.getRole() == RaftNode.NodeRole.LEADER) {
                return node;
            }
        }
        return null;
    }

    public Map<String, RaftNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public ShardManager getShardManager() {
        return shardManager;
    }
}
