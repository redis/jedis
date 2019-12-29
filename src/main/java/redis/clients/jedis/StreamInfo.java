package redis.clients.jedis;

import java.io.Serializable;
import java.util.Map;

public class StreamInfo implements Serializable {

  public static final String LENGHT = "length";
  public static final String RADIX_TREE_KEYS = "radix-tree-keys";
  public static final String RADIX_TREE_NODES = "radix-tree-nodes";
  public static final String GROUPS = "groups";
  public static final String LAST_GENERATED_ID = "last-generated-id";
  public static final String FIRST_ENTRY = "first-entry";
  public static final String LAST_ENTRY = "last-entry";
  public static final String STREAM_INFO = "stream";

  private final Long length;
  private final Long radixTreeKeys;

  private final Long radixTreeNodes;
  private final Long groups;
  private final String lastGeneratedId;
  private final StreamEntry firstEntry;
  private final StreamEntry lastEntry;

  public StreamInfo(Map<String,Object> map) {

    if (map!= null) {
      length = (Long) map.get(LENGHT);
      radixTreeKeys = (Long) map.get(RADIX_TREE_KEYS);
      radixTreeNodes = (Long) map.get(RADIX_TREE_NODES);
      groups = (Long) map.get(GROUPS);
      lastGeneratedId = (String) map.get(LAST_GENERATED_ID);
      firstEntry = (StreamEntry) map.get(FIRST_ENTRY);
      lastEntry = (StreamEntry) map.get(LAST_ENTRY);

    } else throw  new IllegalArgumentException("InfoMap can not be null");
  }

  public Long getLength() {
    return length;
  }

  public Long getRadixTreeKeys() {
    return radixTreeKeys;
  }

  public Long getRadixTreeNodes() {
    return radixTreeNodes;
  }

  public Long getGroups() {
    return groups;
  }

  public String getLastGeneratedId() {
    return lastGeneratedId;
  }

  public StreamEntry getFirstEntry() {
    return firstEntry;
  }

  public StreamEntry getLastEntry() {
    return lastEntry;
  }

  public static class StreamInfoType {

    private static final StreamInfoType streamInfoType = new StreamInfoType();
    private StreamInfoType() {
      //Should not be used
    };

    public static StreamInfoType getStreamInfoType() {
      return streamInfoType;
    }
  }
}
