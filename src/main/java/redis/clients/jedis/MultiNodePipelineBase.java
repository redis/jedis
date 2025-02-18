package redis.clients.jedis;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

public abstract class MultiNodePipelineBase extends PipelineBase {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * The number of processes for {@code sync()}. If you have enough cores for client (and you have
   * more than 3 cluster nodes), you may increase this number of workers.
   * Suggestion:&nbsp;&le;&nbsp;cluster&nbsp;nodes.
   */
  public static volatile int MULTI_NODE_PIPELINE_SYNC_WORKERS = 3;

  private final Map<HostAndPort, Queue<Response<?>>> pipelinedResponses;
  private final Map<HostAndPort, Connection> connections;
  private ExecutorService executorService;
  private volatile boolean syncing = false;

  public MultiNodePipelineBase(CommandObjects commandObjects) {
    super(commandObjects);
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
  }


  public MultiNodePipelineBase(CommandObjects commandObjects, ExecutorService executorService) {
    super(commandObjects);
    this.executorService = executorService;
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
  }

  /**
   * Sub-classes must call this method, if graph commands are going to be used.
   * @param connectionProvider connection provider
   */
  protected final void prepareGraphCommands(ConnectionProvider connectionProvider) {
    GraphCommandObjects graphCommandObjects = new GraphCommandObjects(connectionProvider);
    graphCommandObjects.setBaseCommandArgumentsCreator((comm) -> this.commandObjects.commandArguments(comm));
    super.setGraphCommands(graphCommandObjects);
  }
  
  protected abstract HostAndPort getNodeKey(CommandArguments args);

  protected abstract Connection getConnection(HostAndPort nodeKey);

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    HostAndPort nodeKey = getNodeKey(commandObject.getArguments());

    Queue<Response<?>> queue;
    Connection connection;
    if (pipelinedResponses.containsKey(nodeKey)) {
      queue = pipelinedResponses.get(nodeKey);
      connection = connections.get(nodeKey);
    } else {
      Connection newOne = getConnection(nodeKey);
      connections.putIfAbsent(nodeKey, newOne);
      connection = connections.get(nodeKey);
      if (connection != newOne) {
        log.debug("Duplicate connection to {}, closing it.", nodeKey);
        IOUtils.closeQuietly(newOne);
      }

      pipelinedResponses.putIfAbsent(nodeKey, new LinkedList<>());
      queue = pipelinedResponses.get(nodeKey);
    }

    connection.sendCommand(commandObject.getArguments());
    Response<T> response = new Response<>(commandObject.getBuilder());
    queue.add(response);
    return response;
  }

  @Override
  public void close() {
    try {
      sync();
    } finally {
      connections.values().forEach(IOUtils::closeQuietly);
    }
  }

  @Override
  public final void sync() {
    if (syncing) {
      return;
    }
    syncing = true;
    ExecutorService executorService = getExecutorService();
    CompletableFuture[] futures
            = pipelinedResponses.entrySet().stream()
            .map(e -> CompletableFuture.runAsync(() -> closeConnection(e), executorService))
            .toArray(CompletableFuture[]::new);
    CompletableFuture awaitAllCompleted = CompletableFuture.allOf(futures);
    try {
        awaitAllCompleted.get();
        if (executorService != this.executorService) {
          executorService.shutdown();
        }
    } catch (ExecutionException e) {
      log.error("Failed execution.", e);
    } catch (InterruptedException e) {
      log.error("Thread is interrupted during sync.", e);
      Thread.currentThread().interrupt();
    }
    syncing = false;
  }

  private ExecutorService getExecutorService() {
    if (executorService == null) {
      return Executors.newFixedThreadPool(Math.min(this.pipelinedResponses.size(), MULTI_NODE_PIPELINE_SYNC_WORKERS));
    }
    return executorService;
  }

  private void closeConnection(Map.Entry<HostAndPort, Queue<Response<?>>> entry) {
    HostAndPort nodeKey = entry.getKey();
    Queue<Response<?>> queue = entry.getValue();
    Connection connection = connections.get(nodeKey);
    try {
      List<Object> unformatted = connection.getMany(queue.size());
      for (Object o : unformatted) {
        queue.poll().set(o);
      }
    } catch (JedisConnectionException jce) {
      log.error("Error with connection to " + nodeKey, jce);
      connections.remove(nodeKey);
      IOUtils.closeQuietly(connection);
    }
  }

  @Deprecated
  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }
}
