package redis.clients.jedis.util;

/**
 * @deprecated This class will be removed in next major release.
 */
@Deprecated
public abstract class ShardInfo<T> {
  private int weight;

  public ShardInfo() {
  }

  public ShardInfo(int weight) {
    this.weight = weight;
  }

  public int getWeight() {
    return this.weight;
  }

  protected abstract T createResource();

  public abstract String getName();
}
