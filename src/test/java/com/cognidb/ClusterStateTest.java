package com.cognidb;

import com.cognidb.cluster.ClusterState;
import com.cognidb.cluster.RaftNode;
import com.cognidb.cluster.ShardManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterStateTest {

    private ClusterState clusterState;
    private ShardManager shardManager;

    @BeforeEach
    public void setup() {
        clusterState = new ClusterState();
        shardManager = new ShardManager(100);
    }

    @Test
    public void testRaftNodesInitialization() {
        assertEquals(4, clusterState.getNodes().size());

        RaftNode leader = clusterState.getLeader();
        assertNotNull(leader);
        assertEquals(RaftNode.NodeRole.LEADER, leader.getRole());
    }

    @Test
    public void testConsistentHashingSharding() {
        shardManager.addNode("node-1");
        shardManager.addNode("node-2");
        shardManager.addNode("node-3");

        String targetNode1 = shardManager.getNodeForKey("user_key_100");
        String targetNode2 = shardManager.getNodeForKey("user_key_200");

        assertNotNull(targetNode1);
        assertNotNull(targetNode2);
        assertTrue(targetNode1.startsWith("node-"));
    }
}
