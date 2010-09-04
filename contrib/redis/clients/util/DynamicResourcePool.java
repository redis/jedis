package redis.clients.util;

import java.util.concurrent.LinkedBlockingDeque;

import redis.clients.jedis.JedisException;

/**
 * @author secmask@gmail.com
 *
 * @param <T> Type of resource
 */
public abstract class DynamicResourcePool<T> {
	private static class Wrapper<T> {
		long	timestamp;
		T		wrapped;

		public Wrapper(T wrapped) {
			this.wrapped = wrapped;
			mark();
		}

		public void mark() {
			timestamp = System.currentTimeMillis();
		}

		public long getLastMark() {
			return timestamp;
		}
	}

	private class CleanupDaemon extends Thread {
		private long	maxIdle;
		private long	delay;

		public CleanupDaemon(long maxIdle, long delayPeRound) {
			this.setDaemon(true);
			this.maxIdle = maxIdle;
			this.delay = delayPeRound;
			this.setName("DynamicPoolCleaner");
		}

		@Override
		public void run() {
			// this a daemon thread, so system will terminated it automatically
			// when need.
			while (true) {
				// maybe availableQueue size change later, but this should
				// enough.
				while (true) {
					// the oldest resource will be at the tail of queue
					// the most active will be at the head, so we only need to
					// check at the tail.
					Wrapper<T> wa = availableQueue.pollLast();
					if (wa == null) {
						break;
					}
					if (((System.currentTimeMillis() - wa.getLastMark()) > maxIdle)
							|| (!isResourceValid(wa.wrapped))) {
						// destroy resource if it reach maxIdle or is invalid.
						destroyResource(wa.wrapped);
					} else {
						if (!availableQueue.offerLast(wa)) {
							System.err.println("cannot resource offer back to available pool");
							destroyResource(wa.wrapped);
						}
						break;
					}
				}
				silentSleep(delay);
			}
		}
	}

	static protected void silentSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	static protected void handleException(Exception e) {
		e.printStackTrace();
	}

	private LinkedBlockingDeque<Wrapper<T>>	availableQueue;
	final long								maxIdle;
	final long								delayPerRound;
	volatile long							resourcesCreated	= 0;
	volatile long							resourcesReturned	= 0;

	public DynamicResourcePool(long maxIdle, long delayCheck) {
		availableQueue = new LinkedBlockingDeque<Wrapper<T>>();
		this.maxIdle = maxIdle;
		this.delayPerRound = delayCheck;
		new CleanupDaemon(this.maxIdle, this.delayPerRound).start();
	}

	/**
	 * Return a resource to the pool. When no longer needed.
	 * 
	 * @param resource
	 */
	public void returnResource(T resource) {
		if (resource == null)
			throw new IllegalArgumentException("The resource shouldn't be null.");
		if (isResourceValid(resource)) {
			if (!availableQueue.offerFirst(new Wrapper<T>(resource))) {
				// cannot push back to available pool, destroy resource.
				destroyResource(resource);
				throw new IllegalStateException(
						"This shouldn't happen. Offering to available queue rejected.");
			}
			resourcesReturned++;
		} else {
			destroyResource(resource);
		}
	}

	/**
	 * clean resource if able.
	 * 
	 * @param resource
	 */
	public void returnBrokenResource(T resource) {
		destroyResource(resource);
		resourcesReturned++;
	}

	/**
	 * Get a resource from the pool if there's a available, create new one if
	 * need.
	 * 
	 * @return Resource object.
	 * @throws Exception
	 *             the caller should handle Exception that throw by
	 *             createResource();
	 */
	public T getResource() throws JedisException {
		Wrapper<T> ret = availableQueue.poll();
		if (ret != null) {
			return ret.wrapped;
		}
		T res = createResource();
		resourcesCreated++;
		return res;
	}

	/*
	 * Implementation dependent methods. To be implemented.
	 */

	/**
	 * Create a resource for the pool
	 */
	protected abstract T createResource();

	/**
	 * Check if the resource is still valid.
	 * 
	 * @param resource
	 * @return
	 */
	protected abstract boolean isResourceValid(T resource);

	/**
	 * Destroy a resource.
	 * 
	 * @param resource
	 */
	protected abstract void destroyResource(T resource);

	/**
	 * Coming features: TODO Pool destruction. Down resources/threads and wait.
	 * TODO Busy time check. Cron to check when a resource is being taken for a
	 * long time. TODO Validation of long time idle objects
	 */

}
