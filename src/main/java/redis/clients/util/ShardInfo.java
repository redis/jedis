package redis.clients.util;

public abstract class ShardInfo<T> {
    private T resource;

    private int weight;

    public ShardInfo() {
    }

    public ShardInfo(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return this.weight;
    }

    public T getResource() {
        return resource;
    }

    public void initResource () {
        resource = createResource();
    }

    protected abstract T createResource();
}
