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
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream);
            TopicName topicName = TopicName.of(projectId, topicId);
            this.publisher = Publisher.newBuilder(topicName)
                    .setCredentialsProvider(() -> credentials)
                    .build();
            logger.info("Created publisher with credentials from file: {}", credentialsPath);
        }
    }

    /**
     * Creates a publisher using specified credentials
     */
    public PubSubPublisher(String projectId, String topicId, GoogleCredentials credentials) throws IOException {
        TopicName topicName = TopicName.of(projectId, topicId);
        this.publisher = Publisher.newBuilder(topicName)
                .setCredentialsProvider(() -> credentials)
                .build();
    }

    /**
     * Publishes a message with attributes to the topic
     *
     * @param message Message content
     * @param attributes Optional message attributes
     * @return Message ID
     * @throws ExecutionException if the message fails to publish
     * @throws InterruptedException if the operation is interrupted
     */
    public String publish(String message, Map<String, String> attributes) throws ExecutionException, InterruptedException {
        ByteString data = ByteString.copyFromUtf8(message);
        PubsubMessage.Builder messageBuilder = PubsubMessage.newBuilder()
                .setData(data);

        if (attributes != null) {
            messageBuilder.putAllAttributes(attributes);
        }

        ApiFuture<String> future = publisher.publish(messageBuilder.build());
        return future.get();
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
     * Shuts down the publisher and releases resources
     *
     * @throws InterruptedException if the shutdown is interrupted
     */
    public void shutdown() throws InterruptedException {
        if (publisher != null) {
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
