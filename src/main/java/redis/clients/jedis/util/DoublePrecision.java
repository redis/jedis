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

      switch (str) {

        case "inf":
        case "+inf":
          return Double.POSITIVE_INFINITY;

        case "-inf":
          return Double.NEGATIVE_INFINITY;

        case "nan":
          return Double.NaN;

        default:
          throw e;
      }
    }
  }
}
