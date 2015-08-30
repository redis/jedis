package redis.clients.jedis;

public class GeoCoordinate {
  private double longitude;
  private double latitude;

  public GeoCoordinate(double longitude, double latitude) {
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GeoCoordinate)) return false;

    GeoCoordinate that = (GeoCoordinate) o;

    if (Double.compare(that.longitude, longitude) != 0) return false;
    return Double.compare(that.latitude, latitude) == 0;
  }

  @Override
  public int hashCode() {
    // follows IntelliJ default hashCode implementation
    int result;
    long temp;
    temp = Double.doubleToLongBits(longitude);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(latitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "(" + longitude + "," + latitude + ")";
  }
}
