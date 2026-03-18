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
  protected final CommandFlagsRegistry commandFlagsRegistry;

  public MultiNodePipelineBase(CommandObjects commandObjects) {
    this(commandObjects, StaticCommandFlagsRegistry.registry());
  }

  protected MultiNodePipelineBase(CommandObjects commandObjects, CommandFlagsRegistry commandFlagsRegistry) {
    super(commandObjects);
    this.commandFlagsRegistry = commandFlagsRegistry;
    pipelinedResponses = new LinkedHashMap<>();
    connections = new LinkedHashMap<>();
  }

  protected abstract HostAndPort getNodeKey(CommandArguments args);

  protected abstract Connection getConnection(HostAndPort nodeKey);

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    // Validate that the command is supported in pipeline mode
    validatePipelineCommand(commandObject.getArguments());

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
      executorService = Executors.newFixedThreadPool(MULTI_NODE_PIPELINE_SYNC_WORKERS);
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

      executorService.shutdownNow();
    }

    syncing = false;
  }

  /**
   * Validates that a command can be executed in a multi-node pipeline.
   * Commands with ALL_SHARDS, MULTI_SHARD, ALL_NODES, or SPECIAL request policies
   * require execution on multiple nodes and cannot be properly handled in pipelines.
   *
   * @param args the command arguments
   * @throws UnsupportedOperationException if the command requires multi-node execution
   */
  private void validatePipelineCommand(CommandArguments args) {
    CommandFlagsRegistry.RequestPolicy policy =
        commandFlagsRegistry.getRequestPolicy(args);

    switch (policy) {
      case ALL_SHARDS:
        throw new UnsupportedOperationException(
            "Command '" + args.getCommand() + "' with ALL_SHARDS request policy "
                + "cannot be executed in pipeline mode. This command requires execution on all "
                + "master shards but pipelines route to a single node. "
                + "Use non-pipeline cluster client for this command.");

      case MULTI_SHARD:
        throw new UnsupportedOperationException(
            "Command '" + args.getCommand() + "' with MULTI_SHARD request policy "
                + "cannot be executed in pipeline mode. This command requires execution on "
                + "multiple shards but pipelines route to a single node. "
                + "Use non-pipeline cluster client for this command.");

      case ALL_NODES:
        throw new UnsupportedOperationException(
            "Command '" + args.getCommand() + "' with ALL_NODES request policy "
                + "cannot be executed in pipeline mode. This command requires execution on all "
                + "nodes (masters and replicas) but pipelines route to a single node. "
                + "Use non-pipeline cluster client for this command.");

      case SPECIAL:
        throw new UnsupportedOperationException(
            "Command '" + args.getCommand() + "' with SPECIAL request policy "
                + "cannot be executed in pipeline mode. This command has non-trivial routing "
                + "requirements that cannot be handled in pipelines. "
                + "Use non-pipeline cluster client for this command.");

      case DEFAULT:
      default:
        // DEFAULT policy and unknown policies - allow standard command execution
        // Routes to single node based on key hash
        break;
    }
  }

  @Deprecated
  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }
}
