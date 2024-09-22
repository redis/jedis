package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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
      args.add(Keyword.NX);
    } else if (xx) {
      args.add(Keyword.XX);
    }

    if (ch) {
      args.add(Keyword.CH);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GeoAddParams that = (GeoAddParams) o;
    return nx == that.nx && xx == that.xx && ch == that.ch;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nx, xx, ch);
  }
}
