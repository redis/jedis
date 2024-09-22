package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class GeoRadiusStoreParam implements IParams {

  private boolean store = false;
  private boolean storeDist = false;
  private String key;

  public GeoRadiusStoreParam() {
  }

  public static GeoRadiusStoreParam geoRadiusStoreParam() {
    return new GeoRadiusStoreParam();
  }

  /**
   * WARNING: In Redis, if STOREDIST exists, store will be ignored.
   * <p>
   * Refer: https://github.com/antirez/redis/blob/6.0/src/geo.c#L649
   */
  public GeoRadiusStoreParam store(String key) {
    if (key != null) {
      this.store = true;
      this.key = key;
    }
    return this;
  }

  public GeoRadiusStoreParam storeDist(String key) {
    if (key != null) {
      this.storeDist = true;
      this.key = key;
    }
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (storeDist) {
      args.add(Keyword.STOREDIST).key(key);
    } else if (store) {
      args.add(Keyword.STORE).key(key);
    } else {
      throw new IllegalArgumentException(this.getClass().getSimpleName()
          + " must has store or storedist option");
    }
  }
}
