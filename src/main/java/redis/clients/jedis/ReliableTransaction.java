package redis.clients.jedis;

import java.util.List;
import redis.clients.jedis.exceptions.JedisException;

public class ReliableTransaction extends TransactionBase {

  public ReliableTransaction(Connection connection) {
    super(connection);
  }

  /**
   * If you want to WATCH/UNWATCH keys before MULTI command you should do {@code doMulti = true}.
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
    if (!"QUEUED".equals(status)) {
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
