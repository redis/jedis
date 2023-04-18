package redis.clients.jedis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

import redis.clients.jedis.Builder;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.providers.ConnectionProvider;

public abstract class JedisRoundRobinBase<R> {

  private final Builder<R> builder;

  private final Queue<Map.Entry> connections;

  private Map.Entry connection;

  private R lastReply;

  private boolean roundRobinCompleted;
  private boolean iterationCompleted;

  protected JedisRoundRobinBase(ConnectionProvider connectionProvider, Builder<R> responseBuilder) {
    Map connectionMap = connectionProvider.getConnectionMap();
    ArrayList<Map.Entry> connectionList = new ArrayList<>(connectionMap.entrySet());
    Collections.shuffle(connectionList);
    this.connections = new LinkedList<>(connectionList);
    this.builder = responseBuilder;
    this.iterationCompleted = true;
    this.roundRobinCompleted = this.connections.isEmpty();
  }

  public final boolean isRoundRobinCompleted() {
    return roundRobinCompleted;
  }

  protected abstract boolean isIterationCompleted(R reply);

  protected abstract CommandArguments initCommandArguments();

  protected abstract CommandArguments nextCommandArguments(R lastReply);

  public final R get() {
    if (roundRobinCompleted) {
      throw new NoSuchElementException();
    }

    CommandArguments args;
    if (iterationCompleted) {
      connection = connections.poll();
      args = initCommandArguments();
    } else {
      args = nextCommandArguments(lastReply);
    }

    Object rawReply;
    if (connection.getValue() instanceof Connection) {
      rawReply = ((Connection) connection.getValue()).executeCommand(args);
    } else if (connection.getValue() instanceof Pool) {
      try (Connection c = ((Pool<Connection>) connection.getValue()).getResource()) {
        rawReply = c.executeCommand(args);
      }
    } else {
      throw new IllegalArgumentException(connection.getValue().getClass() + "is not supported.");
    }

    lastReply = builder.build(rawReply);
    iterationCompleted = isIterationCompleted(lastReply);
    if (iterationCompleted) {
      if (connections.isEmpty()) {
        roundRobinCompleted = true;
      }
    }
    return lastReply;
  }
}
