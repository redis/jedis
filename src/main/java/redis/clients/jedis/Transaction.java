package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.exceptions.AbortedTransactionException;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Transaction is nearly identical to Pipeline, only differences are the multi/discard behaviors
 */
public class Transaction extends MultiKeyPipelineBase implements Closeable {

  protected boolean inTransaction = true;

  protected Transaction() {
    // client will be set later in transaction block
  }

  public Transaction(final Client client) {
    this.client = client;
  }

  @Override
  protected Client getClient(String key) {
    return client;
  }

  @Override
  protected Client getClient(byte[] key) {
    return client;
  }

  public void clear() {
    if (inTransaction) {
      discard();
    }
  }

  public List<Object> exec() {
    List<Object> queuedCommands = client.getMany(getPipelinedResponseLength());
    discardTransactionOnQueuedCommandErrors(queuedCommands);

    client.exec();
    inTransaction = false;

    List<Object> unformatted = client.getObjectMultiBulkReply();
    if (unformatted == null) {
      return null;
    }
    List<Object> formatted = new ArrayList<>();
    for (Object o : unformatted) {
      try {
        formatted.add(generateResponse(o).get());
      } catch (JedisDataException e) {
        formatted.add(e);
      }
    }

    return formatted;
  }

  /**
   * @deprecated This method will be removed in next major release.
   */
  @Deprecated
  public List<Response<?>> execGetResponse() {
    List<Object> queuedCommands = client.getMany(getPipelinedResponseLength());
    discardTransactionOnQueuedCommandErrors(queuedCommands);
    client.exec();
    inTransaction = false;

    List<Object> unformatted = client.getObjectMultiBulkReply();
    if (unformatted == null) {
      return null;
    }
    List<Response<?>> response = new ArrayList<>();
    for (Object o : unformatted) {
      response.add(generateResponse(o));
    }
    return response;
  }

  public String discard() {
    client.getMany(getPipelinedResponseLength());
    client.discard();
    inTransaction = false;
    clean();
    return client.getStatusCodeReply();
  }

  public void setClient(Client client) {
    this.client = client;
  }

  @Override
  public void close() {
    clear();
  }

  private static final String WATCH_INSIDE_MULTI_MESSAGE = "WATCH inside MULTI is not allowed";

  /**
   * @param keys
   * @return
   * @throws UnsupportedOperationException
   * @deprecated {@value #WATCH_INSIDE_MULTI_MESSAGE}
   */
  @Override
  @Deprecated
  public Response<String> watch(String... keys) throws UnsupportedOperationException {
    throw new UnsupportedOperationException(WATCH_INSIDE_MULTI_MESSAGE);
  }

  /**
   * @param keys
   * @return
   * @throws UnsupportedOperationException
   * @deprecated {@value #WATCH_INSIDE_MULTI_MESSAGE}
   */
  @Override
  @Deprecated
  public Response<String> watch(byte[]... keys) throws UnsupportedOperationException {
    throw new UnsupportedOperationException(WATCH_INSIDE_MULTI_MESSAGE);
  }

  private void discardTransactionOnQueuedCommandErrors(List<Object> queuedCommands) {
    queuedCommands
        .stream()
        .filter(JedisDataException.class::isInstance)
        .map(JedisDataException.class::cast)
        .findFirst()
        .ifPresent(e -> {
          client.discard();
          inTransaction = false;
          clean();
          client.getStatusCodeReply();
          throw new AbortedTransactionException("Transaction aborted. At least one command failed to be queued. Cause " + e.getMessage(), e);
        });
  }
}
