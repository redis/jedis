package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
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
    this.count = count;
    return this;
  }

  public GeoRadiusParam count(int count, boolean any) {
    this.count = count;
    this.any = any;
    return this;
  }

  public GeoRadiusParam any() {
    if (this.count == null) {
      throw new IllegalArgumentException("COUNT must be set before ANY to be set");
    }
    this.any = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (withCoord) {
      args.add(Keyword.WITHCOORD);
    }
    if (withDist) {
      args.add(Keyword.WITHDIST);
    }
    if (withHash) {
      args.add(Keyword.WITHHASH);
    }

    if (count != null) {
      args.add(Keyword.COUNT).add(count);
      if (any) {
        args.add(Keyword.ANY);
      }
    }

    if (sortingOrder != null) {
      args.add(sortingOrder);
    }
  }
}
