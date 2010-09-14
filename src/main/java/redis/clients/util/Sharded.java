package redis.clients.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class Sharded<T> {
    public static final int DEFAULT_WEIGHT = 1;
    private static MessageDigest md5 = null; // avoid recurring construction
    private TreeMap<Long, ShardInfo> nodes;
    private int totalWeight;
    private Map<ShardInfo, T> resources;

    public Sharded(List<ShardInfo> shards) {
	initialize(shards);
    }

    private void initialize(List<ShardInfo> shards) {
	nodes = new TreeMap<Long, ShardInfo>();
	resources = new HashMap<ShardInfo, T>();

	totalWeight = 0;

	for (ShardInfo shard : shards) {
	    totalWeight += shard.getWeight();
	}

	MessageDigest md5;
	try {
	    md5 = MessageDigest.getInstance("MD5");
	} catch (NoSuchAlgorithmException e) {
	    throw new IllegalStateException("++++ no md5 algorythm found");
	}

	for (ShardInfo shard : shards) {
	    double factor = Math
		    .floor(((double) (40 * shards.size() * DEFAULT_WEIGHT))
			    / (double) totalWeight);

	    for (long j = 0; j < factor; j++) {
		byte[] d = md5.digest((shard.toString() + "-" + j).getBytes());
		for (int h = 0; h < 4; h++) {
		    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
			    | ((long) (d[2 + h * 4] & 0xFF) << 16)
			    | ((long) (d[1 + h * 4] & 0xFF) << 8)
			    | ((long) (d[0 + h * 4] & 0xFF));
		    nodes.put(k, shard);
		}
	    }
	    resources.put(shard, create(shard));
	}
    }

    public ShardInfo getShardInfo(String key) {
	long hv = calculateHash(key);

	return nodes.get(findPointFor(hv));
    }

    private Long calculateHash(String key) {
	if (md5 == null) {
	    try {
		md5 = MessageDigest.getInstance("MD5");
	    } catch (NoSuchAlgorithmException e) {
		throw new IllegalStateException("++++ no md5 algorythm found");
	    }
	}

	md5.reset();
	md5.update(key.getBytes());
	byte[] bKey = md5.digest();
	long res = ((long) (bKey[3] & 0xFF) << 24)
		| ((long) (bKey[2] & 0xFF) << 16)
		| ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
	return res;
    }

    private Long findPointFor(Long hashK) {
	Long k = nodes.ceilingKey(hashK);

	if (k == null) {
	    k = nodes.firstKey();
	}

	return k;
    }

    public T getShard(String key) {
	ShardInfo shard = getShardInfo(key);
	return resources.get(shard);
    }

    protected abstract T create(ShardInfo shard);

    public Collection<T> getAllShards() {
	return resources.values();
    }
}