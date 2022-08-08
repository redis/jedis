package redis.clients.jedis.search.querybuilder;

import redis.clients.jedis.args.GeoUnit;

/**
 * Created by mnunberg on 2/23/18.
 */
public class GeoValue extends Value {

  private final String unit;
  private final double lon;
  private final double lat;
  private final double radius;

  public GeoValue(double lon, double lat, double radius, GeoUnit unit) {
    this.lon = lon;
    this.lat = lat;
    this.radius = radius;
    this.unit = unit.toString();
  }

  @Override
  public String toString() {
    return "[" + lon + " " + lat + " " + radius + " " + unit + "]";
  }

  @Override
  public boolean isCombinable() {
    return false;
  }
}
