package com.cognidb;

import com.cognidb.ai.AIEngine;
import com.cognidb.cli.CogniCLI;
import com.cognidb.cluster.ClusterState;
import com.cognidb.engine.StorageEngine;
import com.cognidb.engine.schema.Tuple;
import com.cognidb.net.NettyServer;
import com.cognidb.sql.QueryExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CogniDBServer {
    private static final Logger log = LoggerFactory.getLogger(CogniDBServer.class);

    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;
    private final QueryExecutor queryExecutor;
    private final ClusterState clusterState;
    private final NettyServer nettyServer;

    public CogniDBServer(int port, Path dataDir) throws Exception {
        this.storageEngine = new StorageEngine(dataDir);
        this.aiEngine = new AIEngine(128);
        this.queryExecutor = new QueryExecutor(storageEngine, aiEngine);
        this.clusterState = new ClusterState();
        this.nettyServer = new NettyServer(port, storageEngine, aiEngine, queryExecutor, clusterState);
    }

    public void start(boolean startCli) throws Exception {
        log.info("Starting CogniDB AI-Native Database Server...");

        // 1. Initialize Default Schema
        queryExecutor.execute("CREATE TABLE users (id VARCHAR PRIMARY KEY, name VARCHAR, city VARCHAR, age INT, role VARCHAR, bio VARCHAR, embedding FLOAT_VECTOR(128))");

        // 2. Seed Initial Sample Data with Embeddings & AI Functions
        seedSampleData();

        // 3. Start Netty HTTP API Server & Web Management Console
        nettyServer.start();

        log.info("==========================================================================");
        log.info("⚡ CogniDB Next-Gen Unified AI-Native Engine is Ready ⚡");
        log.info("🌐 Web Console & SQL Studio: http://localhost:8080/");
        log.info("📡 REST Query API: POST http://localhost:8080/api/sql");
        log.info("🔍 HNSW Vector API: POST http://localhost:8080/api/vector/search");
        log.info("==========================================================================");

        if (startCli) {
            CogniCLI cli = new CogniCLI(queryExecutor);
            cli.startInteractiveRepl();
        } else {
            Thread.currentThread().join();
        }
    }

    private void seedSampleData() throws Exception {
        log.info("Seeding initial AI-native data and vectors...");

        Object[][] usersData = new Object[][]{
            {"usr_101", "Upendra Kumar", "Hyderabad", 29, "Principal AI & Systems Architect", "Specializes in high throughput distributed databases, Raft consensus, Netty, Java 21, and vector embeddings.", "Java Engineer"},
            {"usr_102", "Ananya Sharma", "Hyderabad", 31, "Lead Systems Engineer", "Passionate about low-latency LSM Trees, memory caching, RocksDB, Rust, and streaming data pipelines.", "Java Engineer"},
            {"usr_103", "Rahul Verma", "Bengaluru", 34, "Senior AI Researcher", "Focused on HNSW graphs, vector search, LLM retrieval augmented generation (RAG), and neural semantic search.", "AI Researcher"},
            {"usr_104", "Sophia Chen", "San Francisco", 28, "Distributed Systems Engineer", "Building high availability cloud databases, horizontal sharding, Kubernetes operators, and Raft replication.", "Cloud Database Specialist"},
            {"usr_105", "Vikram Malhotra", "Hyderabad", 32, "Staff Data Platform Engineer", "Expert in DuckDB, columnar storage format Apache Arrow, query optimizers, and high write throughput.", "Java Engineer"},
            {"usr_106", "Elena Rostova", "London", 30, "AI Infrastructure Engineer", "Specializes in deep learning inference optimization, vector indexing, GPU caching, and full-text search.", "AI Researcher"}
        };

        for (Object[] u : usersData) {
            Tuple tuple = new Tuple();
            tuple.set("id", u[0]);
            tuple.set("name", u[1]);
            tuple.set("city", u[2]);
            tuple.set("age", u[3]);
            tuple.set("role", u[4]);
            tuple.set("bio", u[5]);
            tuple.set("embedding", aiEngine.aiEmbed(u[6].toString()));
            storageEngine.insert("users", tuple);
        }

        // Seed stream event topic
        queryExecutor.execute("PUBLISH INTO system_events VALUES {\"event\": \"CLUSTER_INITIALIZED\", \"status\": \"HEALTHY\"}");
    }

    public static void main(String[] args) {
        try {
            int port = 8080;
            boolean cli = args.length > 0 && "--cli".equalsIgnoreCase(args[0]);
            Path dataDir = Paths.get(System.getProperty("user.home"), ".cognidb");

            CogniDBServer server = new CogniDBServer(port, dataDir);
            server.start(cli);
        } catch (Exception e) {
            log.error("Fatal error starting CogniDB Server", e);
            System.exit(1);
        }
    }
}
