package redis.clients.util;

import java.lang.reflect.Proxy;
import org.apache.commons.pool.BasePoolableObjectFactory;

import redis.clients.jedis.JedisFacade;

public class FailSafeJedisFactory extends BasePoolableObjectFactory {
	private final FailSafeJedisCluster cluster;

	public FailSafeJedisFactory(FailSafeJedisCluster cluster) {
		this.cluster = cluster;
	}

	public Object makeObject() throws Exception {
		FailSafeInvocationHandler handler = new FailSafeInvocationHandler(
				cluster);

		Object proxy = Proxy.newProxyInstance(
				JedisFacade.class.getClassLoader(),
				new Class[] { JedisFacade.class }, handler);

		return proxy;
	}

	public void destroyObject(final Object obj) throws Exception {
		FailSafeInvocationHandler handler = (FailSafeInvocationHandler) Proxy
				.getInvocationHandler(obj);
		handler.destroyProxy();
	}

	public boolean validateObject(final Object obj) {
		JedisFacade jedis = (JedisFacade) obj;
		try {
			return "OK".equals(jedis.set("foo", "bar"));
		} catch (Exception ex) {
		}
		return false;
	}
}
