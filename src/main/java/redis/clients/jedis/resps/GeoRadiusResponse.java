package redis.clients.jedis.resps;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;
import java.util.Objects;

public class GeoRadiusResponse {

  private byte[] member;
  private double distance;
  private GeoCoordinate coordinate;
  private long rawScore;

  public GeoRadiusResponse(byte[] member) {
    this.member = member;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  public void setCoordinate(GeoCoordinate coordinate) {
    this.coordinate = coordinate;
  }

  public void setRawScore(long rawScore) {
    this.rawScore = rawScore;
  }

  public byte[] getMember() {
    return member;
  }

  public String getMemberByString() {
    return SafeEncoder.encode(member);
  }

  /**
   * @return The distance of the returned item from the specified center. The distance is returned
   * in the same unit as the unit specified as the radius argument of the command.
   */
  public double getDistance() {
    return distance;
  }

  /**
   * @return The longitude,latitude coordinates of the matching item.
   */
  public GeoCoordinate getCoordinate() {
    return coordinate;
  }

  /**
   * @return The raw geohash-encoded sorted set score of the item, in the form of a 52 bit unsigned
   * integer. This is only useful for low level hacks or debugging and is otherwise of little
   * interest for the general user.
   */
  public long getRawScore() {
    return rawScore;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof GeoRadiusResponse)) {
      return false;
    }

    GeoRadiusResponse response = (GeoRadiusResponse) obj;
    return Double.compare(distance, response.getDistance()) == 0
            && rawScore == response.getRawScore() && coordinate.equals(response.coordinate)
            && Arrays.equals(member, response.getMember());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 67 * hash + Arrays.hashCode(this.member);
    hash = 67 * hash + (int) (Double.doubleToLongBits(this.distance) ^ (Double.doubleToLongBits(this.distance) >>> 32));
    hash = 67 * hash + Objects.hashCode(this.coordinate);
    hash = 67 * hash + (int) (this.rawScore ^ (this.rawScore >>> 32));
    return hash;
  }
}
