package redis.clients.jedis.search.querybuilder;

/**
 * Created by mnunberg on 2/23/18.
 */
public abstract class Value {
  public boolean isCombinable() {
    return false;
  }

  public abstract String toString();
}
