package redis.clients.jedis;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;

/**
 * Using ConsistentHashing to supporting removing failed server and rehashing
 * 
 * @author briangxchen@gmail.com
 * 
 * 
 */
public class ConsistentJedisPool {
	private ConsistentHash<String> consistentHash;
	private Map<String, JedisPool> poolMap = new HashMap<String, JedisPool>();

	private List<String> addresses;
	private Hashing hashAlg;
	private int numberOfReplicas;
	private JedisPoolConfig config;

	public ConsistentJedisPool(List<String> addresses, int numberOfReplicas,
			JedisPoolConfig config, Hashing hashAlg) {
		this.addresses = addresses;
		this.numberOfReplicas = numberOfReplicas;
		this.config = config;
		this.hashAlg = hashAlg;

		init();

	}

	public void destroy() {
		try {

			for (JedisPool pool : poolMap.values()) {
				pool.destroy();
			}

		} catch (Exception e) {
			throw new JedisException("Could not destroy the pool", e);
		}
	}

	private void init() {
		consistentHash = new ConsistentHash<String>(hashAlg, numberOfReplicas,
				addresses);

		for (String addr : addresses) {
			JedisPool pool = new JedisPool(config, addr.split(":")[0],
					Integer.parseInt(addr.split(":")[1]));
			poolMap.put(addr, pool);
		}

	}

	private Jedis safeRes(JedisPool pool) {
		Jedis jedis = null;

		try {
			jedis = pool.getResource();
		} catch (Exception e) {
			// Do nothing
		}

		return jedis;
	}

	private TreeMap<String, Jedis> getResource(String key) {
		String addr = consistentHash.get(key);
		TreeMap<String, Jedis> resRun = new TreeMap<String, Jedis>();
		Jedis jedis = safeRes(poolMap.get(addr));

		if (null == jedis) {
			int retry = 0;
			SortedMap<Long, String> oCircle = consistentHash.getCircle();
			TreeMap<Long, String> rCircle = new TreeMap<Long, String>(oCircle);

			while (null == jedis && !rCircle.isEmpty()) {
				for (Iterator<Map.Entry<Long, String>> it = rCircle.entrySet()
						.iterator(); it.hasNext();) {
					Map.Entry<Long, String> entry = it.next();
					if (addr.equals(entry.getValue())) {
						it.remove();
					}
				}

				addr = consistentHash.get(retry + key, rCircle);
				jedis = safeRes(poolMap.get(addr));
				retry++;

				// log.debug("${retry} Try ${addr}");
			}
		}

		resRun.put(addr, jedis);
		return resRun;
	}

	public Object redisCall(RedisCallback callback) throws Throwable {
		Jedis jedis = null;
		JedisPool pool = null;
		try {
			TreeMap<String, Jedis> resRun = getResource(callback.getKey());
			jedis = resRun.firstEntry().getValue();
			String key = resRun.firstKey();
			pool = poolMap.get(key);

			Object ret = callback.doInRedis(jedis);
			return ret;
		} catch (Throwable e) {
			// log.error("Failed to call redis",e);
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
			}
			throw e;
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
	}
}
