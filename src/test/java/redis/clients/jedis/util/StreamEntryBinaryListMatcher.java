package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.resps.StreamEntryBinary;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class StreamEntryBinaryListMatcher extends TypeSafeMatcher<List<StreamEntryBinary>> {

  private final List<StreamEntryBinary> expected;

  public StreamEntryBinaryListMatcher(List<StreamEntryBinary> expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(List<StreamEntryBinary> actual) {
    if (actual.size() != expected.size()) return false;

    for (int i = 0; i < expected.size(); i++) {
      StreamEntryBinary e = expected.get(i);
      StreamEntryBinary a = actual.get(i);

      if (!e.getID().equals(a.getID())) return false;
      if (!mapsEqual(e.getFields(), a.getFields())) return false;
    }

    return true;
  }

  private boolean mapsEqual(Map<byte[], byte[]> m1, Map<byte[], byte[]> m2) {
    if (m1.size() != m2.size()) return false;

    outer:
    for (Map.Entry<byte[], byte[]> e1 : m1.entrySet()) {
      for (Map.Entry<byte[], byte[]> e2 : m2.entrySet()) {
        if (Arrays.equals(e1.getKey(), e2.getKey()) &&
            Arrays.equals(e1.getValue(), e2.getValue())) {
          continue outer;
        }
      }
      return false;
    }

    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("StreamEntryBinary lists to match by ID and field content");
  }

  public static StreamEntryBinaryListMatcher equalsStreamEntries(List<StreamEntryBinary> expected) {
    return new StreamEntryBinaryListMatcher(expected);
  }
}