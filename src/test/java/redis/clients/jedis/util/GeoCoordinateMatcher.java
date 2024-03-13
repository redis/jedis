package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.GeoCoordinate;

public class GeoCoordinateMatcher extends TypeSafeMatcher<GeoCoordinate> {

  public static GeoCoordinateMatcher atCoordinates(double longitude, double latitude) {
    return new GeoCoordinateMatcher(longitude, latitude);
  }

  private static final double EPSILON = 1e-5;

  private final double longitude;
  private final double latitude;

  public GeoCoordinateMatcher(double longitude, double latitude) {
    this.longitude = longitude;
    this.latitude = latitude;
  }

  @Override
  protected boolean matchesSafely(GeoCoordinate item) {
    return item != null &&
        Math.abs(longitude - item.getLongitude()) < EPSILON &&
        Math.abs(latitude - item.getLatitude()) < EPSILON;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("matches " + longitude + " longitude " + latitude + " latitude with precision " + EPSILON);
  }
}
