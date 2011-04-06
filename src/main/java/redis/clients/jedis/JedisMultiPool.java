package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;

public class JedisMultiPool { 
	
	public static final int DEFAULT_WEIGHT = 1;
    
    // the tag is anything between {}
    public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern.compile("\\{(.+?)\\}");
	
	/**
     * The default pattern used for extracting a key tag. The pattern must have
     * a group (between parenthesis), which delimits the tag to be hashed. A
     * null pattern avoids applying the regular expression for each lookup,
     * improving performance a little bit is key tags aren't being used.
     */
    private Pattern tagPattern = null;

    private final Hashing algo;
    
    private List<JedisShardInfo> shardsInfo;
    
    private TreeMap<Long, JedisShardInfo> nodes;
	
	private final Map<JedisShardInfo, JedisPool> resources = new LinkedHashMap<JedisShardInfo, JedisPool>();
	
    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards) {
        this(poolConfig, shards, Hashing.MURMUR_HASH);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo) {
        this(poolConfig, shards, algo, null);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Pattern keyTagPattern) {
        this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
    	this.algo = algo;
        this.tagPattern = keyTagPattern;

        nodes = new TreeMap<Long, JedisShardInfo>();
        shardsInfo = new ArrayList<JedisShardInfo>(shards);

        for (int i = 0; i != shards.size(); ++i) {
            final JedisShardInfo shardInfo = shards.get(i);
            for (int n = 0; n < 160 * shardInfo.getWeight(); n++) {
                nodes.put(this.algo.hash(shardInfo.toString() + n), shardInfo);
            }
            resources.put(shardInfo, new JedisPool(poolConfig, shardInfo));
        }
    }
    
    public Pool<Jedis> getPool(JedisShardInfo shardInfo) {
    	return resources.get(shardInfo);
    }
    
    public Pool<Jedis> getPool(byte[] key) {
    	return getPool(getShardInfo(key));
    }
    
    public Pool<Jedis> getPool(String key) {
    	return getPool(getShardInfo(key));
    }

    public void destroy() {
        Exception lastException = null;
    	for(JedisPool pool: resources.values()){
    		try {
    			pool.destroy();
    		} catch (Exception e) {
    			lastException = e;
    		}
    	}
    	
    	if(lastException != null){
            throw new JedisException("Could not destroy some pools", lastException);
    	}
    }
    
    public JedisShardInfo getShardInfo(byte[] key) {
        SortedMap<Long, JedisShardInfo> tail = nodes.tailMap(algo.hash(key));
        if (tail.size() == 0) {
            return nodes.get(nodes.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    public JedisShardInfo getShardInfo(String key) {
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

    public Collection<JedisShardInfo> getAllShardInfo() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<JedisShardInfo> getShards() {
        return Collections.unmodifiableCollection(shardsInfo);
    }
 
}