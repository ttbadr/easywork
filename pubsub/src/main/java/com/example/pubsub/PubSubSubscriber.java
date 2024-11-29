package com.example.pubsub;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class PubSubSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(PubSubSubscriber.class);
    private final String projectId;
    private final String subscriptionId;
    private Subscriber subscriber;
    private SubscriptionAdminClient subscriptionAdminClient;
    private final ExecutorProvider executorProvider;

    /**
     * Creates a subscriber using default credentials
     */
    public PubSubSubscriber(String projectId, String subscriptionId) throws IOException {
        this(projectId, subscriptionId, GoogleCredentials.getApplicationDefault());
    }

    /**
     * Creates a subscriber using service account key file
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath) throws IOException {
        this(projectId, subscriptionId, ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath)));
    }

    /**
     * Creates a subscriber using specified credentials
     */
    public PubSubSubscriber(String projectId, String subscriptionId, GoogleCredentials credentials) throws IOException {
        this(projectId, subscriptionId, credentials, 1);
    }

    /**
     * Creates a subscriber using default credentials with a custom thread pool size
     */
    public PubSubSubscriber(String projectId, String subscriptionId, int threadPoolSize) throws IOException {
        this(projectId, subscriptionId, GoogleCredentials.getApplicationDefault(), threadPoolSize);
    }

    /**
     * Creates a subscriber using service account key file with a custom thread pool size
     */
    public PubSubSubscriber(String projectId, String subscriptionId, String credentialsPath, int threadPoolSize) throws IOException {
        this(projectId, subscriptionId, ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath)), threadPoolSize);
    }

    /**
     * Creates a subscriber using specified credentials and custom thread pool size
     */
    public PubSubSubscriber(String projectId, String subscriptionId, GoogleCredentials credentials, int threadPoolSize) throws IOException {
        this.projectId = projectId;
        this.subscriptionId = subscriptionId;
        this.executorProvider = InstantiatingExecutorProvider.newBuilder()
                .setExecutorThreadCount(threadPoolSize)
                .build();
    }

    protected GoogleCredentials getCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }

    protected Subscriber createSubscriber(MessageReceiver receiver) throws IOException {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        return Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(() -> {
                    try {
                        return getCredentials();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get credentials", e);
                    }
                })
                .setExecutorProvider(executorProvider)
                .build();
    }

    protected SubscriptionAdminClient createSubscriptionAdminClient() throws IOException {
        return SubscriptionAdminClient.create();
    }

    /**
     * Starts subscription in push mode with a message handler
     */
    public void startSubscription(BiConsumer<String, Map<String, String>> messageHandler) throws IOException {
        MessageReceiver receiver = (message, consumer) -> {
            try {
                String messageData = message.getData().toStringUtf8();
                Map<String, String> attributes = message.getAttributesMap();
                messageHandler.accept(messageData, attributes);
                consumer.ack();
            } catch (Exception e) {
                logger.error("Error processing message: {}", e.getMessage());
                consumer.nack();
            }
        };

        subscriber = createSubscriber(receiver);
        subscriber.startAsync().awaitRunning();
        logger.info("Started subscription: {}", subscriptionId);
    }

    /**
     * Pulls messages in synchronous mode
     */
    public List<ReceivedMessage> pullMessages(int maxMessages) throws IOException {
        if (subscriptionAdminClient == null) {
            subscriptionAdminClient = createSubscriptionAdminClient();
        }

        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        PullRequest pullRequest = PullRequest.newBuilder()
                .setMaxMessages(maxMessages)
                .setSubscription(subscriptionName.toString())
                .build();

        PullResponse pullResponse = subscriptionAdminClient.pull(pullRequest);
        logger.debug("Received {} messages", pullResponse.getReceivedMessagesCount());
        return pullResponse.getReceivedMessagesList();
    }

    /**
     * Acknowledges messages
     */
    public void acknowledgeMessages(List<ReceivedMessage> messages) throws IOException {
        if (subscriptionAdminClient == null) {
            subscriptionAdminClient = createSubscriptionAdminClient();
        }

        List<String> ackIds = new ArrayList<>();
        for (ReceivedMessage message : messages) {
            ackIds.add(message.getAckId());
        }

        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
                .setSubscription(subscriptionName.toString())
                .addAllAckIds(ackIds)
                .build();

        subscriptionAdminClient.acknowledge(acknowledgeRequest);
        logger.debug("Acknowledged {} messages", ackIds.size());
    }

    /**
     * Stops the subscription and releases resources
     */
    public void stopSubscription() throws Exception {
        if (subscriber != null) {
            subscriber.stopAsync().awaitTerminated();
            logger.info("Stopped subscription: {}", subscriptionId);
        }

        if (subscriptionAdminClient != null) {
            subscriptionAdminClient.close();
            logger.info("Closed subscription admin client");
        }
    }
}
