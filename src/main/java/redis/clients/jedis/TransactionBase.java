package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.DISCARD;
import static redis.clients.jedis.Protocol.Command.EXEC;
import static redis.clients.jedis.Protocol.Command.MULTI;
import static redis.clients.jedis.Protocol.Command.UNWATCH;
import static redis.clients.jedis.Protocol.Command.WATCH;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.graph.GraphCommandObjects;

public abstract class TransactionBase extends TransactionCoreBase {

  private final Queue<Response<?>> pipelinedResponses = new LinkedList<>();
  protected final Connection connection;
  private final boolean closeConnection;

  private boolean broken = false;
  private boolean inWatch = false;
  private boolean inMulti = false;

  /**
   * Creates a new transaction.
   *
   * A MULTI command will be added to be sent to server. WATCH/UNWATCH/MULTI commands must not be
   * called with this object.
   * @param connection connection
   */
  public TransactionBase(Connection connection) {
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
  public TransactionBase(Connection connection, boolean doMulti) {
    this(connection, doMulti, false);
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
  public TransactionBase(Connection connection, boolean doMulti, boolean closeConnection) {
    this.connection = connection;
    this.closeConnection = closeConnection;
    setGraphCommands(new GraphCommandObjects(this.connection));
    if (doMulti) multi();
  }

  @Override
  public final void multi() {
    connection.sendCommand(MULTI);
    processMultiResponse();
    inMulti = true;
  }

  @Override
  public String watch(final String... keys) {
    connection.sendCommand(WATCH, keys);
    String status = connection.getStatusCodeReply();
    inWatch = true;
    return status;
  }

  @Override
  public String watch(final byte[]... keys) {
    connection.sendCommand(WATCH, keys);
    String status = connection.getStatusCodeReply();
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

  protected abstract void processMultiResponse();

  protected abstract void processAppendStatus();

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    processAppendStatus();
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

  @Override
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

  protected abstract void processPipelinedResponses(int pipelineLength);

  @Override
  public List<Object> exec() {
    if (!inMulti) {
      throw new IllegalStateException("EXEC without MULTI");
    }

    try {
      processPipelinedResponses(pipelinedResponses.size());
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
    }
  }

  @Override
  public String discard() {
    if (!inMulti) {
      throw new IllegalStateException("DISCARD without MULTI");
    }

    try {
      processPipelinedResponses(pipelinedResponses.size());
      connection.sendCommand(DISCARD);
      return connection.getStatusCodeReply();
    } catch (JedisConnectionException jce) {
      broken = true;
      throw jce;
    } finally {
      inMulti = false;
      inWatch = false;
      pipelinedResponses.clear();
    }
  }
}
