package redis.clients.jedis.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The collection of replies where a command is broadcasted to multiple nodes, but the replies are expected to differ
 * even in ideal situation.
 */
public class JedisBroadcastReplies {

  private static final Object DUMMY_OBJECT = new Object();

  private final Map<Object, Object> replies;

  public JedisBroadcastReplies() {
    this(new HashMap<>());
  }

  public JedisBroadcastReplies(int size) {
    this(new HashMap<>(size));
  }

  private JedisBroadcastReplies(Map<Object, Object> replies) {
    this.replies = replies;
  }

  public void addReply(Object node, Object reply) {
    replies.put(node, reply);
  }

  public Map<Object, Object> getReplies() {
    return Collections.unmodifiableMap(replies);
  }

  public static JedisBroadcastReplies singleton(Object reply) {
    return new JedisBroadcastReplies(Collections.singletonMap(DUMMY_OBJECT, reply));
  }
}
