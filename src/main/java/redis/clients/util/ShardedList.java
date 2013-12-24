package redis.clients.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ShardedList<R, S extends ShardInfo<R>> implements Sharding<R, S> {

    private List<S> nodes;
    private final Hashing algo;
    private final Map<S, R> resources = new LinkedHashMap<S, R>();

    public ShardedList(List<S> nodes, Hashing algo) {
        this.nodes = new ArrayList<S>(nodes);
        this.algo = algo;
        for (S s: this.nodes) {
            if (resources.containsKey(s) == false) {
                resources.put(s, s.createResource());
            }
        }
    }

    public String getKeyTag(String key) {
        return key;
    }

    public R getShard(byte[] key) {
        return resources.get(getShardInfo(key));
    }

    public R getShard(String key) {
        return resources.get(getShardInfo(key));
    }

    public S getShardInfo(byte[] key) {
        return nodes.get((int)(algo.hash(key) % nodes.size()));
    }

    public S getShardInfo(String key) {
        return getShardInfo(SafeEncoder.encode(key));
    }

    public Collection<S> getAllShardInfo() {
        return Collections.unmodifiableCollection(nodes);
    }

    public Collection<R> getAllShards() {
        return Collections.unmodifiableCollection(resources.values());
    }
}
