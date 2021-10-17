package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;

/**
 * Transaction is nearly identical to Pipeline, only differences are the multi/discard behaviors
 */
public class PipelinedTransactionBase extends Queable implements Closeable {

  private boolean inTransaction = true;

  protected final Connection connection;

  public PipelinedTransactionBase(Connection connection) {
    this.connection = connection;
    this.connection.sendCommand(Protocol.Command.MULTI);
  }

  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    connection.sendCommand(commandObject.getArguments());
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
    // ignore QUEUED or ERROR
    connection.getMany(1 + getPipelinedResponseLength());
    connection.sendCommand(Protocol.Command.EXEC);
    inTransaction = false;

    List<Object> unformatted = connection.getObjectMultiBulkReply();
    unformatted.stream().forEachOrdered(u -> generateResponse(u));
  }

  public void discard() {
    // ignore QUEUED or ERROR
    connection.getMany(1 + getPipelinedResponseLength());
    connection.sendCommand(Protocol.Command.DISCARD);
    connection.getStatusCodeReply(); // OK
    inTransaction = false;
    clean();
  }
}
