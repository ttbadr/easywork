package com.example.pubsub;

import com.google.pubsub.v1.ReceivedMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PubSubExample {
    public static void main(String[] args) {
        try {
            String projectId = "your-project-id";
            String topicId = "your-topic-id";
            String subscriptionId = "your-subscription-id";
            String credentialsPath = "/path/to/your/credentials.json";

            // Initialize the multi-threaded subscriber
            PubSubSubscriber multiThreadSubscriber = new PubSubSubscriber(projectId, subscriptionId, credentialsPath);
            multiThreadSubscriber.startSubscription((messageData, attributes) -> {
                System.out.println("Received message: " + messageData);
                System.out.println("Attributes: " + attributes);
            });

            // Initialize publisher
            PubSubPublisher publisher = new PubSubPublisher(projectId, topicId, credentialsPath);

            // Publish some test messages
            for (int i = 0; i < 10; i++) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put("messageNumber", String.valueOf(i));
                attributes.put("timestamp", String.valueOf(System.currentTimeMillis()));

                try {
                    String messageId = publisher.publish("Test message " + i, attributes);
                    System.out.println("Published message " + i + " with ID: " + messageId);
                } catch (ExecutionException | InterruptedException e) {
                    System.err.println("Error publishing message: " + e.getMessage());
                }
            }

            // Wait for messages to be processed
            Thread.sleep(10000);

            // Cleanup
            publisher.shutdown();
            multiThreadSubscriber.stopSubscription();

            // Pull mode example
            System.out.println("\n=== Pull Mode Example ===");
            PubSubSubscriber pullSubscriber = new PubSubSubscriber(projectId, subscriptionId, credentialsPath);
            List<ReceivedMessage> messages = pullSubscriber.pullMessages(5);
            for (ReceivedMessage message : messages) {
                String messageData = message.getMessage().getData().toStringUtf8();
                Map<String, String> attributes = message.getMessage().getAttributesMap();
                
                System.out.println("Pull mode - Received message: " + messageData);
                System.out.println("Attributes: " + attributes);
            }
        } catch (Exception e) {
            System.err.println("Error in Pub/Sub operations: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
