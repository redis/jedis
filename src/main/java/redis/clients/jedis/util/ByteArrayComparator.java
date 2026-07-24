package redis.clients.jedis.util;

public final class ByteArrayComparator {
  private ByteArrayComparator() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static int compare(final byte[] val1, final byte[] val2) {
    int len1 = val1.length;
    int len2 = val2.length;
    int lmin = Math.min(len1, len2);

    for (int i = 0; i < lmin; i++) {
      int result = Integer.compare(val1[i] & 0xFF, val2[i] & 0xFF);
      if (result != 0) return result;
    }

    if (len1 < len2) return -1;
    if (len1 > len2) return 1;
    return 0;
  }
}
