package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds information about an array returned by {@code ARINFO key FULL}. Known top-level
 * fields can be accessed via getters; {@link #getArrayFullInfo()} returns the underlying
 * {@link Map} so that callers can read additional aggregate fields that are not yet promoted to
 * typed getters or that the server may add in the future.
 * <p>
 * This class is intentionally a sibling of {@link ArrayInfo} (not a subclass): the two replies map
 * to two distinct command shapes.
 */
public class ArrayFullInfo implements Serializable {

  public static final String COUNT = "count";
  public static final String LEN = "len";
  public static final String NEXT_INSERT_INDEX = "next-insert-index";
  public static final String SLICES = "slices";

  private final Long count;
  private final Long length;
  private final Long next;
  private final Map<String, Object> arrayFullInfo;

  /**
   * @param map contains key-value pairs with array info (including the additional aggregate fields
   *          reported by {@code ARINFO key FULL})
   */
  public ArrayFullInfo(Map<String, Object> map) {
    arrayFullInfo = map;
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
   * @return the raw map containing all key-value pairs returned by the server, including any
   *         additional aggregate fields reported only by {@code ARINFO key FULL}
   */
  public Map<String, Object> getArrayFullInfo() {
    return arrayFullInfo;
  }
}
