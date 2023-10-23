package redis.clients.jedis;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import redis.clients.jedis.activeactive.CircuitBreakerFailoverConnectionProvider;
import redis.clients.jedis.commands.PipelineBinaryCommands;
import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.commands.RedisModulePipelineCommands;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.util.KeyValue;

/**
 * This is high memory dependent solution as all the appending commands will be hold in memory until
 * {@link MultiClusterFailoverPipeline#sync() SYNC}
 * (or {@link MultiClusterFailoverPipeline#close() CLOSE}) gets called.
 */
public class MultiClusterFailoverPipeline extends PipelineBase
    implements PipelineCommands, PipelineBinaryCommands, RedisModulePipelineCommands, Closeable {

  private final CircuitBreakerFailoverConnectionProvider provider;
  private final Queue<KeyValue<CommandArguments, Response<?>>> commands = new LinkedList<>();

  public MultiClusterFailoverPipeline(MultiClusterPooledConnectionProvider provider) {
    super(new CommandObjects());
    try (Connection connection = provider.getConnection()) { // we don't need a healthy connection now
      RedisProtocol proto = connection.getRedisProtocol();
      if (proto != null) this.commandObjects.setProtocol(proto);
    }

    this.provider = new CircuitBreakerFailoverConnectionProvider(provider);
  }

  @Override
  public final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    CommandArguments args = commandObject.getArguments();
    Response<T> response = new Response<>(commandObject.getBuilder());
    commands.add(KeyValue.of(args, response));
    return response;
  }

  @Override
  public void close() {
    sync();
  }

  /**
   * Synchronize pipeline by reading all responses. This operation close the pipeline. In order to
   * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
   * commands you execute.
   */
  @Override
  public void sync() {
    if (!hasPipelinedResponse()) return;

    try (Connection connection = provider.getConnection()) {
      for (KeyValue<CommandArguments, Response<?>> command : commands) {
        connection.sendCommand(command.getKey());
      }
      // connection.flush(); // following flushes anyway
      
      List<Object> unformatted = connection.getMany(commands.size());
      for (Object o : unformatted) {
        commands.poll().getValue().set(o);
      }
    }
  }

  public final boolean hasPipelinedResponse() {
    return commands.size() > 0;
  }

  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }

  public Response<KeyValue<Long, Long>> waitAOF(long numLocal, long numReplicas, long timeout) {
    return appendCommand(commandObjects.waitAOF(numLocal, numReplicas, timeout));
  }

  public Response<List<String>> time() {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.TIME), BuilderFactory.STRING_LIST));
  }

  // RedisGraph commands
  @Override
  public Response<ResultSet> graphQuery(String name, String query) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphReadonlyQuery(String name, String query) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphQuery(String name, String query, long timeout) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphReadonlyQuery(String name, String query, long timeout) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphQuery(String name, String query, Map<String, Object> params, long timeout) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<ResultSet> graphReadonlyQuery(String name, String query, Map<String, Object> params, long timeout) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<String> graphDelete(String name) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }

  @Override
  public Response<List<String>> graphProfile(String graphName, String query) {
    throw new UnsupportedOperationException("Graph commands are not supported.");
  }
  // RedisGraph commands
}
