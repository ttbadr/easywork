package com.example.pubsub;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PubSubPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PubSubPublisher.class);
    private final Publisher publisher;
    private final String projectId;
    private final String topicId;

    /**
     * Creates a publisher using default credentials
     */
    public PubSubPublisher(String projectId, String topicId) throws IOException {
        this(projectId, topicId, GoogleCredentials.getApplicationDefault());
    }

    /**
     * Creates a publisher using service account key file
     *
     * @param projectId Project ID
     * @param topicId Topic ID
     * @param credentialsPath Path to service account key file
     * @throws IOException if unable to read credentials file
     */
    public PubSubPublisher(String projectId, String topicId, String credentialsPath) throws IOException {
        this(projectId, topicId, credentialsPath, null);
    }

    /**
     * Creates a publisher using service account key file
     *
     * @param projectId Project ID
     * @param topicId Topic ID
     * @param credentialsPath Path to service account key file
     * @param credentials GoogleCredentials
     * @throws IOException if unable to read credentials file
     */
    public PubSubPublisher(String projectId, String topicId, String credentialsPath, GoogleCredentials credentials) throws IOException {
        this.projectId = projectId;
        this.topicId = topicId;
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            if (credentials == null) {
                credentials = ServiceAccountCredentials.fromStream(credentialsStream);
            }
            this.publisher = createPublisher(credentials);
            logger.info("Created publisher with credentials from file: {}", credentialsPath);
        }
    }

    /**
     * Creates a publisher using specified credentials
     */
    public PubSubPublisher(String projectId, String topicId, GoogleCredentials credentials) throws IOException {
        this.projectId = projectId;
        this.topicId = topicId;
        this.publisher = createPublisher(credentials);
        logger.info("Created publisher for topic: {}", topicId);
    }

    protected Publisher createPublisher(GoogleCredentials credentials) throws IOException {
        TopicName topicName = TopicName.of(projectId, topicId);
        return Publisher.newBuilder(topicName)
                .setCredentialsProvider(() -> credentials)
                .build();
    }

    protected GoogleCredentials getCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }

    /**
     * Publishes a message to the topic
     *
     * @param message Message to publish
     * @param attributes Optional attributes to attach to the message
     * @return Message ID
     * @throws ExecutionException if publishing fails
     * @throws InterruptedException if publishing is interrupted
     */
    public String publish(String message, Map<String, String> attributes) throws ExecutionException, InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        PubsubMessage.Builder messageBuilder = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(message));

        if (attributes != null) {
            messageBuilder.putAllAttributes(attributes);
        }

        ApiFuture<String> future = publisher.publish(messageBuilder.build());
        String messageId = future.get();
        logger.debug("Published message with ID: {}", messageId);
        return messageId;
    }

    /**
     * Publishes a message without attributes to the topic
     *
     * @param message Message content
     * @return Message ID
     * @throws ExecutionException if the message fails to publish
     * @throws InterruptedException if the operation is interrupted
     */
    public String publish(String message) throws ExecutionException, InterruptedException {
        return publish(message, null);
    }

    /**
     * Shuts down the publisher
     */
    public void shutdown() throws Exception {
        if (publisher != null) {
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
            logger.info("Publisher shut down successfully");
        }
    }
}
