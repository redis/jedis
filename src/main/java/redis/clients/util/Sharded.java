package redis.clients.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sharded<R, S extends ShardInfo<R>> {

    public static final int DEFAULT_WEIGHT = 1;
    private TreeMap<Long, S> nodes;
    private final Hashing algo;
    private final Map<ShardInfo<R>, R> resources = new HashMap<ShardInfo<R>, R>();

    /**
     * The default pattern used for extracting a key tag. The pattern must have
     * a group (between parenthesis), which delimits the tag to be hashed. A
     * null pattern avoids applying the regular expression for each lookup,
     * improving performance a little bit is key tags aren't being used.
     */
    private Pattern tagPattern = null;
    // the tag is anything between {}
    public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
            .compile("\\{(.+?)\\}");

    public Sharded(List<S> shards) {
        this(shards, Hashing.MURMUR_HASH); // MD5 is really not good as we works
        // with 64-bits not 128
    }

    public Sharded(List<S> shards, Hashing algo) {
        this.algo = algo;
        initialize(shards);
    }

    public Sharded(List<S> shards, Pattern tagPattern) {
        this(shards, Hashing.MURMUR_HASH, tagPattern); // MD5 is really not good
        // as we works with
        // 64-bits not 128
    }

    public Sharded(List<S> shards, Hashing algo, Pattern tagPattern) {
        this.algo = algo;
        this.tagPattern = tagPattern;
        initialize(shards);
    }

    private void initialize(List<S> shards) {
        nodes = new TreeMap<Long, S>();

        int totalWeight = 0;

        for (ShardInfo<?> shard : shards) {
            totalWeight += shard.getWeight();
        }

        long oneForthOfStep = (1L << 62) / totalWeight; // 62 vs 64 to normalize
        // math in Long

        long floor = Long.MIN_VALUE;
        for (int i = 0; i != shards.size(); ++i) {
            final S shardInfo = shards.get(i);
            resources.put(shardInfo, shardInfo.createResource());
            nodes.put(floor, shardInfo);
            floor += 4 * oneForthOfStep * shardInfo.getWeight(); // *4 to
            // compensate
            // 62 vs 64
        }
    }

    public R getShard(byte[] key) {
        return resources.get(getShardInfo(key));
    }

    public R getShard(String key) {
        return resources.get(getShardInfo(key));
    }

    private S getShardInfo(byte[] key) {
        Iterator<Entry<Long, S>> iterator = nodes.headMap(algo.hash(key))
                .entrySet().iterator();
        Entry<Long, S> next = iterator.next();
        if (iterator.hasNext()) {
            next = iterator.next();
        }
        return next.getValue();
    }

    public S getShardInfo(String key) {
        return getShardInfo(SafeEncoder.encode(getKeyTag(key)));
    }

    /**
     * A key tag is a special pattern inside a key that, if preset, is the only
     * part of the key hashed in order to select the server for this key.
     * 
     * @see http://code.google.com/p/redis/wiki/FAQ#I
     *      'm_using_some_form_of_key_hashing_for_partitioning,_but_wh
     * @param key
     * @return The tag if it exists, or the original key
     */
    public String getKeyTag(String key) {
        if (tagPattern != null) {
            Matcher m = tagPattern.matcher(key);
            if (m.find())
                return m.group(1);
        }
        return key;
    }

    public Collection<S> getAllShardInfo() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<R> getAllShards() {
        return Collections.unmodifiableCollection(resources.values());
    }
}