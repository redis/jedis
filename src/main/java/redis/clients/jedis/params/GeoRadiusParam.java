package redis.clients.jedis.params;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;

public class GeoRadiusParam extends Params {
  private static final String WITHCOORD = "withcoord";
  private static final String WITHDIST = "withdist";
  private static final String WITHHASH = "withhash";

  private static final String ASC = "asc";
  private static final String DESC = "desc";
  private static final String COUNT = "count";

  public GeoRadiusParam() {
  }

  public static GeoRadiusParam geoRadiusParam() {
    return new GeoRadiusParam();
  }

  public GeoRadiusParam withCoord() {
    addParam(WITHCOORD);
    return this;
  }

  public GeoRadiusParam withDist() {
    addParam(WITHDIST);
    return this;
  }

  public GeoRadiusParam withHash() {
    addParam(WITHHASH);
    return this;
  }

  public GeoRadiusParam sortAscending() {
    addParam(ASC);
    return this;
  }

  public GeoRadiusParam sortDescending() {
    addParam(DESC);
    return this;
  }

  public GeoRadiusParam count(int count) {
    if (count > 0) {
      addParam(COUNT, count);
    }
    return this;
  }

  public byte[][] getByteParams(byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    for (byte[] arg : args) {
      byteParams.add(arg);
    }

    if (contains(WITHCOORD)) {
      byteParams.add(SafeEncoder.encode(WITHCOORD));
    }
    if (contains(WITHDIST)) {
      byteParams.add(SafeEncoder.encode(WITHDIST));
    }
    if (contains(WITHHASH)) {
      byteParams.add(SafeEncoder.encode(WITHHASH));
    }

    if (contains(COUNT)) {
      byteParams.add(SafeEncoder.encode(COUNT));
      byteParams.add(Protocol.toByteArray((int) getParam(COUNT)));
    }

    if (contains(ASC)) {
      byteParams.add(SafeEncoder.encode(ASC));
    } else if (contains(DESC)) {
      byteParams.add(SafeEncoder.encode(DESC));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
