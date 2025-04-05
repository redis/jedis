package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.GeoCoordinate;

public class GeoCoordinateMatcher extends TypeSafeMatcher<GeoCoordinate> {

  public static GeoCoordinateMatcher atCoordinates(double longitude, double latitude) {
    return atCoordinates(new GeoCoordinate(longitude, latitude));
  }

  static GeoCoordinateMatcher atCoordinates(GeoCoordinate expected) {
    return new GeoCoordinateMatcher(expected);
  }

  private static final double EPSILON = 1e-5;

  private final GeoCoordinate expected;

  public GeoCoordinateMatcher(GeoCoordinate expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(GeoCoordinate actual) {
    return Math.abs(actual.getLatitude() - expected.getLatitude()) < EPSILON &&
            Math.abs(actual.getLongitude() - expected.getLongitude()) < EPSILON;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a GeoCoordinate within ")
            .appendValue(EPSILON)
            .appendText(" of ")
            .appendValue(expected);
  }

  @Override
  protected void describeMismatchSafely(GeoCoordinate actual, Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendValue(actual);
  }
}
