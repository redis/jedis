package redis.clients.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisServerInfo;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

public class FailSafeInvocationHandler implements InvocationHandler {
	private final Map<JedisServerInfo, Jedis> resources = new HashMap<JedisServerInfo, Jedis>();
	private final FailSafeJedisCluster cluster;

	public FailSafeInvocationHandler(FailSafeJedisCluster cluster) {
		this.cluster = cluster;
		for (JedisServerInfo info : cluster.getMembers()) {
			Jedis jedis = new Jedis(info.getHost(), info.getPort());
			if (info.getPassword() != null)
				jedis.auth(info.getPassword());
			resources.put(info, jedis);
		}
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		if (cluster.getMaster() == null)
			throw new JedisException("No Master is found");

		Jedis j = resources.get(cluster.getMaster());
		try {
			return method.invoke(j, args);
		} catch (InvocationTargetException ex) {
			if (ex.getCause() instanceof JedisConnectionException)
				j = resources.get(cluster.electNewMaster());
			else if (ex.getCause() instanceof JedisDataException
					&& ex.getCause().getMessage().startsWith("READONLY"))
				j = resources.get(cluster.electNewMaster());
			else
				throw ex.getCause();
		}
		return method.invoke(j, args);
	}

	public void destroyProxy() {
		try {
			for (Jedis j : resources.values()) {
				j.quit();
				j.disconnect();
			}
		} catch (Exception e) {
		}
	}
}
