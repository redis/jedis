package redis.clients.jedis;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class MultiNodePipelineBase implements Closeable {

  private final Map<HostAndPort, Queue<Response<?>>> pipelinedResponses;
  private final Map<HostAndPort, Connection> connections;
  private volatile boolean synced;

  public MultiNodePipelineBase() {
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
    synced = false;
  }

  protected abstract Connection getConnection(HostAndPort nodeKey);

  protected final <T> Response<T> appendCommand(HostAndPort nodeKey, CommandObject<T> commandObject) {
    Queue<Response<?>> queue;
    Connection connection;
    if (pipelinedResponses.containsKey(nodeKey)) {
      queue = pipelinedResponses.get(nodeKey);
      connection = connections.get(nodeKey);
    } else {
      queue = new LinkedList<>();
      connection = getConnection(nodeKey);
      pipelinedResponses.put(nodeKey, queue);
      connections.put(nodeKey, connection);
    }

    connection.sendCommand(commandObject.getArguments());
    Response<T> response = new Response<>(commandObject.getBuilder());
    queue.add(response);
    return response;
  }

  @Override
  public final void close() {
    sync();
    for (Connection connection : connections.values()) {
      connection.close();
    }
  }

  public final void sync() {
    if (synced) {
      return;
    }
    for (Map.Entry<HostAndPort, Queue<Response<?>>> entry : pipelinedResponses.entrySet()) {
      HostAndPort nodeKey = entry.getKey();
      Queue<Response<?>> queue = entry.getValue();
      List<Object> unformatted = connections.get(nodeKey).getMany(queue.size());
      for (Object o : unformatted) {
        queue.poll().set(o);
      }
    }
    synced = true;
  }
}
