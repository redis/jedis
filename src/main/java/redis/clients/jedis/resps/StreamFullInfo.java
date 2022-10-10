package redis.clients.jedis.resps;

import redis.clients.jedis.StreamEntryID;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class holds information about a stream info with command <code>xinfo stream mystream full<code/>.
 * They can be access via getters. For future purpose there is also {@link #getStreamFullInfo()} method
 * that returns a generic {@code Map} - in case where more info is returned from the server.
 */
public class StreamFullInfo implements Serializable {

  public static final String LENGTH = "length";
  public static final String RADIX_TREE_KEYS = "radix-tree-keys";
  public static final String RADIX_TREE_NODES = "radix-tree-nodes";
  public static final String GROUPS = "groups";
  public static final String LAST_GENERATED_ID = "last-generated-id";
  public static final String ENTRIES = "entries";

  private final long length;
  private final long radixTreeKeys;
  private final long radixTreeNodes;
  private final List<StreamGroupFullInfo> groups;
  private final StreamEntryID lastGeneratedId;
  private final List<StreamEntry> entries;
  private final Map<String, Object> streamFullInfo;

  /**
   * @param map contains key-value pairs with stream info
   */
  @SuppressWarnings("unchecked")
  public StreamFullInfo(Map<String, Object> map) {

    streamFullInfo = map;
    length = (Long) map.get(LENGTH);
    radixTreeKeys = (Long) map.get(RADIX_TREE_KEYS);
    radixTreeNodes = (Long) map.get(RADIX_TREE_NODES);
    groups = (List<StreamGroupFullInfo>) map.get(GROUPS);
    lastGeneratedId = (StreamEntryID) map.get(LAST_GENERATED_ID);
    entries = (List<StreamEntry>) map.get(ENTRIES);

  }

  public long getLength() {
    return length;
  }

  public long getRadixTreeKeys() {
    return radixTreeKeys;
  }

  public long getRadixTreeNodes() {
    return radixTreeNodes;
  }

  public List<StreamGroupFullInfo> getGroups() {
    return groups;
  }

  public StreamEntryID getLastGeneratedId() {
    return lastGeneratedId;
  }

  public List<StreamEntry> getEntries() {
    return entries;
  }

  public Map<String, Object> getStreamFullInfo() {
    return streamFullInfo;
  }
}
