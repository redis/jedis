package redis.clients.jedis.util;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import java.util.Map;
import java.util.Arrays;

public class ByteArrayMapMatcher extends TypeSafeMatcher<Map<byte[], byte[]>> {

  private final Map<byte[], byte[]> expected;

  public ByteArrayMapMatcher(Map<byte[], byte[]> expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(Map<byte[], byte[]> actual) {
    if (actual.size() != expected.size()) return false;

    outer:
    for (Map.Entry<byte[], byte[]> expectedEntry : expected.entrySet()) {
      for (Map.Entry<byte[], byte[]> actualEntry : actual.entrySet()) {
        if (Arrays.equals(expectedEntry.getKey(), actualEntry.getKey()) &&
            Arrays.equals(expectedEntry.getValue(), actualEntry.getValue())) {
          continue outer;
        }
      }
      return false;
    }

    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("maps to be equal by byte[] content");
  }

  public static ByteArrayMapMatcher equalToByteArrayMap(Map<byte[], byte[]> expected) {
    return new ByteArrayMapMatcher(expected);
  }
}