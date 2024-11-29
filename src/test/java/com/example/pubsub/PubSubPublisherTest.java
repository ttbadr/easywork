package com.example.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PubSubPublisherTest {

    private static final String PROJECT_ID = "test-project";
    private static final String TOPIC_ID = "test-topic";
    private static final String MESSAGE_ID = "test-message-id";

    @Mock
    private Publisher mockPublisher;

    @Mock
    private GoogleCredentials mockCredentials;

    private PubSubPublisher pubSubPublisher;

    @Before
    public void setUp() throws Exception {
        pubSubPublisher = new PubSubPublisher(PROJECT_ID, TOPIC_ID) {
            @Override
            protected Publisher createPublisher(GoogleCredentials credentials) throws IOException {
                return mockPublisher;
            }

            @Override
            protected GoogleCredentials getCredentials() {
                return mockCredentials;
            }
        };
    }

    @Test
    public void publish_WithValidMessage_ShouldSucceed() throws Exception {
        // Arrange
        String message = "test message";
        Map<String, String> attributes = new HashMap<>();
        attributes.put("key", "value");

        ApiFuture<String> future = ApiFutures.immediateFuture(MESSAGE_ID);
        when(mockPublisher.publish(any(PubsubMessage.class))).thenReturn(future);

        // Act
        String resultMessageId = pubSubPublisher.publish(message, attributes);

        // Assert
        assertThat(resultMessageId).isEqualTo(MESSAGE_ID);
        verify(mockPublisher).publish(any(PubsubMessage.class));
    }

    @Test
    public void publish_WithNullMessage_ShouldThrowException() {
        // Arrange
        String message = null;
        Map<String, String> attributes = new HashMap<>();

        // Act & Assert
        assertThatThrownBy(() -> pubSubPublisher.publish(message, attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message cannot be null");
    }

    @Test
    public void publish_WhenPublisherFails_ShouldThrowException() throws Exception {
        // Arrange
        String message = "test message";
        Map<String, String> attributes = new HashMap<>();
        
        when(mockPublisher.publish(any(PubsubMessage.class)))
                .thenReturn(ApiFutures.immediateFailedFuture(new RuntimeException("Publish failed")));

        // Act & Assert
        assertThatThrownBy(() -> pubSubPublisher.publish(message, attributes))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseMessage("Publish failed");
    }

    @Test
    public void shutdown_ShouldClosePublisher() throws Exception {
        // Act
        pubSubPublisher.shutdown();

        // Assert
        verify(mockPublisher).shutdown();
    }
}
