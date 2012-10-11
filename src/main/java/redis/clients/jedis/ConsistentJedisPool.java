package redis.clients.jedis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import redis.clients.util.Hashing;

/**
 * @author briangxchen@gmail.com
 *
 */
public class ConsistentJedisPool {
	private ConsistentHash<String> consistentHash;
	private Map<String, JedisPool> poolMap;

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

	public Object redisCall(String action, Object[] args) throws Throwable {
		Jedis jedis = null;
		JedisPool pool = null;
		try {
			TreeMap<String, Jedis> resRun = getResource((String) args[0]);
			jedis = resRun.firstEntry().getValue();
			String key = resRun.firstKey();
			pool = poolMap.get(key);

			Object ret = invoke(jedis, "${action}", args);
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

	private Object invoke(Object obj, String method, Object[] args)
			throws Throwable {
		Class<?>[] argClasses = null;
		Object[] argValues = null;

		if (args != null) {
			argValues = args;
			argClasses = new Class[argValues.length];

			for (int i = 0; i < argValues.length; ++i) {
				if (null == argValues[i]) {
					throw new Exception("Found null paramaters!!!");
				}

				argClasses[i] = argValues[i].getClass();
			}
		}

		return execute(obj, method, argClasses, argValues);
	}

	private final Object execute(Object obj, final String methodName,
			final Class<?>[] argClasses, final Object[] argValues)
			throws Throwable {
		Method method = Jedis.class.getDeclaredMethod(methodName, argClasses);

		if (null == method) {
			throw new Exception("The method cannot be found: " + methodName);
		}

		try {
			return method.invoke(obj, argValues);
		} catch (InvocationTargetException it_e) {
			throw it_e.getTargetException();
		}
	}

	public void redisSetEx(String key, int seconds, String value)
			throws Throwable {
		redisCall("setex", new Object[] { key, seconds, value });
	}

	public Object redisGet(String key) throws Throwable {
		return redisCall("get", new Object[] { key });
	}

	public void redisSet(String key, String value) throws Throwable {
		redisCall("set", new Object[] { key, value });
	}

}
