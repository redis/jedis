package redis.clients.jedis.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.ComparisonFailure;
import redis.clients.jedis.RedisProtocol;

public class AssertUtil {

  public static void assertOK(String str) {
    assertEquals("OK", str);
  }

  public static void assertEqualsByProtocol(RedisProtocol protocol, Object expectedResp2, Object expectedResp3, Object actual) {
    if (protocol != RedisProtocol.RESP3) {
      assertEquals(expectedResp2, actual);
    } else {
      assertEquals(expectedResp3, actual);
    }
  }

  public static <T> boolean assertCollectionContains(Collection<T> array, T expected) {
    for (T element : array) {
      if (Objects.equals(element, expected)) {
        return true;
      }
    }
    throw new ComparisonFailure("element is missing", Objects.toString(expected), array.toString());
  }

  public static boolean assertByteArrayCollectionContains(Collection<byte[]> array, byte[] expected) {
    for (byte[] bytes : array) {
      if (Arrays.equals(bytes, expected)) {
        return true;
      }
    }
    throw new ComparisonFailure("element is missing", Arrays.toString(expected), array.toString());
  }

  public static void assertByteArrayListEquals(List<byte[]> expected, List<byte[]> actual) {
    assertEquals(expected.size(), actual.size());
    for (int n = 0; n < expected.size(); n++) {
      assertArrayEquals(n + "'th elements don't match", expected.get(n), actual.get(n));
    }
  }

  public static void assertByteArraySetEquals(Set<byte[]> expected, Set<byte[]> actual) {
    assertEquals(expected.size(), actual.size());
    Iterator<byte[]> e = expected.iterator();
    while (e.hasNext()) {
      byte[] next = e.next();
      boolean contained = false;
      for (byte[] element : actual) {
        if (Arrays.equals(next, element)) {
          contained = true;
          break;
        }
      }
      if (!contained) {
        throw new ComparisonFailure("element is missing", Arrays.toString(next), actual.toString());
      }
    }
  }

  public static void assertCollectionContainsAll(Collection all, Collection few) {
    Iterator fi = few.iterator();
    while (fi.hasNext()) {
      Object fo = fi.next();
      boolean found = false;
      for (Object ao : all) {
        if (Objects.equals(fo, ao)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new ComparisonFailure("element is missing", Objects.toString(fo), all.toString());
      }
    }
  }

  public static void assertByteArrayCollectionContainsAll(Collection<byte[]> all,
      Collection<byte[]> few) {
    Iterator<byte[]> fi = few.iterator();
    while (fi.hasNext()) {
      byte[] fo = fi.next();
      boolean found = false;
      for (byte[] ao : all) {
        if (Arrays.equals(fo, ao)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new ComparisonFailure("element is missing", Arrays.toString(fo), all.toString());
      }
    }
  }

  public static void assertPipelineSyncAll(List<Object> expected, List<Object> actual) {
    assertEquals(expected.size(), actual.size());
    for (int n = 0; n < expected.size(); n++) {
      Object expObj = expected.get(n);
      Object actObj = actual.get(n);
      if (expObj instanceof List) {
        if (!(actObj instanceof List)) {
          throw new ComparisonFailure(n + "'th element is not a list",
              expObj.getClass().toString(), actObj.getClass().toString());
        }
        assertPipelineSyncAll((List) expObj, (List) actObj);
      } else if (expObj instanceof List) {
        if (!(actObj instanceof List)) {
          throw new ComparisonFailure(n + "'th element is not a list",
              expObj.getClass().toString(), actObj.getClass().toString());
        }
        assertPipelineSyncAll((List) expObj, (List) actObj);
      } else if (expObj instanceof Set) {
        if (!(actObj instanceof Set)) {
          throw new ComparisonFailure(n + "'th element is not a set",
              expObj.getClass().toString(), actObj.getClass().toString());
        }
        assertPipelineSyncAllSet((Set) expObj, (Set) actObj);
      } else if (expObj instanceof byte[]) {
        if (!(actObj instanceof byte[])) {
          throw new ComparisonFailure(n + "'th element is not byte array",
              expObj.getClass().toString(), actObj.getClass().toString());
        }
        assertArrayEquals((byte[]) expObj, (byte[]) actObj);
      } else {
        assertEquals(n + "'th element mismatched", expObj, actObj);
      }
    }
  }

  private static void assertPipelineSyncAllSet(Set<?> expected, Set<?> actual) {
    assertEquals(expected.size(), actual.size());
    if (expected.iterator().next() instanceof byte[]) {
      assertByteArraySetEquals((Set<byte[]>) expected, (Set<byte[]>) actual);
    } else {
      assertEquals(expected, actual);
    }
  }
}
