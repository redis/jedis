package redis.clients.jedis.params.geo;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.Params;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;

public class GeoRadiusParam extends Params {
  private static final String WITHCOORD = "withcoord";
  private static final String WITHDIST = "withdist";

  // Do not add WITHHASH since we can't classify result of WITHHASH and WITHDIST,
  // and WITHHASH is for debugging purposes

  private static final String ASC = "asc";
  private static final String DESC = "desc";
  private static final String COUNT = "count";

  private GeoRadiusParam() {
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
    ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
    for (byte[] arg : args) {
      byteParams.add(arg);
    }

    if (contains(WITHCOORD)) {
      byteParams.add(SafeEncoder.encode(WITHCOORD));
    }
    if (contains(WITHDIST)) {
      byteParams.add(SafeEncoder.encode(WITHDIST));
    }

    if (contains(COUNT)) {
      byteParams.add(SafeEncoder.encode(COUNT));
      byteParams.add(Protocol.toByteArray((Integer) getParam(COUNT)));
    }

    if (contains(ASC)) {
      byteParams.add(SafeEncoder.encode(ASC));
    } else if (contains(DESC)) {
      byteParams.add(SafeEncoder.encode(DESC));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
