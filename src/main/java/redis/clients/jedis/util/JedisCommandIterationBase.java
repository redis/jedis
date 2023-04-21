package redis.clients.jedis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Supplier;

import redis.clients.jedis.Builder;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * @param <B> Type of each batch reply
 * @param <D> Type of each data
 */
public abstract class JedisCommandIterationBase<B, D> {

  private final Builder<B> builder;

  private final Queue<Map.Entry> connections;

  private Map.Entry connection;

  private B lastReply;

  private boolean roundRobinCompleted;
  private boolean iterationCompleted;

  protected JedisCommandIterationBase(ConnectionProvider connectionProvider, Builder<B> responseBuilder) {
    Map connectionMap = connectionProvider.getConnectionMap();
    ArrayList<Map.Entry> connectionList = new ArrayList<>(connectionMap.entrySet());
    Collections.shuffle(connectionList);
    this.connections = new LinkedList<>(connectionList);
    this.builder = responseBuilder;
    this.iterationCompleted = true;
    this.roundRobinCompleted = this.connections.isEmpty();
  }

  public final boolean isIterationCompleted() {
    return roundRobinCompleted;
  }

  protected abstract boolean isNodeCompleted(B reply);

  protected abstract CommandArguments initCommandArguments();

  protected abstract CommandArguments nextCommandArguments(B lastReply);

  public final B nextBatch() {
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
    iterationCompleted = isNodeCompleted(lastReply);
    if (iterationCompleted) {
      if (connections.isEmpty()) {
        roundRobinCompleted = true;
      }
    }
    return lastReply;
  }

  protected abstract Collection<D> convertBatchToData(B batch);

  public final Collection<D> nextBatchList() {
    return convertBatchToData(nextBatch());
  }

  public final Collection<D> collect(Collection<D> c) {
    while (!isIterationCompleted()) {
      c.addAll(nextBatchList());
    }
    return c;
  }
}
