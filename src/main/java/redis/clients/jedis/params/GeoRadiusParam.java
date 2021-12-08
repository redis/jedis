package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.ASC;
import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.DESC;
import static redis.clients.jedis.Protocol.Keyword.WITHCOORD;
import static redis.clients.jedis.Protocol.Keyword.WITHDIST;
import static redis.clients.jedis.Protocol.Keyword.WITHHASH;

import redis.clients.jedis.CommandArguments;

public class GeoRadiusParam implements IParams {

  private boolean withCoord = false;
  private boolean withDist = false;
  private boolean withHash = false;

  private Integer count = null;
  private boolean asc = false;
  private boolean desc = false;

  public GeoRadiusParam() {
  }

  public static GeoRadiusParam geoRadiusParam() {
    return new GeoRadiusParam();
  }

  public GeoRadiusParam withCoord() {
    withCoord = true;
    return this;
  }

  public GeoRadiusParam withDist() {
    withDist = true;
    return this;
  }

  public GeoRadiusParam withHash() {
    withHash = true;
    return this;
  }

  public GeoRadiusParam sortAscending() {
    asc = true;
    return this;
  }

  public GeoRadiusParam sortDescending() {
    desc = true;
    return this;
  }

  public GeoRadiusParam count(int count) {
    if (count > 0) {
      this.count = count;
    }
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (withCoord) {
      args.add(WITHCOORD);
    }
    if (withDist) {
      args.add(WITHDIST);
    }
    if (withHash) {
      args.add(WITHHASH);
    }

    if (count != null) {
      args.add(COUNT).add(count);
    }

    if (asc) {
      args.add(ASC);
    } else if (desc) {
      args.add(DESC);
    }
  }
}
