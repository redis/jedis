package redis.clients.jedis.params;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoRadiusParam extends Params {
  protected static final String FROMMEMBER = "frommember";
  protected static final String FROMLONLAT = "fromlonlat";
  protected static final String BYRADIUS = "byradius";
  protected static final String BYBOX = "bybox";
  protected GeoUnit unit;

  protected static final String WITHCOORD = "withcoord";
  protected static final String WITHDIST = "withdist";
  protected static final String WITHHASH = "withhash";

  protected static final String ASC = "asc";
  protected static final String DESC = "desc";
  protected static final String COUNT = "count";
  protected static final String ANY = "any";
  protected static final String STOREDIST = "storedist";


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

  public GeoRadiusParam count(int count, boolean any) {
    if (count > 0) {
      addParam(COUNT, count);
      if (any) {
        addParam(ANY);
      }
    }
    return this;
  }

  public byte[][] getByteParams(byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    Collections.addAll(byteParams, args);

    if (contains(FROMMEMBER)) {
      byteParams.add(SafeEncoder.encode(FROMMEMBER));
      byteParams.add(((String) getParam(FROMMEMBER)).getBytes());
    } else if (contains(FROMLONLAT)) {
      byteParams.add(SafeEncoder.encode(FROMLONLAT));
      GeoCoordinate lonlat = getParam(FROMLONLAT);
      byteParams.add(Protocol.toByteArray(lonlat.getLongitude()));
      byteParams.add(Protocol.toByteArray(lonlat.getLatitude()));
    }

    if (contains(BYRADIUS)) {
      byteParams.add(SafeEncoder.encode(BYRADIUS));
      byteParams.add(Protocol.toByteArray((double) getParam(BYRADIUS)));
      if (this.unit != null) {
        byteParams.add(this.unit.raw);
      }
    } else if (contains(BYBOX)) {
      byteParams.add(SafeEncoder.encode(BYBOX));
      double[] box = getParam(BYBOX);
      byteParams.add(Protocol.toByteArray(box[0]));
      byteParams.add(Protocol.toByteArray(box[1]));
      if (this.unit != null) {
        byteParams.add(this.unit.raw);
      }
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
      if (contains(ANY)) {
        byteParams.add(SafeEncoder.encode(ANY));
      }
    }

    if (contains(ASC)) {
      byteParams.add(SafeEncoder.encode(ASC));
    } else if (contains(DESC)) {
      byteParams.add(SafeEncoder.encode(DESC));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
