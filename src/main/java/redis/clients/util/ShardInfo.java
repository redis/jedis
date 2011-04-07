package redis.clients.util;

public abstract class ShardInfo<T> {
    private final int weight;

    public ShardInfo(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return this.weight;
    }

    protected abstract T createResource();
}
