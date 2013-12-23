package redis.clients.util;

import java.util.Collection;

public interface Sharding<R, S extends ShardInfo<R>> {

    public R getShard(byte[] key);
    
    public R getShard(String key);
    
    public S getShardInfo(byte[] key);
    
    public S getShardInfo(String key);
    
    public Collection<S> getAllShardInfo();

    public Collection<R> getAllShards();
}
