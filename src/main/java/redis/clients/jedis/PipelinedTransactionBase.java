package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.exceptions.JedisDataException;

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

  public Response<String> unwatch() {
    return appendCommand(new CommandObject<>(new CommandArguments(Protocol.Command.UNWATCH), BuilderFactory.STRING));
  }

//  public final void exec() {
//    // ignore QUEUED or ERROR
//    connection.getMany(1 + getPipelinedResponseLength());
//    connection.sendCommand(Protocol.Command.EXEC);
//    inTransaction = false;
//
//    List<Object> unformatted = connection.getObjectMultiBulkReply();
//    unformatted.stream().forEachOrdered(u -> generateResponse(u));
//  }
  public List<Object> exec() {
    if (!inTransaction) throw new IllegalStateException("EXEC without MULTI");
    // ignore QUEUED or ERROR
    connection.getMany(1 + getPipelinedResponseLength());
    connection.sendCommand(Protocol.Command.EXEC);
    inTransaction = false;

    List<Object> unformatted = connection.getObjectMultiBulkReply();
    if (unformatted == null) return null;
    List<Object> formatted = new ArrayList<>(unformatted.size());
    for (Object o : unformatted) {
      try {
        formatted.add(generateResponse(o).get());
      } catch (JedisDataException e) {
        formatted.add(e);
      }
    }
    return formatted;
  }

//  public final void discard() {
//    // ignore QUEUED or ERROR
//    connection.getMany(1 + getPipelinedResponseLength());
//    connection.sendCommand(Protocol.Command.DISCARD);
//    connection.getStatusCodeReply(); // OK
//    inTransaction = false;
//    clean();
//  }
  public String discard() {
    if (!inTransaction) throw new IllegalStateException("DISCARD without MULTI");
    // ignore QUEUED or ERROR
    connection.getMany(1 + getPipelinedResponseLength());
    connection.sendCommand(Protocol.Command.DISCARD);
    String status = connection.getStatusCodeReply(); // OK
    inTransaction = false;
    clean();
    return status;
  }
}
