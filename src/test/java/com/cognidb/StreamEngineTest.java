package com.cognidb;

import com.cognidb.engine.stream.StreamEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class StreamEngineTest {

    private StreamEngine streamEngine;

    @BeforeEach
    public void setup() {
        streamEngine = new StreamEngine();
    }

    @Test
    public void testPublishAndSubscribe() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> received = new AtomicReference<>();

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "NODE_JOINED");

        StreamEngine.StreamTopic topic = streamEngine.getTopic("system_logs");
        if (topic == null) {
            streamEngine.publish("system_logs", payload);
            topic = streamEngine.getTopic("system_logs");
        }

        topic.subscribe(msg -> {
            received.set(msg.getPayload().get("event"));
            latch.countDown();
        });

        streamEngine.publish("system_logs", payload);

        boolean success = latch.await(2, TimeUnit.SECONDS);
        assertTrue(success);
        assertEquals("NODE_JOINED", received.get());
    }

    @Test
    public void testTopicMessageHistory() {
        Map<String, Object> p1 = new HashMap<>();
        p1.put("k", "v1");
        Map<String, Object> p2 = new HashMap<>();
        p2.put("k", "v2");

        streamEngine.publish("telemetry", p1);
        streamEngine.publish("telemetry", p2);

        StreamEngine.StreamTopic topic = streamEngine.getTopic("telemetry");
        assertNotNull(topic);
        assertEquals(2, topic.getLatestMessages(10).size());
    }
}
