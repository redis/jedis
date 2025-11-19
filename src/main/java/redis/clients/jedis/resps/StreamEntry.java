package redis.clients.jedis.resps;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import redis.clients.jedis.StreamEntryID;

public class StreamEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  private StreamEntryID id;
  private Map<String, String> fields;
  private Long millisElapsedFromDelivery;
  private Long deliveredCount;

  public StreamEntry(StreamEntryID id, Map<String, String> fields) {
    this.id = id;
    this.fields = fields;
  }

  public StreamEntry(StreamEntryID id, Map<String, String> fields, Long millisElapsedFromDelivery, Long deliveredCount) {
    this.id = id;
    this.fields = fields;
    this.millisElapsedFromDelivery = millisElapsedFromDelivery;
    this.deliveredCount = deliveredCount;
  }

  /**
   * @return the milliseconds since the last delivery of this message when CLAIM was used.
   *         <ul>
   *         <li>{@code null} when not applicable</li>
   *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
   *         <li>{@code > 0} means claimed from the PEL</li>
   *         </ul>
   * @since 7.1
   */
  public Long getMillisElapsedFromDelivery() {
    return millisElapsedFromDelivery;
  }

  /**
   * @return the number of prior deliveries of this message when CLAIM was used:
   *         <ul>
   *         <li>{@code null} when not applicable</li>
   *         <li>{@code 0} means not claimed from the pending entries list (PEL)</li>
   *         <li>{@code > 0} means claimed from the PEL</li>
   *         </ul>
   * @since 7.1
   */
  public Long getDeliveredCount() {
    return deliveredCount;
  }

  public boolean isClaimed() {
    return this.deliveredCount != null && this.deliveredCount > 0;
  }

  public StreamEntryID getID() {
    return id;
  }

  public Map<String, String> getFields() {
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
    this.fields = (Map<String, String>) in.readUnshared();
  }
}
