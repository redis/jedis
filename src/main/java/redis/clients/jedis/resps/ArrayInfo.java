package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds information about an array returned by the {@code ARINFO} command. Known fields
 * can be accessed via getters; {@link #getArrayInfo()} returns the underlying {@link Map} so that
 * callers can read fields that are not yet promoted to typed getters or that the server may add in
 * the future.
 */
public class ArrayInfo implements Serializable {

  public static final String COUNT = "count";
  public static final String LEN = "len";
  public static final String NEXT_INSERT_INDEX = "next-insert-index";

  private final Long count;
  private final Long length;
  private final Long next;
  private final Map<String, Object> arrayInfo;

  /**
   * @param map contains key-value pairs with array info
   */
  public ArrayInfo(Map<String, Object> map) {
    arrayInfo = map;
    count = (Long) map.get(COUNT);
    length = (Long) map.get(LEN);
    next = (Long) map.get(NEXT_INSERT_INDEX);
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
   * @return the raw map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getArrayInfo() {
    return arrayInfo;
  }
}
