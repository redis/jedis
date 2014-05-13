package redis.clients.util;

public abstract class ShardInfo<T> {
    public static final int DEFAULT_WEIGHT = 1;

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
