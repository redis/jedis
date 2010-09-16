package redis.clients.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class Sharded<R, S extends ShardInfo<R>> {
    public static final int DEFAULT_WEIGHT = 1;
    private TreeMap<Long, S> nodes;
    private final Hashing algo;

    public Sharded(List<S> shards) {
        this(shards, Hashing.MURMUR_HASH); // MD5 is really not good as we works with 64-bits not 128
    }

    public Sharded(List<S> shards, Hashing algo) {
        this.algo = algo;
        initialize(shards);
    }

    private void initialize(List<S> shards) {
        nodes = new TreeMap<Long, S>();

        int totalWeight = 0;

        for (ShardInfo shard : shards) {
            totalWeight += shard.getWeight();
        }

        long oneForthOfStep = (1L << 62) / totalWeight; // 62 vs 64 to normalize math in Long

        long floor = Long.MIN_VALUE;
        for (int i = 0; i != shards.size(); ++i) {
            final S shardInfo = shards.get(i);
            shardInfo.initResource();
            nodes.put(floor, shardInfo);
            floor += 4 * oneForthOfStep * shardInfo.getWeight(); // *4 to compensate 62 vs 64
        }
    }

    public R getShard(String key) {
        return nodes.floorEntry(algo.hash(key)).getValue().getResource();
    }

    public S getShardInfo(String key) {
        return nodes.floorEntry(algo.hash(key)).getValue();
    }

    public Collection<S> getAllShards() {
        return Collections.unmodifiableCollection(nodes.values());
    }
}