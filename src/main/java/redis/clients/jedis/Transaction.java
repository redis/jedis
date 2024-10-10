package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.DISCARD;
import static redis.clients.jedis.Protocol.Command.EXEC;
import static redis.clients.jedis.Protocol.Command.MULTI;
import static redis.clients.jedis.Protocol.Command.UNWATCH;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.graph.GraphCommandObjects;

/**
 * A transaction based on <a href="https://redis.io/docs/manual/pipelining/">pipelining</a>.
 */
public class Transaction extends TransactionBase {

  private final Queue<Response<?>> pipelinedResponses = new LinkedList<>();

  private Jedis jedis = null;

  protected final Connection connection;
  private final boolean closeConnection;

  private boolean broken = false;
  private boolean inWatch = false;
  private boolean inMulti = false;

  // Legacy - to support Jedis.multi()
  // TODO: Should be package private ??
  public Transaction(Jedis jedis) {
    this(jedis.getConnection());
    this.jedis = jedis;
  }

  /**
   * Creates a new transaction.
   * 
   * A MULTI command will be added to be sent to server. WATCH/UNWATCH/MULTI commands must not be
   * called with this object.
   * @param connection connection
   */
  public Transaction(Connection connection) {
    this(connection, true);
  }

  /**
   * Creates a new transaction.
   *
   * A user wanting to WATCH/UNWATCH keys followed by a call to MULTI ({@link #multi()}) it should
   * be {@code doMulti=false}.
   *
   * @param connection connection
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   */
  public Transaction(Connection connection, boolean doMulti) {
    this(connection, doMulti, false, createCommandObjects(connection));
  }

  /**
   * Creates a new transaction.
   *
   * A user wanting to WATCH/UNWATCH keys followed by a call to MULTI ({@link #multi()}) it should
   * be {@code doMulti=false}.
   *
   * @param connection connection
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   * @param closeConnection should the 'connection' be closed when 'close()' is called?
   */
  public Transaction(Connection connection, boolean doMulti, boolean closeConnection) {
    this(connection, doMulti, closeConnection, createCommandObjects(connection));
  }

  /**
   * Creates a new transaction.
   *
   * A user wanting to WATCH/UNWATCH keys followed by a call to MULTI ({@link #multi()}) it should
   * be {@code doMulti=false}.
   *
   * @param connection connection
   * @param commandObjects command objects
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   * @param closeConnection should the 'connection' be closed when 'close()' is called?
   */
  Transaction(Connection connection, boolean doMulti, boolean closeConnection, CommandObjects commandObjects) {
    super(commandObjects);
    this.connection = connection;
    this.closeConnection = closeConnection;
    GraphCommandObjects graphCommandObjects = new GraphCommandObjects(this.connection);
    graphCommandObjects.setBaseCommandArgumentsCreator(protocolCommand -> commandObjects.commandArguments(protocolCommand));
    setGraphCommands(graphCommandObjects);
    if (doMulti) multi();
  }

  private static CommandObjects createCommandObjects(Connection connection) {
    CommandObjects commandObjects = new CommandObjects();
    RedisProtocol proto = connection.getRedisProtocol();
    if (proto != null) commandObjects.setProtocol(proto);
    return commandObjects;
  }

  @Override
  public final void multi() {
    connection.sendCommand(MULTI);
    // processMultiResponse(); // do nothing
    inMulti = true;
  }

  @Override
  public String watch(final String... keys) {
    String status = connection.executeCommand(commandObjects.watch(keys));
    inWatch = true;
    return status;
  }

  @Override
  public String watch(final byte[]... keys) {
    String status = connection.executeCommand(commandObjects.watch(keys));
    inWatch = true;
    return status;
  }

  @Override
  public String unwatch() {
    connection.sendCommand(UNWATCH);
    String status = connection.getStatusCodeReply();
    inWatch = false;
    return status;
  }

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    // processAppendStatus(); // do nothing
    Response<T> response = new Response<>(commandObject.getBuilder());
    pipelinedResponses.add(response);
    return response;
  }

  @Override
  public final void close() {
    try {
      clear();
    } finally {
      if (closeConnection) {
        connection.close();
      }
    }
  }

  @Deprecated // TODO: private
  public final void clear() {
    if (broken) {
      return;
    }
    if (inMulti) {
      discard();
    } else if (inWatch) {
      unwatch();
    }
  }

  @Override
  public List<Object> exec() {
    if (!inMulti) {
      throw new IllegalStateException("EXEC without MULTI");
    }

    try {
      // ignore QUEUED (or ERROR)
      // processPipelinedResponses(pipelinedResponses.size());
      connection.getMany(1 + pipelinedResponses.size());

      connection.sendCommand(EXEC);

      List<Object> unformatted = connection.getObjectMultiBulkReply();
      if (unformatted == null) {
        pipelinedResponses.clear();
        return null;
      }

      List<Object> formatted = new ArrayList<>(unformatted.size());
      for (Object o : unformatted) {
        try {
          Response<?> response = pipelinedResponses.poll();
          response.set(o);
          formatted.add(response.get());
        } catch (JedisDataException e) {
          formatted.add(e);
        }
      }
      return formatted;
    } catch (JedisConnectionException jce) {
      broken = true;
      throw jce;
    } finally {
      inMulti = false;
      inWatch = false;
      pipelinedResponses.clear();
      if (jedis != null) {
        jedis.resetState();
      }
    }
  }

  @Override
  public String discard() {
    if (!inMulti) {
      throw new IllegalStateException("DISCARD without MULTI");
    }

    try {
      // ignore QUEUED (or ERROR)
      // processPipelinedResponses(pipelinedResponses.size());
      connection.getMany(1 + pipelinedResponses.size());

      connection.sendCommand(DISCARD);

      return connection.getStatusCodeReply();
    } catch (JedisConnectionException jce) {
      broken = true;
      throw jce;
    } finally {
      inMulti = false;
      inWatch = false;
      pipelinedResponses.clear();
      if (jedis != null) {
        jedis.resetState();
      }
    }
  }
}
