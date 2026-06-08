package redis.clients.jedis.mcf;

import static redis.clients.jedis.Protocol.Command.EXEC;
import static redis.clients.jedis.Protocol.Command.MULTI;
import static redis.clients.jedis.Protocol.Command.UNWATCH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.*;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Transaction implementation backed by {@link MultiDbConnectionSupplier}.
 * <p>
 * Connection lifecycle:
 * <ul>
 * <li>When the initial operation is {@code MULTI} ({@code doMulti=true}), connection acquisition is
 * deferred until {@link #exec()} (or {@link #discard()} / {@link #close()}).</li>
 * <li>When the initial operation is not {@code MULTI} ({@code doMulti=false}), a connection is
 * obtained on the first command that needs to be sent to the server and held until the first of
 * {@link #exec()}, {@link #discard()} or {@link #close()} is called.</li>
 * </ul>
 * Command execution:
 * <ul>
 * <li>Commands issued before {@code MULTI} are executed immediately on the held connection and
 * their responses are returned to the caller.</li>
 * <li>Commands issued after {@code MULTI} (including {@code UNWATCH}) are buffered in memory and
 * dispatched to the server in a single batch when {@link #exec()} is called.</li>
 * </ul>
 */
@Experimental
public class MultiDbTransaction extends AbstractTransaction {
  private static final byte[] QUEUED_IN_BYTES = SafeEncoder.encode("QUEUED");
  private static final byte[] OK_IN_BYTES = SafeEncoder.encode("OK");
  private static final String OK_STR = "OK";

  private static final Builder<?> NO_OP_BUILDER = BuilderFactory.RAW_OBJECT;

  private final MultiDbConnectionSupplier failoverProvider;
  private final Queue<KeyValue<CommandArguments, Response<?>>> commands = new LinkedList<>();

  private Connection connection;
  private boolean inWatch = false;
  private boolean inMulti = false;

  /**
   * A user wanting to WATCH/UNWATCH keys followed by a call to MULTI ({@link #multi()}) it should
   * be {@code doMulti=false}.
   * @param provider
   * @param doMulti {@code false} should be set to enable manual WATCH, UNWATCH and MULTI
   * @param commandObjects command objects
   */
  public MultiDbTransaction(MultiDbConnectionProvider provider, boolean doMulti,
      CommandObjects commandObjects) {
    super(commandObjects);
    this.failoverProvider = new MultiDbConnectionSupplier(provider);

    if (doMulti) {
      multi();
    }
  }

  @Override
  public final void multi() {
    inMulti = true;
  }

  @Override
  public final String watch(String... keys) {
    if (inMulti) {
      throw new IllegalStateException("WATCH inside MULTI is not allowed");
    }
    String status = appendCommand(commandObjects.watch(keys)).get();
    inWatch = true;
    return status;
  }

  @Override
  public final String watch(byte[]... keys) {
    if (inMulti) {
      throw new IllegalStateException("WATCH inside MULTI is not allowed");
    }
    String status = appendCommand(commandObjects.watch(keys)).get();
    inWatch = true;
    return status;
  }

  @Override
  public final String unwatch() {
    Response<String> response = appendCommand(
      new CommandObject<>(new CommandArguments(UNWATCH), BuilderFactory.STRING));
    inWatch = false;
    // when inside MULTI, the command has only been buffered; its reply will be delivered by exec()
    return inMulti ? null : response.get();
  }

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    if (inMulti) {
      CommandArguments args = commandObject.getArguments();
      Response<T> response = new Response<>(commandObject.getBuilder());
      commands.add(KeyValue.of(args, response));
      return response;
    }
    try {
      return Response.of(acquireConnection().executeCommand(commandObject));
    } catch (JedisDataException e) {
      return Response.error(e);
    }
  }

  @Override
  public void close() {
    try {
      if (inMulti) {
        discard();
      } else if (inWatch) {
        unwatch();
      }
    } finally {
      releaseConnection();
    }
  }

  @Override
  public final List<Object> exec() {
    if (!inMulti) {
      throw new IllegalStateException("EXEC without MULTI");
    }

    try {
      Connection conn = acquireConnection();

      Object multiReply = conn
          .executeCommand(new CommandObject<>(new CommandArguments(MULTI), NO_OP_BUILDER));
      if (!bytesEquals(OK_IN_BYTES, multiReply)) {
        throw new JedisDataException("Unexpected response: " + multiReply);
      }

      commands.forEach((command) -> conn.sendCommand(command.getKey()));
      // following connection.getMany(int) flushes anyway, so no flush here.

      // server replies QUEUED for each buffered command
      List<Object> queuedCmdResponses = conn.getMany(commands.size());
      queuedCmdResponses.forEach((rawReply) -> {
        if (!bytesEquals(QUEUED_IN_BYTES, rawReply)) {
          throw new JedisDataException("Unexpected response: " + rawReply);
        }
      });

      conn.sendCommand(EXEC);

      List<Object> unformatted = conn.getObjectMultiBulkReply();
      if (unformatted == null) {
        return null;
      }

      List<Object> formatted = new ArrayList<>(unformatted.size());
      for (Object rawReply : unformatted) {
        try {
          Response<?> response = commands.poll().getValue();
          response.set(rawReply);
          formatted.add(response.get());
        } catch (JedisDataException e) {
          formatted.add(e);
        }
      }
      return formatted;

    } finally {
      inMulti = false;
      inWatch = false;
      commands.clear();
      releaseConnection();
    }
  }

  @Override
  public final String discard() {
    if (!inMulti) {
      throw new IllegalStateException("DISCARD without MULTI");
    }

    try {
      // MULTI was never sent to the server (acquisition is deferred until exec()).
      // Buffered commands only exist locally, so there is nothing to roll back server-side
      // unless we have already acquired a connection for pre-MULTI traffic (e.g. WATCH).
      if (inWatch) {
        acquireConnection().sendCommand(UNWATCH);
        return connection.getStatusCodeReply();
      }
      return OK_STR.toString();
    } finally {
      inMulti = false;
      inWatch = false;
      commands.clear();
      releaseConnection();
    }
  }

  private static boolean bytesEquals(byte[] actualBytes, Object obj) {
    if (!(obj instanceof byte[])) {
      return false;
    }
    return Arrays.equals(actualBytes, (byte[]) obj);
  }

  private Connection acquireConnection() {
    if (connection == null) {
      connection = failoverProvider.getConnection();
    }
    return connection;
  }

  private void releaseConnection() {
    if (connection != null) {
      IOUtils.closeQuietly(connection);
      connection = null;
    }
  }
}
