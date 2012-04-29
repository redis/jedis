package redis.clients.util;

public interface ShardInfo<T> {
    
    int getWeight();
    T createResource();
    String getName();
}
