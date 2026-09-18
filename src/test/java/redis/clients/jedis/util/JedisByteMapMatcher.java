package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.Map;

public class JedisByteMapMatcher<T> extends TypeSafeMatcher<Map<byte[], T>> {

  private final Map<byte[], T> expected;

  public JedisByteMapMatcher(Map<byte[], T> expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(Map<byte[], T> actual) {
    if (actual == null) {
      return expected == null;
    }

    if (actual.size() != expected.size()) return false;

    // For each expected key, find the matching key in actual and verify the value matches
    for (Map.Entry<byte[], T> expectedEntry : expected.entrySet()) {
      byte[] expectedKey = expectedEntry.getKey();
      T expectedValue = expectedEntry.getValue();

      // Find the actual entry with matching key
      boolean keyFound = false;
      for (Map.Entry<byte[], T> actualEntry : actual.entrySet()) {
        if (Arrays.equals(expectedKey, actualEntry.getKey())) {
          keyFound = true;
          // Verify the value for this key matches
          if (!expectedValue.equals(actualEntry.getValue())) {
            return false; // Key found but value doesn't match
          }
          break;
        }
      }

      if (!keyFound) {
        return false; // Expected key not found in actual
      }
    }

    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("maps to be equal by byte[] content");
  }

  public static <T> JedisByteMapMatcher<T> contentEquals(Map<byte[], T> expected) {
    return new JedisByteMapMatcher<T>(expected);
  }
}