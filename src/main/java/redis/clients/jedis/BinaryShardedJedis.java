package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;
import redis.clients.util.Sharding;

public class BinaryShardedJedis extends BinaryShardedJedisBase {
    
    public BinaryShardedJedis(Sharding<Jedis, JedisShardInfo> shards) {
	super(shards);
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards) {
	super(new Sharded<Jedis, JedisShardInfo>(shards));
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo) {
	super(new Sharded<Jedis, JedisShardInfo>(shards, algo));
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Pattern keyTagPattern) {
	super(new Sharded<Jedis, JedisShardInfo>(shards, keyTagPattern));
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo,
	    Pattern keyTagPattern) {
	super(new Sharded<Jedis, JedisShardInfo>(shards, algo, keyTagPattern));
    }
}
