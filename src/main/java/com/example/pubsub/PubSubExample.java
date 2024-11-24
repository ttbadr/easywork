package com.example.pubsub;

import com.google.pubsub.v1.ReceivedMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PubSubExample {
    public static void main(String[] args) {
        String projectId = "your-project-id";
        String topicId = "your-topic-id";
        String subscriptionId = "your-subscription-id";
        String credentialsPath = "/path/to/service-account-key.json";

        try {
            // Example 1: Using default credentials with single thread (default)
            PubSubSubscriber defaultSubscriber = new PubSubSubscriber(projectId, subscriptionId);

            // Example 2: Using default credentials with multiple threads
            PubSubSubscriber multiThreadSubscriber = new PubSubSubscriber(projectId, subscriptionId, 3);

            // Example 3: Using service account with custom thread pool size
            PubSubSubscriber customSubscriber = new PubSubSubscriber(projectId, subscriptionId, credentialsPath, 5);

            // Create a publisher
            PubSubPublisher publisher = new PubSubPublisher(projectId, topicId, credentialsPath);

            // Start the multi-threaded subscriber
            multiThreadSubscriber.startSubscription((message, attributes) -> {
                System.out.println("Received message: " + message + " on thread: " + Thread.currentThread().getName());
                if (attributes != null) {
                    System.out.println("Attributes: " + attributes);
                }
            });

            // Publish some test messages
            for (int i = 0; i < 10; i++) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put("messageNumber", String.valueOf(i));
                attributes.put("timestamp", String.valueOf(System.currentTimeMillis()));

                String messageId = publisher.publish("Test message " + i, attributes);
                System.out.println("Published message " + i + " with ID: " + messageId);
            }

            // Wait for messages to be processed
            Thread.sleep(10000);

            // Cleanup
            publisher.shutdown();
            multiThreadSubscriber.stopSubscription();

            // 示例2：使用拉取模式（同步批量处理）
            System.out.println("\n=== Pull Mode Example ===");
            PubSubSubscriber pullSubscriber = new PubSubSubscriber(projectId, subscriptionId, credentialsPath);
            List<ReceivedMessage> messages = pullSubscriber.pullMessages(5); // 拉取最多5条消息
            for (ReceivedMessage message : messages) {
                String messageData = message.getMessage().getData().toStringUtf8();
                Map<String, String> attributes = message.getMessage().getAttributesMap();
                
                System.out.println("Pull mode - Received message: " + messageData);
                System.out.println("Attributes: " + attributes);
            }

            // 确认处理完成的消息
            pullSubscriber.acknowledgeMessages(messages);

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
