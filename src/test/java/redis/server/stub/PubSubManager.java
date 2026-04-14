package redis.server.stub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import redis.server.stub.util.GlobPatternMatcher;

/**
 * Manages pub/sub subscriptions for the Redis server stub.
 * <p>
 * This is the SINGLE SOURCE OF TRUTH for all subscription state. Subscription state is NOT tracked
 * in ClientState.
 * <p>
 * <b>Decoupled Design</b>: Works with {@link Subscriber} interface instead of {@link ClientHandler}
 * directly. This enables:
 * <ul>
 * <li>Testing with mock subscribers</li>
 * <li>Clean separation of concerns</li>
 * <li>Future subscriber types (e.g., internal server subscribers)</li>
 * </ul>
 * <p>
 * <b>Thread Safety</b>: This class is NOT thread-safe by design. It is accessed ONLY from the
 * single-threaded commandExecutor in RedisServerStub, so no synchronization is needed.
 * <p>
 * <b>Bidirectional Tracking</b>: Uses bidirectional mappings for O(1) lookups:
 * <ul>
 * <li>Publishing: "Which subscribers are on channel X?" → O(1)</li>
 * <li>Protocol: "How many subscriptions does this subscriber have?" → O(1)</li>
 * </ul>
 */
public class PubSubManager {

  // Bidirectional tracking for exact channel subscriptions
  // channel -> subscribers (for publishing)
  private final Map<String, Set<Subscriber>> channelSubscribers = new HashMap<>();
  // subscriber -> channels (for counting subscriptions)
  private final Map<Subscriber, Set<String>> subscriberChannels = new HashMap<>();

  // Bidirectional tracking for pattern subscriptions
  // pattern -> subscribers (for pattern matching)
  private final Map<String, Set<Subscriber>> patternSubscribers = new HashMap<>();
  // subscriber -> patterns (for counting pattern subscriptions)
  private final Map<Subscriber, Set<String>> subscriberPatterns = new HashMap<>();

  /**
   * Subscribe a subscriber to a channel. Updates both channelSubscribers and subscriberChannels
   * maps.
   * @param subscriber the subscriber
   * @param channel the channel name
   */
  public void subscribe(Subscriber subscriber, String channel) {
    // Add to channel -> subscribers mapping
    channelSubscribers.computeIfAbsent(channel, k -> new HashSet<>()).add(subscriber);

    // Add to subscriber -> channels mapping
    subscriberChannels.computeIfAbsent(subscriber, k -> new HashSet<>()).add(channel);
  }

  /**
   * Unsubscribe a subscriber from a channel. Updates both channelSubscribers and subscriberChannels
   * maps.
   * @param subscriber the subscriber
   * @param channel the channel name
   */
  public void unsubscribe(Subscriber subscriber, String channel) {
    // Remove from channel -> subscribers mapping
    Set<Subscriber> subscribers = channelSubscribers.get(channel);
    if (subscribers != null) {
      subscribers.remove(subscriber);
      if (subscribers.isEmpty()) {
        channelSubscribers.remove(channel);
      }
    }

    // Remove from subscriber -> channels mapping
    Set<String> channels = subscriberChannels.get(subscriber);
    if (channels != null) {
      channels.remove(channel);
      if (channels.isEmpty()) {
        subscriberChannels.remove(subscriber);
      }
    }
  }

  /**
   * Subscribe a subscriber to a pattern. Updates both patternSubscribers and subscriberPatterns
   * maps.
   * @param subscriber the subscriber
   * @param pattern the pattern (e.g., "news.*")
   */
  public void psubscribe(Subscriber subscriber, String pattern) {
    // Add to pattern -> subscribers mapping
    patternSubscribers.computeIfAbsent(pattern, k -> new HashSet<>()).add(subscriber);

    // Add to subscriber -> patterns mapping
    subscriberPatterns.computeIfAbsent(subscriber, k -> new HashSet<>()).add(pattern);
  }

  /**
   * Unsubscribe a subscriber from a pattern. Updates both patternSubscribers and subscriberPatterns
   * maps.
   * @param subscriber the subscriber
   * @param pattern the pattern
   */
  public void punsubscribe(Subscriber subscriber, String pattern) {
    // Remove from pattern -> subscribers mapping
    Set<Subscriber> subscribers = patternSubscribers.get(pattern);
    if (subscribers != null) {
      subscribers.remove(subscriber);
      if (subscribers.isEmpty()) {
        patternSubscribers.remove(pattern);
      }
    }

    // Remove from subscriber -> patterns mapping
    Set<String> patterns = subscriberPatterns.get(subscriber);
    if (patterns != null) {
      patterns.remove(pattern);
      if (patterns.isEmpty()) {
        subscriberPatterns.remove(subscriber);
      }
    }
  }

