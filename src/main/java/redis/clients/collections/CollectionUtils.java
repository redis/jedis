package redis.clients.collections;

public class CollectionUtils {
  private static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  /**
   * Calculate capacity of map from expectedSize to keep map from being resized.
   * This method is ported from Google's guava library.
   * See {@link com.google.common.collect.Maps#capacity}
   */
  public static int mapCapacity(int expectedSize) {
    if (expectedSize < 3) {
      return 4;
    }
    if (expectedSize < MAX_POWER_OF_TWO) {
      return expectedSize + expectedSize / 3;
    }
    return Integer.MAX_VALUE;
  }
}
