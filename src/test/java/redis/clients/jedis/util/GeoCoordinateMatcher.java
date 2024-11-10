package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.GeoCoordinate;

public class GeoCoordinateMatcher extends TypeSafeMatcher<GeoCoordinate> {

  public static GeoCoordinateMatcher isEqualWithTolerance(GeoCoordinate expected, double tolerance) {
    return new GeoCoordinateMatcher(expected, tolerance);
  }

  public static GeoCoordinateMatcher isEqualWithTolerance(GeoCoordinate expected) {
    return new GeoCoordinateMatcher(expected, DEFAULT_TOLERANCE);
  }

  public static GeoCoordinateMatcher isEqualWithTolerance(double longitude, double latitude, double tolerance) {
    return new GeoCoordinateMatcher(new GeoCoordinate(longitude, latitude), tolerance);
  }

  public static GeoCoordinateMatcher isEqualWithTolerance(double longitude, double latitude) {
    return new GeoCoordinateMatcher(new GeoCoordinate(longitude,latitude), latitude);
  }

  public static final double DEFAULT_TOLERANCE = 1e-14;

  private final double tolerance;
  private final GeoCoordinate expected;


  public GeoCoordinateMatcher(GeoCoordinate expected, double tolerance) {
    this.expected = expected;
    this.tolerance = tolerance;
  }

  public GeoCoordinateMatcher(GeoCoordinate expected) {
    this(expected, DEFAULT_TOLERANCE);
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
