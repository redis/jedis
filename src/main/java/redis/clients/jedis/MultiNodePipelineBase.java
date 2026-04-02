package redis.clients.jedis;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
   */
  public static volatile int MULTI_NODE_PIPELINE_SYNC_WORKERS = 3;

  private final Map<HostAndPort, Queue<Response<?>>> pipelinedResponses;
  private final Map<HostAndPort, Connection> connections;
  private volatile boolean syncing = false;

  /**
   * External executor service to use for {@code sync()}. If not set, a new executor service will be
   * created for each {@code sync()} call.
   */
  private final ExecutorService sharedExecutorService;

  public MultiNodePipelineBase(CommandObjects commandObjects) {
    this(commandObjects, null);
  }

  MultiNodePipelineBase(CommandObjects commandObjects, ExecutorService executorService) {
        super(commandObjects);
        pipelinedResponses = new LinkedHashMap<>();
        connections = new LinkedHashMap<>();
        this.sharedExecutorService = executorService;
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

    boolean multiNode = pipelinedResponses.size() > 1;
    Executor executor;
    ExecutorService executorService = null;
    if (multiNode) {
      executorService = getPipelineExecutor();
      executor = executorService;
    } else {
      executor = Runnable::run;
    }
    CountDownLatch countDownLatch = multiNode
        ? new CountDownLatch(pipelinedResponses.size())
        : null;

    Iterator<Map.Entry<HostAndPort, Queue<Response<?>>>> pipelinedResponsesIterator = pipelinedResponses.entrySet()
        .iterator();
    while (pipelinedResponsesIterator.hasNext()) {
      Map.Entry<HostAndPort, Queue<Response<?>>> entry = pipelinedResponsesIterator.next();
      HostAndPort nodeKey = entry.getKey();
      Queue<Response<?>> queue = entry.getValue();
      Connection connection = connections.get(nodeKey);
      executor.execute(() -> {
        try {
          List<Object> unformatted = connection.getMany(queue.size());
          for (Object o : unformatted) {
            queue.poll().set(o);
          }
        } catch (JedisConnectionException jce) {
          log.error("Error with connection to " + nodeKey, jce);
          // cleanup the connection
          // TODO these operations not thread-safe and when executed here, the iter may moved
          pipelinedResponsesIterator.remove();
          connections.remove(nodeKey);
          IOUtils.closeQuietly(connection);
        } finally {
          if (multiNode) {
            countDownLatch.countDown();
          }
        }
      });
    }

    if (multiNode) {
      try {
        countDownLatch.await();
      } catch (InterruptedException e) {
        log.error("Thread is interrupted during sync.", e);
      }

      releasePipelineExecutor(executorService);
    }

    syncing = false;
  }

  /**
   * Acquires the executor service to run multi-node pipeline commands.
   * <p>
   * If a shared executor is provided by the user, it is returned.
   * Otherwise, a new dedicated executor is created for this pipeline.
   * </p>
   */
  private ExecutorService getPipelineExecutor() {
    return isUsingSharedExecutor()
            ? this.sharedExecutorService
            : createDedicatedPipelineExecutor();
  }

  /**
   * Releases the executor service used by the pipeline.
   * <p>
   * Dedicated executors are shut down after use.
   * Shared executors are managed externally and not shut down.
   * </p>
   */
  private void releasePipelineExecutor(ExecutorService executorService) {
    if (!isUsingSharedExecutor()) {
      executorService.shutdownNow();
    }
  }

  /**
   * Returns true if this pipeline is using a shared executor service
   * provided externally.
   */
  private boolean isUsingSharedExecutor() {
    return this.sharedExecutorService != null;
  }

  /**
   * Creates a new dedicated executor for multi-node pipeline execution.
   */
  private ExecutorService createDedicatedPipelineExecutor() {
    return Executors.newFixedThreadPool(MULTI_NODE_PIPELINE_SYNC_WORKERS);
  }

  @Deprecated
  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }
}
