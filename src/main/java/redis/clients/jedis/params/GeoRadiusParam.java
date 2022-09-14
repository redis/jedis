package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.WITHCOORD;
import static redis.clients.jedis.Protocol.Keyword.WITHDIST;
import static redis.clients.jedis.Protocol.Keyword.WITHHASH;
import static redis.clients.jedis.Protocol.Keyword.ANY;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.SortingOrder;

public class GeoRadiusParam implements IParams {

  private boolean withCoord = false;
  private boolean withDist = false;
  private boolean withHash = false;

  private Integer count = null;
  private boolean any = false;
  private SortingOrder sortingOrder = null;

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
    return sortingOrder(SortingOrder.ASC);
  }

  public GeoRadiusParam sortDescending() {
    return sortingOrder(SortingOrder.DESC);
  }

  public GeoRadiusParam sortingOrder(SortingOrder order) {
    this.sortingOrder = order;
    return this;
  }

  public GeoRadiusParam count(int count) {
    if (count > 0) {
      this.count = count;
    }
    return this;
  }

  public GeoRadiusParam count(int count, boolean any) {
    if (count > 0) {
      this.count = count;

      if (any) {
        this.any = true;
      }
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
      if (any) {
        args.add(ANY);
      }
    }

    if (sortingOrder != null) {
      args.add(sortingOrder);
    }
  }
}
