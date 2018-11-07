package redis.clients.jedis;

import java.util.Map;

public class StreamEntry {
  
  private final EntryID id;
  private final Map<String, String> fields;
  
  public StreamEntry(EntryID id, Map<String, String> fields) {
    this.id = id;
    this.fields = fields;
  }
  
  public EntryID getID() {
    return id;
  }
  
  public Map<String, String> getFields() {
    return fields;
  }
  
  public String toString() {
    return id + " " + fields;
  }
}
