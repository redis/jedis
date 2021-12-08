package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.CH;
import static redis.clients.jedis.Protocol.Keyword.NX;
import static redis.clients.jedis.Protocol.Keyword.XX;

import redis.clients.jedis.CommandArguments;

public class GeoAddParams implements IParams {

  private boolean nx = false;
  private boolean xx = false;
  private boolean ch = false;

  public GeoAddParams() {
  }

  public static GeoAddParams geoAddParams() {
    return new GeoAddParams();
  }

  /**
   * Don't update already existing elements. Always add new elements.
   * @return GetExParams
   */
  public GeoAddParams nx() {
    this.nx = true;
    return this;
  }

  /**
   * Only update elements that already exist. Never add elements.
   * @return GetExParams
   */
  public GeoAddParams xx() {
    this.xx = true;
    return this;
  }

  /**
   * Modify the return value from the number of new elements added, to the total number of elements
   * changed
   * @return GetExParams
   */
  public GeoAddParams ch() {
    this.ch = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (nx) {
      args.add(NX);
    } else if (xx) {
      args.add(XX);
    }

    if (ch) {
      args.add(CH);
    }
  }

}
