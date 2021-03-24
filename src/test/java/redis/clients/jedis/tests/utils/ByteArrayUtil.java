package redis.clients.jedis.tests.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ByteArrayUtil {

  public static boolean byteArrayCollectionRemove(Collection<byte[]> all, byte[] element) {
    Iterator<byte[]> it = all.iterator();
    while (it.hasNext()) {
      if (Arrays.equals(it.next(), element)) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  public static boolean byteArrayCollectionRemoveAll(Collection<byte[]> all, Collection<byte[]> few) {
    boolean modified = false;
    for (byte[] e : few) {
      modified |= byteArrayCollectionRemove(all, e);
    }
    return modified;
  }
}
