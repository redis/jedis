package redis.clients.jedis;

import java.util.List;

/**
 * A pipeline based transaction.
 */
public class Transaction extends TransactionBase {

  private final Jedis jedis;

  // Legacy - to support Jedis.multi()
  // TODO: Should be package private ??
  public Transaction(Jedis jedis) {
    super(jedis.getConnection());
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
    super(connection);
    this.jedis = null;
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
    super(connection, doMulti);
    this.jedis = null;
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
    super(connection, doMulti, closeConnection);
    this.jedis = null;
  }

  @Override
  protected final void processMultiResponse() {
    // do nothing
  }

  @Override
  protected final void processAppendStatus() {
    // do nothing
  }

  @Override
  protected final void processPipelinedResponses(int pipelineLength) {
    // ignore QUEUED or ERROR
    connection.getMany(1 + pipelineLength);
  }

  @Override
  public final List<Object> exec() {
    List<Object> ret;
    try {
      ret = super.exec();
    } finally {
      if (jedis != null) {
        jedis.resetState();
      }
    }
    return ret;
  }

  @Override
  public final String discard() {
    String ret;
    try {
      ret = super.discard();
    } finally {
      if (jedis != null) {
        jedis.resetState();
      }
    }
    return ret;
  }
}
