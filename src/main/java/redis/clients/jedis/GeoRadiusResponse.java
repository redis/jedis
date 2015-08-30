package redis.clients.jedis;

import redis.clients.util.SafeEncoder;

public class GeoRadiusResponse {
  private byte[] member;
  private double distance;
  private GeoCoordinate coordinate;

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
}
