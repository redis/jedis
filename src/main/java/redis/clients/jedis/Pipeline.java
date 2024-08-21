package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.commands.DatabasePipelineCommands;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.graph.GraphCommandObjects;
import redis.clients.jedis.params.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.KeyValue;

public class Pipeline extends PipelineBase implements DatabasePipelineCommands, Closeable {

  private final Queue<Response<?>> pipelinedResponses = new LinkedList<>();
  protected final Connection connection;
  private final boolean closeConnection;
  //private final CommandObjects commandObjects;

  public Pipeline(Jedis jedis) {
    this(jedis.getConnection(), false);
  }

  public Pipeline(Connection connection) {
    this(connection, false);
  }

  public Pipeline(Connection connection, boolean closeConnection) {
    this(connection, closeConnection, createCommandObjects(connection));
  }

  private static CommandObjects createCommandObjects(Connection connection) {
    CommandObjects commandObjects = new CommandObjects();
    RedisProtocol proto = connection.getRedisProtocol();
    if (proto != null) commandObjects.setProtocol(proto);
    return commandObjects;
  }

  Pipeline(Connection connection, boolean closeConnection, CommandObjects commandObjects) {
    super(commandObjects);
    this.connection = connection;
    this.closeConnection = closeConnection;
    GraphCommandObjects graphCommandObjects = new GraphCommandObjects(this.connection);
    graphCommandObjects.setBaseCommandArgumentsCreator(protocolCommand -> commandObjects.commandArguments(protocolCommand));
    setGraphCommands(graphCommandObjects);
  }

  @Override
  public final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    Response<T> response = new Response<>(commandObject.getBuilder());
    pipelinedResponses.add(response);
    return response;
  }

  @Override
  public void close() {
    try {
      sync();
    } finally {
      if (closeConnection) {
        IOUtils.closeQuietly(connection);
      }
    }
  }

  /**
   * Synchronize pipeline by reading all responses. This operation close the pipeline. In order to
   * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
   * commands you execute.
   */
  @Override
  public void sync() {
    if (!hasPipelinedResponse()) return;
    List<Object> unformatted = connection.getMany(pipelinedResponses.size());
    for (Object rawReply : unformatted) {
      pipelinedResponses.poll().set(rawReply);
    }
  }

  /**
   * Synchronize pipeline by reading all responses. This operation close the pipeline. Whenever
   * possible try to avoid using this version and use Pipeline.sync() as it won't go through all the
   * responses and generate the right response type (usually it is a waste of time).
   * @return A list of all the responses in the order you executed them.
   */
  public List<Object> syncAndReturnAll() {
    if (hasPipelinedResponse()) {
      List<Object> unformatted = connection.getMany(pipelinedResponses.size());
      List<Object> formatted = new ArrayList<>();
      for (Object rawReply : unformatted) {
        try {
          Response<?> response = pipelinedResponses.poll();
          response.set(rawReply);
          formatted.add(response.get());
        } catch (JedisDataException e) {
          formatted.add(e);
        }
      }
      return formatted;
    } else {
      return java.util.Collections.<Object> emptyList();
    }
  }

  public final boolean hasPipelinedResponse() {
    return pipelinedResponses.size() > 0;
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

  @Override
  public Response<String> select(final int index) {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.SELECT).add(index), BuilderFactory.STRING));
  }

  @Override
  public Response<Long> dbSize() {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.DBSIZE), BuilderFactory.LONG));
  }

  @Override
  public Response<String> swapDB(final int index1, final int index2) {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.SWAPDB)
        .add(index1).add(index2), BuilderFactory.STRING));
  }

  @Override
  public Response<Long> move(String key, int dbIndex) {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.MOVE)
        .key(key).add(dbIndex), BuilderFactory.LONG));
  }

  @Override
  public Response<Long> move(final byte[] key, final int dbIndex) {
    return appendCommand(new CommandObject<>(commandObjects.commandArguments(Protocol.Command.MOVE)
        .key(key).add(dbIndex), BuilderFactory.LONG));
  }

  @Override
  public Response<Boolean> copy(String srcKey, String dstKey, int db, boolean replace) {
    return appendCommand(commandObjects.copy(srcKey, dstKey, db, replace));
  }

  @Override
  public Response<Boolean> copy(byte[] srcKey, byte[] dstKey, int db, boolean replace) {
    return appendCommand(commandObjects.copy(srcKey, dstKey, db, replace));
  }

  @Override
  public Response<String> migrate(String host, int port, byte[] key, int destinationDB, int timeout) {
    return appendCommand(commandObjects.migrate(host, port, key, destinationDB, timeout));
  }

  @Override
  public Response<String> migrate(String host, int port, String key, int destinationDB, int timeout) {
    return appendCommand(commandObjects.migrate(host, port, key, destinationDB, timeout));
  }

  @Override
  public Response<String> migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, byte[]... keys) {
    return appendCommand(commandObjects.migrate(host, port, destinationDB, timeout, params, keys));
  }

  @Override
  public Response<String> migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, String... keys) {
    return appendCommand(commandObjects.migrate(host, port, destinationDB, timeout, params, keys));
  }
}
