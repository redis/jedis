package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;

public class GeoSearchParam implements IParams {

  private boolean fromMember = false;
  private boolean fromLonLat = false;
  private String member;
  private GeoCoordinate coord;

  private boolean byRadius = false;
  private boolean byBox = false;
  private double radius;
  private double width;
  private double height;
  private GeoUnit unit;

  private boolean withCoord = false;
  private boolean withDist = false;
  private boolean withHash = false;

  private Integer count = null;
  private boolean any = false;
  private SortingOrder sortingOrder = null;

  public GeoSearchParam() { }

  public static GeoSearchParam geoSearchParam() { return new GeoSearchParam(); }

  public GeoSearchParam fromMember(String member) {
    this.fromMember = true;
    this.member = member;
    return this;
  }

  public GeoSearchParam fromLonLat(double longitude, double latitude) {
    this.fromLonLat = true;
    this.coord = new GeoCoordinate(longitude, latitude);
    return this;
  }

  public GeoSearchParam fromLonLat(GeoCoordinate coord) {
    this.fromLonLat = true;
    this.coord = coord;
    return this;
  }


  public GeoSearchParam byRadius(double radius, GeoUnit unit){
    this.byRadius = true;
    this.radius = radius;
    this.unit = unit;
    return this;
  }

  public GeoSearchParam byBox(double width, double height, GeoUnit unit){
    this.byBox = true;
    this.width = width;
    this.height = height;
    this.unit = unit;
    return this;
  }

  public GeoSearchParam withCoord() {
    withCoord = true;
    return this;
  }

  public GeoSearchParam withDist() {
    withDist = true;
    return this;
  }

  public GeoSearchParam withHash() {
    withHash = true;
    return this;
  }

  public GeoSearchParam asc() {
    return sortingOrder(SortingOrder.ASC);
  }

  public GeoSearchParam desc() {
    return sortingOrder(SortingOrder.DESC);
  }

  public GeoSearchParam sortingOrder(SortingOrder order) {
    sortingOrder = order;
    return this;
  }

  public GeoSearchParam count(int count) {
    this.count = count;
    return this;
  }

  public GeoSearchParam count(int count, boolean any) {
    this.count = count;
    this.any = true;
    return this;
  }

  public GeoSearchParam any() {
    if (this.count == null) {
      throw new IllegalArgumentException("COUNT must be set before ANY to be set");
    }
    this.any = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (this.fromMember) {
      args.add(Keyword.FROMMEMBER).add(this.member);
    } else if (this.fromLonLat) {
      args.add(Keyword.FROMLONLAT).add(coord.getLongitude()).add(coord.getLatitude());
    }

    if (this.byRadius) {
      args.add(Keyword.BYRADIUS).add(this.radius);
    } else if (this.byBox) {
      args.add(Keyword.BYBOX).add(this.width).add(this.height);
    }
    args.add(this.unit);

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
