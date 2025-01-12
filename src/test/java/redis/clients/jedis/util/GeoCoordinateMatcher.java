package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.GeoCoordinate;

public class GeoCoordinateMatcher extends TypeSafeMatcher<GeoCoordinate> {

  private static final double DEFAULT_TOLERANCE = 1e-5;

  private final GeoCoordinate expected;
  private final double tolerance = DEFAULT_TOLERANCE;

  public GeoCoordinateMatcher(GeoCoordinate expected) {
    this.expected = expected;
  }

  public static GeoCoordinateMatcher isEqualWithTolerance(GeoCoordinate expected) {
    return new GeoCoordinateMatcher(expected);
  }

  public static GeoCoordinateMatcher isEqualWithTolerance(double longitude, double latitude) {
    return isEqualWithTolerance(new GeoCoordinate(longitude, latitude));
  }

  @Override
  protected boolean matchesSafely(GeoCoordinate actual) {
    return Math.abs(actual.getLatitude() - expected.getLatitude()) < tolerance &&
            Math.abs(actual.getLongitude() - expected.getLongitude()) < tolerance;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a GeoCoordinate within ")
            .appendValue(tolerance)
            .appendText(" of ")
            .appendValue(expected);
  }

  @Override
  protected void describeMismatchSafely(GeoCoordinate actual, Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendValue(actual);
  }
}
