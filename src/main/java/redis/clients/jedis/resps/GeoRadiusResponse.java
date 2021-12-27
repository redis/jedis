package redis.clients.jedis.resps;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;

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

  public byte[] getMember() {
    return member;
  }

  public String getMemberByString() {
    return SafeEncoder.encode(member);
  }

  public double getDistance() {
    return distance;
  }

  public GeoCoordinate getCoordinate() {
    return coordinate;
  }

  public long getRawScore() {
    return rawScore;
  }

  public void setRawScore(long rawScore) {
    this.rawScore = rawScore;
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
}
