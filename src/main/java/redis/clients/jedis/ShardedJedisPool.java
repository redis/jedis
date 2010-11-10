package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import redis.clients.util.FixedResourcePool;
import redis.clients.util.Hashing;

public class ShardedJedisPool extends FixedResourcePool<ShardedJedis> {
    private List<JedisShardInfo> shards;
    private Hashing algo = Hashing.MD5;
    private Pattern keyTagPattern;

    public ShardedJedisPool(List<JedisShardInfo> shards) {
        this.shards = shards;
    }

    public ShardedJedisPool(List<JedisShardInfo> shards, Hashing algo) {
        this.shards = shards;
        this.algo = algo;
    }

    public ShardedJedisPool(List<JedisShardInfo> shards, Pattern keyTagPattern) {
        this.shards = shards;
        this.keyTagPattern = keyTagPattern;
    }

    public ShardedJedisPool(List<JedisShardInfo> shards, Hashing algo,
            Pattern keyTagPattern) {
        this.shards = shards;
        this.algo = algo;
        this.keyTagPattern = keyTagPattern;
    }

    @Override
    protected ShardedJedis createResource() {
        ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
        boolean done = false;
        while (!done) {
            try {
                for (JedisShardInfo shard : jedis.getAllShards()) {
                    if (!shard.getResource().isConnected()) {
                        shard.getResource().connect();
                    }
                }
                done = true;
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }
        return jedis;
    }

    @Override
    protected void destroyResource(ShardedJedis jedis) {
        if (jedis != null) {
            try {
                jedis.disconnect();
            } catch (Exception e) {

            }
        }
    }

    @Override
    protected boolean isResourceValid(ShardedJedis jedis) {
        try {
            for (JedisShardInfo shard : jedis.getAllShards()) {
                if (!shard.getResource().isConnected()
                        || !shard.getResource().ping().equals("PONG")) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
