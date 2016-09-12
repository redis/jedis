package redis.clients.util;

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

  protected abstract T createResource(String clientName);

  public abstract String getName();
}
