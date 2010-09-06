package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class JedisMultipleSourcesPool<T extends JedisDynamicPool> {
	private static class PoolWrapper<T> {
		private final T				pool;
		private volatile boolean	enable;

		public PoolWrapper(T pool) {
			this.pool = pool;
			enable = true;
		}

		public boolean enable() {
			return enable;
		}

		public T getPool() {
			return pool;
		}

		public boolean enable(boolean newval) {
			boolean val = enable;
			this.enable = newval;
			return val;
		}
	}

	static protected void silentSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	private class RepairPool extends Thread {
		private long	delay;

		public RepairPool(long delay) {
			this.delay = delay;
			this.setName("RepairPool");
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				for (int i = 0; i < sourceSize; i++) {
					PoolWrapper<T> pool = availbleGroups.get(i);
					if (pool.enable() == false) {
						try {
							Jedis res = pool.getPool().getResource();
							pool.getPool().returnResource(res);
							pool.enable(true);
						} catch (JedisException je) {
							handleException(je);
						}
					}
				}
				silentSleep(delay);
			}
		}

		private void handleException(JedisException je) {
			je.printStackTrace();
		}
	}

	final int					sourceSize;
	volatile int				idxCounter		= 0;
	final List<PoolWrapper<T>>	availbleGroups	= new ArrayList<PoolWrapper<T>>();
	private long				repairDelay;

	public JedisMultipleSourcesPool(List<T> pools, long repairDelay) {
		if (pools == null) {
			throw new NullPointerException("pools = null");
		}
		if (pools.size() == 0) {
			throw new IllegalArgumentException("pools should has at least one element");
		}
		for (T source : pools) {
			availbleGroups.add(new PoolWrapper<T>(source));
		}
		sourceSize = availbleGroups.size();
		this.repairDelay = repairDelay;
		new RepairPool(this.repairDelay).start();
	}

	/**
	 * @see findConnection
	 * @param timeout
	 * @return
	 * @throws TimeoutException
	 */
	public Jedis getConnection(long timeout) throws TimeoutException {
		Jedis res = findConnection(timeout);
		if (res == null) {
			throw new TimeoutException("Cannot obtain connection from any available pool");
		}
		return res;
	}

	/**
	 * Get connection from pool, much like
	 * {@link JedisMultipleSourcesPool.getConnection} but return null instead of
	 * throw exception if cannot obtain connection.
	 * 
	 * @param timeout
	 *            time to like to keep trying get connection if it is not
	 *            available
	 * @return connection object.
	 */
	public Jedis findConnection(long timeout) {
		long begin = System.currentTimeMillis();
		int counter = 0;
		do {
			int id = (idxCounter++) % sourceSize;
			PoolWrapper<T> pool = availbleGroups.get(id);
			if (pool.enable()) {
				try {
					Jedis res = pool.getPool().getResource();
					return res;
				} catch (JedisException je) {
					pool.enable(false);
				}
			}
			counter++;
			if (counter == sourceSize && timeout > 0) {
				silentSleep(1);
			}
		} while (System.currentTimeMillis() - begin < timeout);
		return null;
	}

	/**
	 * Try to return Jedis connection resource to a live pool, or to a dead pool
	 * if not available, if resource is returned to dead pool, that pool will be
	 * mark as active immediately.
	 * 
	 * @param resource
	 */
	public void returnConnection(Jedis resource) {
		int counter = 0;
		do {
			int id = (idxCounter++) % sourceSize;
			PoolWrapper<T> pool = availbleGroups.get(id);
			if (pool.enable()) {
				pool.getPool().returnResource(resource);
				break;
			}
			counter++;
			if (counter == sourceSize) {
				pool.getPool().returnResource(resource);
				pool.enable(true);
				break;
			}
		} while (true);
	}
}
