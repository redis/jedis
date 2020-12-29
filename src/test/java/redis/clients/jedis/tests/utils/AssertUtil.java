package redis.clients.jedis.tests.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.ComparisonFailure;

public class AssertUtil {

  public static boolean assertCollectionContains(Collection<byte[]> array, byte[] expected) {
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
      assertArrayEquals(expected.get(n), actual.get(n));
    }
  }

  public static void assertByteArraySetEquals(Set<byte[]> expected, Set<byte[]> actual) {
    assertEquals(expected.size(), actual.size());
    Iterator<byte[]> e = expected.iterator();
    while (e.hasNext()) {
      byte[] next = e.next();
      boolean contained = false;
      for (byte[] element : expected) {
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

  public static void assertByteArrayCollectionContainsAll(Collection<byte[]> all, Collection<byte[]> few) {
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

}
