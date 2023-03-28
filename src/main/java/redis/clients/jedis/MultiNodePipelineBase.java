package redis.clients.jedis;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.commands.PipelineBinaryCommands;
import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.commands.RedisModulePipelineCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.graph.GraphCommandObjects;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public abstract class MultiNodePipelineBase extends PipelineBase
    implements PipelineCommands, PipelineBinaryCommands, RedisModulePipelineCommands, Closeable {

  private static final Logger log = LoggerFactory.getLogger(MultiNodePipelineBase.class);

  private final Map<HostAndPort, Queue<Response<?>>> pipelinedResponses;
  private final Map<HostAndPort, Connection> connections;
  private volatile boolean syncing = false;
  /**
   * The following are the default parameters for the multi node pipeline executor
   * Since Redis query is usually a slower IO operation (requires more threads),
   * so we set DEFAULT_CORE_POOL_SIZE to be the same as the core
   */
  private static final long DEFAULT_KEEPALIVE_TIME_MS = 60000L;
  private static final int DEFAULT_BLOCKING_QUEUE_SIZE = Protocol.CLUSTER_HASHSLOTS;
  private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
  private static final int DEFAULT_MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
  private static ExecutorService executorService = JedisThreadPoolBuilder.pool()
      .setCoreSize(DEFAULT_CORE_POOL_SIZE)
      .setMaxSize(DEFAULT_MAXIMUM_POOL_SIZE)
      .setKeepAliveMillSecs(DEFAULT_KEEPALIVE_TIME_MS)
      .setThreadNamePrefix("jedis-multi-node-pipeline")
      .setWorkQueue(new ArrayBlockingQueue<>(DEFAULT_BLOCKING_QUEUE_SIZE)).build();

  public MultiNodePipelineBase(CommandObjects commandObjects) {
    super(commandObjects);
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
  }

  /**
   * Provide an interface for users to set executors themselves.
   * @param executor the executor
   */
  public static void setExecutorService(ExecutorService executor) {
    if (executorService != executor && executorService != null) {
      executorService.shutdown();
    }
    executorService = executor;
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
      pipelinedResponses.putIfAbsent(nodeKey, new LinkedList<>());
      queue = pipelinedResponses.get(nodeKey);

      Connection newOne = getConnection(nodeKey);
      connections.putIfAbsent(nodeKey, newOne);
      connection = connections.get(nodeKey);
      if (connection != newOne) {
        log.debug("Duplicate connection to {}, closing it.", nodeKey);
        IOUtils.closeQuietly(newOne);
      }
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

    CountDownLatch countDownLatch = new CountDownLatch(pipelinedResponses.size());
    Iterator<Map.Entry<HostAndPort, Queue<Response<?>>>> pipelinedResponsesIterator
        = pipelinedResponses.entrySet().iterator();
    while (pipelinedResponsesIterator.hasNext()) {
      Map.Entry<HostAndPort, Queue<Response<?>>> entry = pipelinedResponsesIterator.next();
      HostAndPort nodeKey = entry.getKey();
      Queue<Response<?>> queue = entry.getValue();
      Connection connection = connections.get(nodeKey);
      try {
        executorService.submit(() -> {
          try {
            List<Object> unformatted = connection.getMany(queue.size());
            for (Object o : unformatted) {
              queue.poll().set(o);
            }
          } catch (JedisConnectionException jce) {
            log.error("Error with connection to " + nodeKey, jce);
            // cleanup the connection
            pipelinedResponsesIterator.remove();
            connections.remove(nodeKey);
            IOUtils.closeQuietly(connection);
          } finally {
            countDownLatch.countDown();
          }
        });
      } catch (RejectedExecutionException e) {
        log.error("Get a reject exception when submitting, it is recommended that you use the "
            + "MultiNodePipelineBase#setExecutorService method to customize the executor", e);
        throw e;
      }
    }

    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      log.error("Thread is interrupted during sync.", e);
    }
    syncing = false;
  }

  @Deprecated
  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }
}
