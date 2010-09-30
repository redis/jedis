package redis.clients.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sharded<R, S extends ShardInfo<R>> {
    public static final int DEFAULT_WEIGHT = 1;
    private TreeMap<Long, S> nodes;
    private final Hashing algo;
    
    /** 
     * The default pattern used for extracting a key tag. 
     * The pattern must have a group (between parenthesis), which delimits the tag to be hashed.
     */
    private Pattern tagPattern = Pattern.compile("\\{(.+?)\\}"); // the tag is anything between {} 

    public Sharded(List<S> shards) {
        this(shards, Hashing.MURMUR_HASH); // MD5 is really not good as we works with 64-bits not 128
    }

    public Sharded(List<S> shards, Hashing algo) {
        this.algo = algo;
        initialize(shards);
    }

    public Sharded(List<S> shards, Pattern tagPattern) {
        this(shards, Hashing.MURMUR_HASH, tagPattern); // MD5 is really not good as we works with 64-bits not 128
    }

    public Sharded(List<S> shards, Hashing algo, Pattern tagPattern) {
        this.algo = algo;
        this.tagPattern = tagPattern; 
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
        return nodes.floorEntry(algo.hash(getKeyTag(key))).getValue().getResource();
    }

    public S getShardInfo(String key) {
        return nodes.floorEntry(algo.hash(getKeyTag(key))).getValue();
    }
    
    /**
     * A key tag is a special pattern inside a key that, if preset, is the only part of the key hashed 
     * in order to select the server for this key.
     * 
     * @see http://code.google.com/p/redis/wiki/FAQ#I'm_using_some_form_of_key_hashing_for_partitioning,_but_wh
     * @param key
     * @return The tag if it exists, or the original key 
     */
    public String getKeyTag(String key){
    	Matcher m = tagPattern.matcher(key);
    	if (m.find()) 
    		return m.group(1); 
    	return key; 
    }

    public Collection<S> getAllShards() {
        return Collections.unmodifiableCollection(nodes.values());
    }
}