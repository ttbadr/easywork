package com.example.pubsub;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class PubSubSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(PubSubSubscriber.class);
    private static final int DEFAULT_THREAD_POOL_SIZE = 1;
    private static final int DEFAULT_MAX_MESSAGES = 100;
    
    private final ProjectSubscriptionName subscriptionName;
    private final GoogleCredentials credentials;
    private ExecutorService executorService;
    private Subscriber subscriber;

    /**
     * Creates a subscriber with default credentials
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @throws IOException if credentials cannot be loaded
     */
    public PubSubSubscriber(String projectId, String subscriptionId) throws IOException {
        this.subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        this.credentials = GoogleCredentials.getApplicationDefault();
    }

    /**
     * Creates a subscriber with service account credentials
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param credentialsPath Path to service account key file
     * @throws IOException if credentials cannot be loaded
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath) throws IOException {
        this.subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        this.credentials = ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath));
    }

    /**
     * Creates a subscriber with custom thread pool size
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param threadPoolSize Thread pool size for message processing
     * @throws IOException if credentials cannot be loaded
     */
    public PubSubSubscriber(String projectId, String subscriptionId, int threadPoolSize) throws IOException {
        this(projectId, subscriptionId);
        initializeThreadPool(threadPoolSize);
    }

    /**
     * Creates a subscriber with service account credentials and custom thread pool size
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param credentialsPath Path to service account key file
     * @param threadPoolSize Thread pool size for message processing
     * @throws IOException if credentials cannot be loaded
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath, int threadPoolSize) throws IOException {
        this(projectId, subscriptionId, credentialsPath);
        initializeThreadPool(threadPoolSize);
    }

    private void initializeThreadPool(int threadPoolSize) {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("PubSubSubscriber-" + threadNumber.getAndIncrement());
            return thread;
        };
        this.executorService = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
        logger.info("Created subscriber with thread pool size: {}", threadPoolSize);
    }

    /**
     * Starts subscription and processes messages
     *
     * @param messageHandler Callback function to handle received messages
     */
    public void startSubscription(BiConsumer<String, Map<String, String>> messageHandler) {
        if (executorService == null) {
            initializeThreadPool(DEFAULT_THREAD_POOL_SIZE);
        }

        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            try {
                String messageData = message.getData().toStringUtf8();
                Map<String, String> attributes = message.getAttributesMap();
                
                executorService.submit(() -> {
                    try {
                        messageHandler.accept(messageData, attributes);
                        consumer.ack();
                    } catch (Exception e) {
                        logger.error("Error processing message: " + messageData, e);
                        consumer.nack();
                    }
                });
            } catch (Exception e) {
                logger.error("Error processing message", e);
                consumer.nack();
            }
        };

        subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(() -> credentials)
                .build();
        
        subscriber.startAsync().awaitRunning();
        logger.info("Subscription started for: {} with thread pool", subscriptionName);
    }

    /**
     * Stops subscription and releases resources
     */
    public void stopSubscription() {
        if (subscriber != null) {
            subscriber.stopAsync().awaitTerminated();
            logger.info("Subscription stopped for: {}", subscriptionName);
        }
        
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        logger.error("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Pulls messages from subscription in a blocking manner
     *
     * @param maxMessages Maximum number of messages to pull
     * @return List of received messages
     * @throws IOException if there's an error communicating with the service
     */
    public List<ReceivedMessage> pullMessages(int maxMessages) throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = createSubscriptionAdminClient()) {
            PullRequest pullRequest = PullRequest.newBuilder()
                    .setMaxMessages(maxMessages)
                    .setSubscription(subscriptionName.toString())
                    .build();

            PullResponse pullResponse = subscriptionAdminClient.pull(pullRequest);
            List<ReceivedMessage> receivedMessages = new ArrayList<>();
            
            for (ReceivedMessage message : pullResponse.getReceivedMessagesList()) {
                receivedMessages.add(message);
            }
            
            logger.info("Pulled {} messages from subscription {}", 
                receivedMessages.size(), subscriptionName);
            
            return receivedMessages;
        }
    }

    /**
     * Pulls messages using default maximum message count (100)
     *
     * @return List of received messages
     * @throws IOException if there's an error communicating with the service
     */
    public List<ReceivedMessage> pullMessages() throws IOException {
        return pullMessages(DEFAULT_MAX_MESSAGES);
    }

    /**
     * Acknowledges a list of received messages
     *
     * @param messages List of messages to acknowledge
     * @throws IOException if there's an error communicating with the service
     */
    public void acknowledgeMessages(List<ReceivedMessage> messages) throws IOException {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try (SubscriptionAdminClient subscriptionAdminClient = createSubscriptionAdminClient()) {
            List<String> ackIds = new ArrayList<>();
            for (ReceivedMessage message : messages) {
                ackIds.add(message.getAckId());
            }

            AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
                    .setSubscription(subscriptionName.toString())
                    .addAllAckIds(ackIds)
                    .build();

            subscriptionAdminClient.acknowledge(acknowledgeRequest);
            logger.info("Acknowledged {} messages from subscription {}", 
                ackIds.size(), subscriptionName);
        }
    }

    private SubscriptionAdminClient createSubscriptionAdminClient() throws IOException {
        SubscriptionAdminSettings settings = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return SubscriptionAdminClient.create(settings);
    }
}
