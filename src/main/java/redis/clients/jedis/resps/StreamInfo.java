package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;
import redis.clients.jedis.StreamEntryID;

/**
 * This class holds information about stream. They can be accessed via getters. There is also
 * {@link StreamInfo#getStreamInfo} method that returns a generic {@code Map} in case more info are
 * returned from the server.
 */
public class StreamInfo implements Serializable {

  public static final String LENGTH = "length";
  public static final String RADIX_TREE_KEYS = "radix-tree-keys";
  public static final String RADIX_TREE_NODES = "radix-tree-nodes";
  public static final String GROUPS = "groups";
  public static final String LAST_GENERATED_ID = "last-generated-id";
  public static final String FIRST_ENTRY = "first-entry";
  public static final String LAST_ENTRY = "last-entry";
  public static final String IDMP_DURATION = "idmp-duration";
  public static final String IDMP_MAXSIZE = "idmp-maxsize";
  public static final String PIDS_TRACKED = "pids-tracked";
  public static final String IIDS_TRACKED = "iids-tracked";
  public static final String IIDS_ADDED = "iids-added";
  public static final String IIDS_DUPLICATES = "iids-duplicates";

  private final long length;
  private final long radixTreeKeys;
  private final long radixTreeNodes;
  private final long groups;
  private final StreamEntryID lastGeneratedId;
  private final StreamEntry firstEntry;
  private final StreamEntry lastEntry;
  private final Long idmpDuration;
  private final Long idmpMaxsize;
  private final Long pidsTracked;
  private final Long iidsTracked;
  private final Long iidsAdded;
  private final Long iidsDuplicates;
  private final Map<String, Object> streamInfo;

  /**
   * @param map contains key-value pairs with stream info
   */
  public StreamInfo(Map<String, Object> map) {

    streamInfo = map;
    length = (Long) map.get(LENGTH);
    radixTreeKeys = (Long) map.get(RADIX_TREE_KEYS);
    radixTreeNodes = (Long) map.get(RADIX_TREE_NODES);
    groups = (Long) map.get(GROUPS);
    lastGeneratedId = (StreamEntryID) map.get(LAST_GENERATED_ID);
    firstEntry = (StreamEntry) map.get(FIRST_ENTRY);
    lastEntry = (StreamEntry) map.get(LAST_ENTRY);
    idmpDuration = (Long) map.get(IDMP_DURATION);
    idmpMaxsize = (Long) map.get(IDMP_MAXSIZE);
    pidsTracked = (Long) map.get(PIDS_TRACKED);
    iidsTracked = (Long) map.get(IIDS_TRACKED);
    iidsAdded = (Long) map.get(IIDS_ADDED);
    iidsDuplicates = (Long) map.get(IIDS_DUPLICATES);

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
   * @return The duration (in seconds) that each idempotent ID is kept, or null if not configured
   */
  public Long getIdmpDuration() {
    return idmpDuration;
  }

  /**
   * @return The maximum number of most recent idempotent IDs kept for each producer ID, or null if not configured
   */
  public Long getIdmpMaxsize() {
    return idmpMaxsize;
  }

  /**
   * @return The number of producer IDs currently tracked in the stream, or null if not available
   */
  public Long getPidsTracked() {
    return pidsTracked;
  }

  /**
   * @return The number of idempotent IDs currently tracked in the stream (for all producers), or null if not available
   */
  public Long getIidsTracked() {
    return iidsTracked;
  }

  /**
   * @return The count of all entries with an idempotent ID added to the stream during its lifetime (not including duplicates), or null if not available
   */
  public Long getIidsAdded() {
    return iidsAdded;
  }

  /**
   * @return The count of duplicate idempotent IDs (for all producers) detected during the stream's lifetime, or null if not available
   */
  public Long getIidsDuplicates() {
    return iidsDuplicates;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getStreamInfo() {
    return streamInfo;
  }

}
