package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    client.exec();
    client.getAll(1); // Discard all but the last reply
    inTransaction = false;

    List<Object> unformatted = client.getObjectMultiBulkReply();
    if (unformatted == null) {
      return null;
    }
    List<Object> formatted = new ArrayList<Object>();
    for (Object o : unformatted) {
      try {
        formatted.add(generateResponse(o).get());
      } catch (JedisDataException e) {
        formatted.add(e);
      }
    }
    return formatted;
  }

  public List<Response<?>> execGetResponse() {
    client.exec();
    client.getAll(1); // Discard all but the last reply
    inTransaction = false;

    List<Object> unformatted = client.getObjectMultiBulkReply();
    if (unformatted == null) {
      return null;
    }
    List<Response<?>> response = new ArrayList<Response<?>>();
    for (Object o : unformatted) {
      response.add(generateResponse(o));
    }
    return response;
  }

  public String discard() {
    client.discard();
    client.getAll(1); // Discard all but the last reply
    inTransaction = false;
    clean();
    return client.getStatusCodeReply();
  }

  @Override
  public void close() throws IOException {
    clear();
  }
}