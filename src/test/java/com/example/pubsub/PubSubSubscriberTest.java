package com.example.pubsub;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PubSubSubscriberTest {

    private static final String PROJECT_ID = "test-project";
    private static final String SUBSCRIPTION_ID = "test-subscription";

    @Mock
    private Subscriber mockSubscriber;

    @Mock
    private GoogleCredentials mockCredentials;

    @Mock
    private SubscriptionAdminClient mockSubscriptionAdminClient;

    private PubSubSubscriber pubSubSubscriber;

    @Before
    public void setUp() throws Exception {
        pubSubSubscriber = new PubSubSubscriber(PROJECT_ID, SUBSCRIPTION_ID) {
            @Override
            protected GoogleCredentials getCredentials() throws IOException {
                return mockCredentials;
            }

            @Override
            protected Subscriber createSubscriber(MessageReceiver receiver) throws IOException {
                return mockSubscriber;
            }

            @Override
            protected SubscriptionAdminClient createSubscriptionAdminClient() throws IOException {
                return mockSubscriptionAdminClient;
            }
        };
    }

    @Test
    public void startSubscription_ShouldStartSubscriberAsync() throws Exception {
        // Arrange
        when(mockSubscriber.startAsync()).thenReturn(mockSubscriber);

        CountDownLatch messageReceived = new CountDownLatch(1);
        BiConsumer<String, Map<String, String>> messageHandler = (message, attributes) -> {
            assertThat(message).isEqualTo("test message");
            assertThat(attributes).containsEntry("key", "value");
            messageReceived.countDown();
        };

        // Act
        pubSubSubscriber.startSubscription(messageHandler);

        // Simulate message reception
        MessageReceiver receiver = mock(MessageReceiver.class);
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFromUtf8("test message"))
                .putAttributes("key", "value")
                .build();
        AckReplyConsumer consumer = mock(AckReplyConsumer.class);

        // Assert
        verify(mockSubscriber).startAsync();
        verify(mockSubscriber).awaitRunning();
    }

    @Test
    public void pullMessages_ShouldReturnReceivedMessages() throws Exception {
        // Arrange
        List<ReceivedMessage> expectedMessages = new ArrayList<>();
        ReceivedMessage message = ReceivedMessage.newBuilder()
                .setMessage(PubsubMessage.newBuilder()
                        .setData(com.google.protobuf.ByteString.copyFromUtf8("test message"))
                        .putAttributes("key", "value")
                        .build())
                .setAckId("test-ack-id")
                .build();
        expectedMessages.add(message);

        PullResponse pullResponse = PullResponse.newBuilder()
                .addAllReceivedMessages(expectedMessages)
                .build();

        when(mockSubscriptionAdminClient.pull(any(PullRequest.class))).thenReturn(pullResponse);

        // Act
        List<ReceivedMessage> actualMessages = pubSubSubscriber.pullMessages(1);

        // Assert
        assertThat(actualMessages).hasSize(1);
        assertThat(actualMessages.get(0).getMessage().getData().toStringUtf8()).isEqualTo("test message");
        assertThat(actualMessages.get(0).getMessage().getAttributesMap()).containsEntry("key", "value");
        verify(mockSubscriptionAdminClient).pull(any(PullRequest.class));
    }

    @Test
    public void acknowledgeMessages_ShouldAcknowledgeAllMessages() throws Exception {
        // Arrange
        List<ReceivedMessage> messages = new ArrayList<>();
        ReceivedMessage message = ReceivedMessage.newBuilder()
                .setAckId("test-ack-id")
                .build();
        messages.add(message);

        // Act
        pubSubSubscriber.acknowledgeMessages(messages);

        // Assert
        verify(mockSubscriptionAdminClient).acknowledge(any(AcknowledgeRequest.class));
    }

    @Test
    public void stopSubscription_ShouldStopSubscriberAndExecutor() throws Exception {
        // Arrange
        when(mockSubscriber.stopAsync()).thenReturn(mockSubscriber);

        // Start subscription first
        pubSubSubscriber.startSubscription((message, attributes) -> {});

        // Act
        pubSubSubscriber.stopSubscription();

        // Assert
        verify(mockSubscriber).stopAsync();
        verify(mockSubscriber).awaitTerminated();
    }
}
