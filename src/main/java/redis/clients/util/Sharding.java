package redis.clients.util;

import java.util.Collection;
import java.util.regex.Pattern;

public interface Sharding<R, S extends ShardInfo<R>> {
    
    public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
            .compile("\\{(.+?)\\}");

    public String getKeyTag(String key);

    public R getShard(byte[] key);
    
    public R getShard(String key);
    
    public S getShardInfo(byte[] key);
    
    public S getShardInfo(String key);
    
    public Collection<S> getAllShardInfo();

    public Collection<R> getAllShards();
}
