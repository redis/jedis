package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.commands.DatabasePipelineCommands;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.KeyValue;

public class Pipeline extends AbstractPipeline implements DatabasePipelineCommands, Closeable {

  /**
   * An entry of the wire-order reply queue, marking whether it belongs to a client-internal
   * command (e.g. HIMPORT's injected {@code PREPARE}): its reply is consumed in wire order like
   * any other, but it is excluded from user-facing results such as {@link #syncAndReturnAll()}.
   */
  private static final class QueuedResponse<T> extends Response<T> {

    private final boolean internal;

    private QueuedResponse(Builder<T> builder, boolean internal) {
      super(builder);
      this.internal = internal;
    }

    static <T> QueuedResponse<T> user(Builder<T> builder) {
      return new QueuedResponse<>(builder, false);
    }

    static <T> QueuedResponse<T> internal(Builder<T> builder) {
      return new QueuedResponse<>(builder, true);
    }

    boolean isInternal() {
      return internal;
    }
  }

  private final Queue<QueuedResponse<?>> pipelinedResponses = new LinkedList<>();
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
    return new CommandObjects(RedisProtocol.orServerDefault(connection.getRedisProtocol()));
  }

  Pipeline(Connection connection, boolean closeConnection, CommandObjects commandObjects) {
    super(commandObjects);
    this.connection = connection;
    this.closeConnection = closeConnection;
  }

  @Override
  public final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    QueuedResponse<T> response = QueuedResponse.user(commandObject.getBuilder());
    pipelinedResponses.add(response);
    return response;
  }

  /**
   * Buffers a client-internal command: its reply is consumed in wire order like any other, but it
   * is excluded from user-facing results ({@link #syncAndReturnAll()}).
   */
  private <T> void appendInternalCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    pipelinedResponses.add(QueuedResponse.internal(commandObject.getBuilder()));
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
        QueuedResponse<?> response = pipelinedResponses.poll();
        response.set(rawReply);
        if (response.isInternal()) {
          continue; // client-internal command; not part of the user's results
        }
        try {
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

  @Override
  public Response<String> himportSet(String key, HashImport fieldset, String... values) {
    HashImportSupport.checkArgs(fieldset, values.length);
    himportPrepareBeforeUse(fieldset);
    return appendCommand(commandObjects.himportSetBare(key, fieldset, values));
  }

  @Override
  public Response<String> himportSet(byte[] key, HashImport fieldset, byte[]... values) {
    HashImportSupport.checkArgs(fieldset, values.length);
    himportPrepareBeforeUse(fieldset);
    return appendCommand(commandObjects.himportSetBare(key, fieldset, values));
  }

  /**
   * Buffers an internal {@code HIMPORT PREPARE} ahead of the {@code SET} when this pipeline's
   * connection has not yet prepared the fieldset, recording it in the connection's note so
   * before-command reconciliation discards it after {@link HashImport#close()}. The pipeline owns
   * its connection, so it uses the bare {@code SET} and injects the PREPARE itself; the hook
   * variant exists for the executor path, where the connection is unknown until execution.
   */
  private void himportPrepareBeforeUse(HashImport fieldset) {
    if (!connection.himportState().isPrepared(fieldset.name())) {
      appendInternalCommand(commandObjects.himportPrepare(fieldset.name(), fieldset.fields()));
      HashImportSupport.markPrepared(connection, fieldset);
    }
  }
}
