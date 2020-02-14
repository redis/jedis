package redis.clients.jedis.params;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GeoRadiusParam extends Params {
  private static final String WITHCOORD = "withcoord";
  private static final String WITHDIST = "withdist";

  // Do not add WITHHASH since we can't classify result of WITHHASH and WITHDIST,
  // and WITHHASH is for debugging purposes

  private static final String ASC = "asc";
  private static final String DESC = "desc";
  private static final String COUNT = "count";

  private static final String STORE = "store";
  private static final String STOREDIST = "storedist";

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

  public GeoRadiusParam store(String key) {
    if (key != null) {
      addParam(STORE, key);
    }
    return this;
  }

  public GeoRadiusParam storeDist(String key) {
    if (key != null) {
      addParam(STOREDIST, key);
    }
    return this;
  }

  public String[] getStringKeys(String key) {
    List<String> keys = new LinkedList<>();
    keys.add(key);

    if (contains(STORE)) {
      keys.add((String)getParam(STORE));
    }

    if (contains(STOREDIST)) {
      keys.add((String)getParam(STOREDIST));
    }
    return keys.toArray(new String[keys.size()]);
  }

  public byte[][] getByteKeys(byte[] key) {
    List<byte[]> keys = new LinkedList<>();
    keys.add(key);

    if (contains(STORE)) {
      keys.add(SafeEncoder.encode((String)getParam(STORE)));
    }

    if (contains(STOREDIST)) {
      keys.add(SafeEncoder.encode((String)getParam(STOREDIST)));
    }
    return keys.toArray(new byte[keys.size()][]);
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
      byteParams.add(Protocol.toByteArray((int) getParam(COUNT)));
    }

    if (contains(STORE)) {
      byteParams.add(SafeEncoder.encode(STORE));
      byteParams.add(SafeEncoder.encode((String)getParam(STORE)));
    }

    if (contains(STOREDIST)) {
      byteParams.add(SafeEncoder.encode(STOREDIST));
      byteParams.add(SafeEncoder.encode((String)getParam(STOREDIST)));
    }

    if (contains(ASC)) {
      byteParams.add(SafeEncoder.encode(ASC));
    } else if (contains(DESC)) {
      byteParams.add(SafeEncoder.encode(DESC));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
