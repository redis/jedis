package redis.clients.jedis;

import java.util.Map;

public class StreamEntry {
  
  final private EntryID id;
  final private Map<String, String> fields;
  
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
