package redis.clients.jedis.resps;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import redis.clients.jedis.StreamEntryID;

public class StreamEntryBinary implements Serializable {

  private static final long serialVersionUID = 1L;

  private StreamEntryID id;
  private Map<byte[], byte[]> fields;

  public StreamEntryBinary(StreamEntryID id, Map<byte[], byte[]> fields) {
    this.id = id;
    this.fields = fields;
  }

  public StreamEntryID getID() {
    return id;
  }

  public Map<byte[], byte[]> getFields() {
    return fields;
  }

  @Override
  public String toString() {
    return id + " " + fields;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeUnshared(this.id);
    out.writeUnshared(this.fields);
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.id = (StreamEntryID) in.readUnshared();
    this.fields = (Map<byte[], byte[]>) in.readUnshared();
  }
}