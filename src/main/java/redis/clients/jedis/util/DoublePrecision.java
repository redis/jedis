package redis.clients.jedis.util;

public final class DoublePrecision {
  private DoublePrecision() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static Double parseFloatingPointNumber(String str) {
    if (str == null) return null;
    try {
      return Double.valueOf(str);
    } catch (NumberFormatException e) {
      if (str.equals("inf") || str.equals("+inf")) return Double.POSITIVE_INFINITY;
      if (str.equals("-inf")) return Double.NEGATIVE_INFINITY;
      throw e;
    }
  }
}
