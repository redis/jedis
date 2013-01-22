package redis.clients.util;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool.BasePoolableObjectFactory;

import redis.clients.jedis.JedisFacade;

public class FailSafeJedisFactory extends BasePoolableObjectFactory {
	private final Map<Object, FailSafeInvocationHandler> resources;
	private final FailSafeJedisCluster cluster;

	public FailSafeJedisFactory(FailSafeJedisCluster cluster) {
		this.cluster = cluster;
		this.resources = new ConcurrentHashMap<Object, FailSafeInvocationHandler>();
	}

	public Object makeObject() throws Exception {
		FailSafeInvocationHandler handler = new FailSafeInvocationHandler(
				cluster);

		Object proxy = Proxy.newProxyInstance(
				JedisFacade.class.getClassLoader(),
				new Class[] { JedisFacade.class }, handler);

		resources.put(proxy, handler);

		return proxy;
	}

	public void destroyObject(final Object obj) throws Exception {
		if (obj != null) {
			FailSafeInvocationHandler handler = resources.get(obj);
			if (handler != null) {
				handler.destroyProxy();
				resources.remove(obj);
			}
		}
	}

	public boolean validateObject(final Object obj) {
		if (obj != null) {
			FailSafeInvocationHandler handler = resources.get(obj);
			if (handler != null) {
				return handler.validateProxy();
			}
		}
		return false;
	}
}
