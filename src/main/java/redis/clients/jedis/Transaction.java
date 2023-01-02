package redis.clients.jedis;

import java.util.List;

/**
 * A pipeline based transaction.
 */
public class Transaction extends TransactionBase {

  private final Jedis jedis;

  public Transaction(Jedis jedis) {
    super(jedis.getConnection());
    this.jedis = jedis;
  }

  public Transaction(Connection connection) {
    super(connection);
    this.jedis = null;
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
  public Transaction(Connection connection, boolean doMulti) {
    super(connection, doMulti);
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
  protected final void processPipelinedResponses() {
    // ignore QUEUED or ERROR
    connection.getMany(1 + getPipelinedResponseLength());
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
