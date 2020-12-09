package redis.clients.jedis;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds information about stream
 * They can be access via getters.
 * For future purpose there is also {@link #getStreamInfo} method
 * that returns a generic {@code Map} - in case where more info is returned from a server
 *
 */

public class StreamInfo implements Serializable {

  public static final String LENGTH = "length";
  public static final String RADIX_TREE_KEYS = "radix-tree-keys";
  public static final String RADIX_TREE_NODES = "radix-tree-nodes";
  public static final String GROUPS = "groups";
  public static final String LAST_GENERATED_ID = "last-generated-id";
  public static final String FIRST_ENTRY = "first-entry";
  public static final String LAST_ENTRY = "last-entry";

  private final long length;
  private final long radixTreeKeys;
  private final long radixTreeNodes;
  private final long groups;
  private final StreamEntryID lastGeneratedId;
  private final StreamEntry firstEntry;
  private final StreamEntry lastEntry;
  private final Map<String,Object> streamInfo;

  /**
   * @param map contains key-value pairs with stream info
   *
   */
  public StreamInfo(Map<String,Object> map) {

    streamInfo = map;
    length = (Long) map.get(LENGTH);
    radixTreeKeys = (Long) map.get(RADIX_TREE_KEYS);
    radixTreeNodes = (Long) map.get(RADIX_TREE_NODES);
    groups = (Long) map.get(GROUPS);
    lastGeneratedId = (StreamEntryID) map.get(LAST_GENERATED_ID);
    firstEntry = (StreamEntry) map.get(FIRST_ENTRY);
    lastEntry = (StreamEntry) map.get(LAST_ENTRY);

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

  public long getGroups() {
    return groups;
  }

  public StreamEntryID getLastGeneratedId() {
    return lastGeneratedId;
  }

  public StreamEntry getFirstEntry() {
    return firstEntry;
  }

  public StreamEntry getLastEntry() {
    return lastEntry;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String,Object> getStreamInfo() {
    return streamInfo;
  }

}
