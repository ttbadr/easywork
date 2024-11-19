package com.example.pubsub;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
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
    
    private final ProjectSubscriptionName subscriptionName;
    private final GoogleCredentials credentials;
    private final ExecutorService executorService;
    private Subscriber subscriber;

    /**
     * Creates a custom thread factory that names threads as "PubSubSubscriber-N"
     */
    private static ThreadFactory createThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("PubSubSubscriber-" + threadNumber.getAndIncrement());
                return thread;
            }
        };
    }

    /**
     * Creates a subscriber using default credentials with default single thread pool
     */
    public PubSubSubscriber(String projectId, String subscriptionId) throws IOException {
        this(projectId, subscriptionId, GoogleCredentials.getApplicationDefault(), DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Creates a subscriber using default credentials with specified thread pool size
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param threadPoolSize Number of threads in the executor service
     */
    public PubSubSubscriber(String projectId, String subscriptionId, int threadPoolSize) throws IOException {
        this(projectId, subscriptionId, GoogleCredentials.getApplicationDefault(), threadPoolSize);
    }

    /**
     * Creates a subscriber using service account key file with default single thread pool
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param credentialsPath Path to service account key file
     * @throws IOException if unable to read credentials file
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath) throws IOException {
        this(projectId, subscriptionId, credentialsPath, DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Creates a subscriber using service account key file with specified thread pool size
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param credentialsPath Path to service account key file
     * @param threadPoolSize Number of threads in the executor service
     * @throws IOException if unable to read credentials file
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath, int threadPoolSize) throws IOException {
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            this.credentials = ServiceAccountCredentials.fromStream(credentialsStream);
            this.subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
            this.executorService = Executors.newFixedThreadPool(threadPoolSize, createThreadFactory());
            logger.info("Created subscriber with credentials from file: {} and thread pool size: {}", 
                credentialsPath, threadPoolSize);
        }
    }

    /**
     * Creates a subscriber using specified credentials with default single thread pool
     */
    public PubSubSubscriber(String projectId, String subscriptionId, GoogleCredentials credentials) {
        this(projectId, subscriptionId, credentials, DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Creates a subscriber using specified credentials with specified thread pool size
     *
     * @param projectId Project ID
     * @param subscriptionId Subscription ID
     * @param credentials Google credentials
     * @param threadPoolSize Number of threads in the executor service
     */
    public PubSubSubscriber(String projectId, String subscriptionId, GoogleCredentials credentials, int threadPoolSize) {
        this.subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        this.credentials = credentials;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize, createThreadFactory());
        logger.info("Created subscriber with thread pool size: {}", threadPoolSize);
    }

    /**
     * Starts subscription and processes messages
     *
     * @param messageHandler Callback function to handle received messages
     */
    public void startSubscription(BiConsumer<String, Map<String, String>> messageHandler) {
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
            try {
                subscriber.stopAsync().awaitTerminated(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("Error stopping subscriber", e);
            }
        }
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
