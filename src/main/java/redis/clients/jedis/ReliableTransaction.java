package redis.clients.jedis;

import java.util.List;
import redis.clients.jedis.exceptions.JedisException;

/**
 * ReliableTransaction is a transaction where commands are immediately sent to Redis server and the
 * 'QUEUED' reply checked.
 */
public class ReliableTransaction extends TransactionBase {

  private static final String QUEUED_STR = "QUEUED";

  /**
   * Creates a new transaction.
   * 
   * A MULTI command will be executed. WATCH/UNWATCH/MULTI commands must not be called with this object.
   */
  public ReliableTransaction(Connection connection) {
    super(connection);
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
