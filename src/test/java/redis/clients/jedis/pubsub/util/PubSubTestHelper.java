package redis.clients.jedis.pubsub.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import redis.clients.jedis.JedisPubSub;

/**
 * Utility class for pub/sub testing.
 * Provides helpers for message synchronization, assertions, and common test patterns.
 */
public class PubSubTestHelper {

  private static final int DEFAULT_TIMEOUT_SECONDS = 5;

  /**
   * Creates a subscriber that captures messages and subscription channels.
   * Uses thread-safe collections to avoid synchronization overhead.
   */
  public static class MessageCapture extends JedisPubSub {
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final List<String> messageChannels = new CopyOnWriteArrayList<>();
    private final List<String> subscribedChannels = new CopyOnWriteArrayList<>();
    private final List<String> subscribedPatterns = new CopyOnWriteArrayList<>();
    private final List<String> unsubscribedChannels = new CopyOnWriteArrayList<>();
    private final CountDownLatch subscribeLatch;
    private final CountDownLatch psubscribeLatch;
    private final CountDownLatch messageLatch;
    private CountDownLatch unsubscribeLatch;

    public MessageCapture(int expectedSubscriptions, int expectedMessages) {
      this(expectedSubscriptions, 0, expectedMessages);
    }

    public MessageCapture(int expectedSubscriptions, int expectedPSubscriptions, int expectedMessages) {
      this.subscribeLatch = new CountDownLatch(expectedSubscriptions);
      this.psubscribeLatch = new CountDownLatch(expectedPSubscriptions);
      this.messageLatch = new CountDownLatch(expectedMessages);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
      this.subscribedChannels.add(channel);
      subscribeLatch.countDown();
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
      this.subscribedPatterns.add(pattern);
      psubscribeLatch.countDown();
    }

    @Override
    public void onMessage(String channel, String message) {
      messageChannels.add(channel);
      messages.add(message);
      messageLatch.countDown();
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
      messageChannels.add(channel);
      messages.add(message);
      messageLatch.countDown();
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
      unsubscribedChannels.add(channel);
      if (unsubscribeLatch != null) {
        unsubscribeLatch.countDown();
      }
    }

    public void expectUnsubscribe(int count) {
      this.unsubscribeLatch = new CountDownLatch(count);
    }

    public boolean awaitSubscription() throws InterruptedException {
      return subscribeLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean awaitUnsubscribe() throws InterruptedException {
      return unsubscribeLatch != null && unsubscribeLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean awaitPSubscription() throws InterruptedException {
      return psubscribeLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean awaitMessages() throws InterruptedException {
      return messageLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public List<String> getMessages() {
      return new ArrayList<>(messages);
    }

    public List<String> getChannels() {
      return new ArrayList<>(subscribedChannels);
    }

    public List<String> getPatterns() {
      return new ArrayList<>(subscribedPatterns);
    }

    public List<String> getMessageChannels() {
      return new ArrayList<>(messageChannels);
    }
  }

  /**
   * Creates a subscriber that unsubscribes after receiving a message.
   */
  public static class AutoUnsubscriber extends JedisPubSub {
    private final CountDownLatch subscribeLatch = new CountDownLatch(1);
    private final CountDownLatch messageLatch = new CountDownLatch(1);
    private final AtomicReference<String> receivedMessage = new AtomicReference<>();

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
      subscribeLatch.countDown();
    }

    @Override
    public void onMessage(String channel, String message) {
      receivedMessage.set(message);
      messageLatch.countDown();
      unsubscribe();
    }

    public boolean awaitSubscription() throws InterruptedException {
      return subscribeLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean awaitMessage() throws InterruptedException {
      return messageLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public String getReceivedMessage() {
      return receivedMessage.get();
    }
  }

  /**
   * Creates a subscriber that supports PING during subscription.
   */
  public static class PingPongSubscriber extends JedisPubSub {
    private final CountDownLatch subscribeLatch = new CountDownLatch(1);
    private final CountDownLatch pongLatch = new CountDownLatch(1);
    private final AtomicReference<String> pongResponse = new AtomicReference<>();

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
      subscribeLatch.countDown();
    }

    @Override
    public void onPong(String pattern) {
      pongResponse.set(pattern);
      pongLatch.countDown();
      unsubscribe();
    }

    public boolean awaitSubscription() throws InterruptedException {
      return subscribeLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean awaitPong() throws InterruptedException {
      return pongLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public String getPongResponse() {
      return pongResponse.get();
    }
  }

  private PubSubTestHelper() {
    // Utility class - prevent instantiation
  }
}

