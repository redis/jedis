package redis.clients.jedis;

import java.util.List;
import redis.clients.jedis.exceptions.JedisException;

/**
 * A transaction where each command will be immediately sent to Redis server and checked the 'QUEUED' reply.
 */
public class ReliableTransaction extends TransactionBase {

  private static final String QUEUED_STR = "QUEUED";

  public ReliableTransaction(Connection connection) {
    super(connection);
  }

  /**
   * Creates a transaction.
   *
   * If user wants to WATCH/UNWATCH keys and then call MULTI ({@link #multi()}) ownself, it should
   * be {@code doMulti=false}.
   *
   * @param connection connection
   * @param doMulti {@code false} for manual WATCH, UNWATCH and MULTI
   */
  public ReliableTransaction(Connection connection, boolean doMulti) {
    super(connection, doMulti);
  }

  @Override
  protected final void processMultiResponse() {
    String status = connection.getStatusCodeReply();
    if (!"OK".equals(status)) {
      throw new JedisException("MULTI command failed. Received response: " + status);
    }
  }

  @Override
  protected final void processAppendStatus() {
    String status = connection.getStatusCodeReply();
    if (!QUEUED_STR.equals(status)) {
      throw new JedisException(status);
    }
  }

  @Override
  protected final void processPipelinedResponses() {
    // do nothing
  }

  @Override
  public final List<Object> exec() {
    return super.exec();
  }

  @Override
  public final String discard() {
    String status = super.discard();
    if (!"OK".equals(status)) {
      throw new JedisException("DISCARD command failed. Received response: " + status);
    }
    return status;
  }
}
