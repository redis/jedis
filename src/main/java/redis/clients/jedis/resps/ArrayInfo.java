package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;

import redis.clients.jedis.annots.Experimental;

/**
 * This class holds information about an array returned by the {@code ARINFO} command. Known fields
 * can be accessed via getters; {@link #getArrayInfo()} returns the underlying {@link Map} so that
 * callers can read fields that are not yet promoted to typed getters or that the server may add in
 * the future.
 */
@Experimental
public class ArrayInfo implements Serializable {

  public static final String COUNT = "count";
  public static final String LEN = "len";
  public static final String NEXT_INSERT_INDEX = "next-insert-index";
  public static final String SLICES = "slices";
  public static final String DIRECTORY_SIZE = "directory-size";
  public static final String SUPER_DIR_ENTRIES = "super-dir-entries";
  public static final String SLICE_SIZE = "slice-size";

  private final Long count;
  private final Long length;
  private final Long next;
  private final Long slices;
  private final Long directorySize;
  private final Long superDirEntries;
  private final Long sliceSize;
  private final Map<String, Object> arrayInfo;

  /**
   * @param map contains key-value pairs with array info
   */
  public ArrayInfo(Map<String, Object> map) {
    arrayInfo = map;
    count = (Long) map.get(COUNT);
    length = (Long) map.get(LEN);
    next = (Long) map.get(NEXT_INSERT_INDEX);
    slices = (Long) map.get(SLICES);
    directorySize = (Long) map.get(DIRECTORY_SIZE);
    superDirEntries = (Long) map.get(SUPER_DIR_ENTRIES);
    sliceSize = (Long) map.get(SLICE_SIZE);
  }

  /**
   * @return the number of non-empty elements in the array, or {@code null} if not reported
   */
  public Long getCount() {
    return count;
  }

  /**
   * @return the array length (max index + 1), or {@code null} if not reported
   */
  public Long getLength() {
    return length;
  }

  /**
   * @return the next index that {@code ARINSERT} would use, or {@code null} if not reported
   */
  public Long getNext() {
    return next;
  }

  /**
   * @return the total number of slices backing the array, or {@code null} if not reported
   */
  public Long getSlices() {
    return slices;
  }

  /**
   * @return the directory size, or {@code null} if not reported
   */
  public Long getDirectorySize() {
    return directorySize;
  }

  /**
   * @return the number of super-directory entries, or {@code null} if not reported
   */
  public Long getSuperDirEntries() {
    return superDirEntries;
  }

  /**
   * @return the slice size, or {@code null} if not reported
   */
  public Long getSliceSize() {
    return sliceSize;
  }

  /**
   * @return the raw map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getArrayInfo() {
    return arrayInfo;
  }
}
