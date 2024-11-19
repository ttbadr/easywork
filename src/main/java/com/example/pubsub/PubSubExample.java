package com.example.pubsub;

import java.io.IOException;
import java.util.HashMap;
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

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
