package redis.clients.jedis;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

public abstract class MultiNodePipelineBase extends AbstractPipeline {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * The number of processes for {@code sync()}. If you have enough cores for client (and you have
   * more than 3 cluster nodes), you may increase this number of workers.
   * Suggestion:&nbsp;&le;&nbsp;cluster&nbsp;nodes.
   *
   * @deprecated Client using this approach are paying the thread creation cost for every pipeline sync. Clients
   * should use refer to {@link JedisClientConfig#getPipelineExecutorProvider()} to provide a single Executor for
   * gain in performance.
   */
  public static volatile int MULTI_NODE_PIPELINE_SYNC_WORKERS = 3;

  private final Map<HostAndPort, Queue<Response<?>>> pipelinedResponses;
  private final Map<HostAndPort, Connection> connections;
  private ClusterPipelineExecutor clusterPipelineExecutor;
  private boolean useSharedExecutor = false;
  private volatile boolean syncing = false;

  public MultiNodePipelineBase(CommandObjects commandObjects) {
    super(commandObjects);
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
  }

  public MultiNodePipelineBase(CommandObjects commandObjects, ClusterPipelineExecutor executorService) {
    super(commandObjects);
    clusterPipelineExecutor = executorService;
    useSharedExecutor = clusterPipelineExecutor != null;
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
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
    ClusterPipelineExecutor executorService = getExecutorService();
    CompletableFuture[] futures
            = pipelinedResponses.entrySet().stream()
            .map(response -> CompletableFuture.runAsync(() -> readCommandResponse(response), executorService))
            .toArray(CompletableFuture[]::new);
    CompletableFuture awaitAllCompleted = CompletableFuture.allOf(futures);
    try {
        awaitAllCompleted.get();
        if (!useSharedExecutor) {
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

  private ClusterPipelineExecutor getExecutorService() {
    if (useSharedExecutor) {
      return clusterPipelineExecutor;
    }
    return ClusterPipelineExecutor.from(
            Executors.newFixedThreadPool(Math.min(this.pipelinedResponses.size(), MULTI_NODE_PIPELINE_SYNC_WORKERS)));
  }

  private void readCommandResponse(Map.Entry<HostAndPort, Queue<Response<?>>> entry) {
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
