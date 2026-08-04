package com.syntricdb.engine.stream;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamEngine {
    private static final Logger log = LoggerFactory.getLogger(StreamEngine.class);
    
    private final Map<String, StreamTopic> topics = new ConcurrentHashMap<>();

    public static class StreamMessage {
        private final long id;
        private final String topic;
        private final Map<String, Object> payload;
        private final long timestamp;

        public StreamMessage(long id, String topic, Map<String, Object> payload) {
            this.id = id;
            this.topic = topic;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }

        public long getId() { return id; }
        public String getTopic() { return topic; }
        public Map<String, Object> getPayload() { return payload; }
        public long getTimestamp() { return timestamp; }
    }

    public static class StreamTopic {
        private final String name;
        private final ConcurrentLinkedQueue<StreamMessage> messages = new ConcurrentLinkedQueue<>();
        private final AtomicLong offsetCounter = new AtomicLong(0);
        private final List<Consumer<StreamMessage>> listeners = new CopyOnWriteArrayList<>();

        public StreamTopic(String name) {
            this.name = name;
        }

        public StreamMessage publish(Map<String, Object> payload) {
            long offset = offsetCounter.incrementAndGet();
            StreamMessage msg = new StreamMessage(offset, name, payload);
            messages.add(msg);

            // Keep buffer size reasonable
            while (messages.size() > 5000) {
                messages.poll();
            }

            for (Consumer<StreamMessage> listener : listeners) {
                try {
                    listener.accept(msg);
                } catch (Exception e) {
                    log.error("Error dispatching stream listener", e);
                }
            }
            return msg;
        }

        public void subscribe(Consumer<StreamMessage> listener) {
            listeners.add(listener);
        }

        public List<StreamMessage> getLatestMessages(int limit) {
            List<StreamMessage> list = new ArrayList<>(messages);
            if (list.size() > limit) {
                return list.subList(list.size() - limit, list.size());
            }
            return list;
        }

        public String getName() { return name; }
        public long getOffset() { return offsetCounter.get(); }
    }

    public StreamMessage publish(String topicName, Map<String, Object> payload) {
        StreamTopic topic = topics.computeIfAbsent(topicName.toLowerCase(), StreamTopic::new);
        return topic.publish(payload);
    }

    public StreamTopic getTopic(String topicName) {
        return topics.get(topicName.toLowerCase());
    }

    public Map<String, StreamTopic> getTopics() {
        return Collections.unmodifiableMap(topics);
    }
}