  /**
   * Unsubscribe a subscriber from all channels and patterns. Called when subscriber disconnects.
   * @param subscriber the subscriber
   */
  public void unsubscribeAll(Subscriber subscriber) {
    // Unsubscribe from all channels
    Set<String> channels = subscriberChannels.get(subscriber);
    if (channels != null) {
      // Copy set to avoid concurrent modification
      for (String channel : new HashSet<>(channels)) {
        unsubscribe(subscriber, channel);
      }
    }

    // Unsubscribe from all patterns
    Set<String> patterns = subscriberPatterns.get(subscriber);
    if (patterns != null) {
      // Copy set to avoid concurrent modification
      for (String pattern : new HashSet<>(patterns)) {
        punsubscribe(subscriber, pattern);
      }
    }
  }

  /**
   * Get total subscription count for subscriber (channels + patterns). This is needed for
   * SUBSCRIBE/UNSUBSCRIBE protocol responses.
   * @param subscriber the subscriber
   * @return number of active subscriptions (channels + patterns)
   */
  public int getSubscriptionCount(Subscriber subscriber) {
    int count = 0;

    Set<String> channels = subscriberChannels.get(subscriber);
    if (channels != null) {
      count += channels.size();
    }

    Set<String> patterns = subscriberPatterns.get(subscriber);
    if (patterns != null) {
      count += patterns.size();
    }

    return count;
  }

  /**
   * Check if subscriber is in pub/sub mode (has any subscriptions). Subscriber is in pub/sub mode
   * if subscriptionCount &gt; 0.
   * @param subscriber the subscriber
   * @return true if subscriber has any subscriptions (channels or patterns)
   */
  public boolean isInPubSubMode(Subscriber subscriber) {
    return getSubscriptionCount(subscriber) > 0;
  }

  /**
   * Get all channels a subscriber is subscribed to.
   * @param subscriber the subscriber
   * @return set of channel names (empty if none)
   */
  public Set<String> getSubscriberChannels(Subscriber subscriber) {
    Set<String> channels = subscriberChannels.get(subscriber);
    return channels != null ? new HashSet<>(channels) : new HashSet<>();
  }

  /**
   * Get all patterns a subscriber is subscribed to.
   * @param subscriber the subscriber
   * @return set of pattern strings (empty if none)
   */
  public Set<String> getSubscriberPatterns(Subscriber subscriber) {
    Set<String> patterns = subscriberPatterns.get(subscriber);
    return patterns != null ? new HashSet<>(patterns) : new HashSet<>();
  }

  /**
   * Get all subscribers for a channel (for testing).
   * @param channel the channel name
   * @return set of subscribers (empty if none)
   */
  public Set<Subscriber> getChannelSubscribers(String channel) {
    Set<Subscriber> subscribers = channelSubscribers.get(channel);
    return subscribers != null ? new HashSet<>(subscribers) : new HashSet<>();
  }

  /**
   * Get all subscribers for a pattern (for testing).
   * @param pattern the pattern string
   * @return set of subscribers (empty if none)
   */
  public Set<Subscriber> getPatternSubscribers(String pattern) {
    Set<Subscriber> subscribers = patternSubscribers.get(pattern);
    return subscribers != null ? new HashSet<>(subscribers) : new HashSet<>();
  }

  /**
   * Publish message to channel. Sends to exact subscribers and pattern matches.
   * <p>
   * Assumes all subscribers in the maps are valid and active. Inactive subscribers are removed via
   * UNSUBSCRIBE commands or {@link #unsubscribeAll(Subscriber)} on disconnect.
   * @param channel the channel name
   * @param message the message to publish
   * @return set of subscribers that received the message
   */
  public Set<Subscriber> publish(String channel, String message) {
    Set<Subscriber> recipients = new HashSet<>();

    // 1. Send to exact channel subscribers
    Set<Subscriber> exactSubs = channelSubscribers.get(channel);
    if (exactSubs != null) {
      for (Subscriber subscriber : exactSubs) {
        // Send regular message push: >3 $7 message $7 channel $... message
        subscriber.sendPushMessage("message", channel, message);
        recipients.add(subscriber);
      }
    }

    // 2. Send to pattern subscribers (if pattern matches channel)
    for (Map.Entry<String, Set<Subscriber>> entry : patternSubscribers.entrySet()) {
      String pattern = entry.getKey();

      // Check if pattern matches channel
      if (GlobPatternMatcher.matches(pattern.getBytes(), channel.getBytes())) {
        for (Subscriber subscriber : entry.getValue()) {
          // Avoid sending duplicate if already received via exact subscription
          if (!recipients.contains(subscriber)) {
            // Send pattern message push: >4 $8 pmessage $... pattern $... channel $... message
            subscriber.sendPushMessage("pmessage", pattern, channel, message);
            recipients.add(subscriber);
          }
        }
      }
    }

    return recipients;
  }
}
