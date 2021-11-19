package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;
import redis.clients.jedis.exceptions.JedisException;

public class ReliableTransactionBase extends Queable implements Closeable {

  private boolean inTransaction = true;

  protected final Connection connection;

  public ReliableTransactionBase(Connection connection) {
    this.connection = connection;
    executeMulti();
  }

  private void executeMulti() {
    connection.sendCommand(Protocol.Command.MULTI);
    String status = connection.getStatusCodeReply();
    if (!"OK".equals(status)) {
      throw new JedisException("MULTI command failed. Received response: " + status);
    }
  }

  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
    String status = connection.getStatusCodeReply();
    if (!"QUEUED".equals(status)) {
      throw new JedisException(status);
    }
    return enqueResponse(commandObject.getBuilder());
  }

  @Override
  public void close() {
    clear();
  }

  public void clear() {
    if (inTransaction) {
      discard();
    }
  }

  public final void exec() {
    connection.sendCommand(Protocol.Command.EXEC);
    inTransaction = false;

    List<Object> unformatted = connection.getObjectMultiBulkReply();
    unformatted.stream().forEachOrdered(u -> generateResponse(u));
  }

  public void discard() {
    connection.sendCommand(Protocol.Command.DISCARD);
    String status = connection.getStatusCodeReply();
    inTransaction = false;
    clean();
    if (!"OK".equals(status)) {
      throw new JedisException("DISCARD command failed. Received response: " + status);
    }
  }
}
