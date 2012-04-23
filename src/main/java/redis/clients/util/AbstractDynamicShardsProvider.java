package redis.clients.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Allow shards custom providers, inspired from {@linkplain Observable}.
 */
public abstract class AbstractDynamicShardsProvider<R, S extends ShardInfo<R>> {
	private final ArrayList<Sharded<R, S>> shardeds;
	private final List<S> shards;
	private boolean changed = false;
	private final Lock readLock;
	private final Lock writeLock;

	/**
	 * Default constructor that initialize an empty list of shards / sharded.
	 */
	public AbstractDynamicShardsProvider() {
		this(null);
	}

	/**
	 * Default constructor with initial shards list.
	 * @param initialShards initial shards list
	 */
	public AbstractDynamicShardsProvider(final List<S> initialShards) {
		super();
		if(null != initialShards && 0 != initialShards.size()) {
			this.shards = new ArrayList<S>(initialShards.size());
			this.shards.addAll(initialShards);
		} else {
			this.shards = new ArrayList<S>(0);
		}
		// We're not expecting a lot of dynamic Sharded waiting for updates ...
		// So the initial size for those is set quite low
		this.shardeds = new ArrayList<Sharded<R,S>>(3);
		
		// Setup the locks
		ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();
	}

	/**
	 * Gets all the shards currently available and useable.
	 * @return all the shards currently available and useable.
	 */
	public List<S> getShards() {
		readLock.lock();
		try {
			return shards;
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Sets a fresh list of shards to be used.
	 * <br/> A null or empty list of shards will NOT thow any error,
	 * The available shards will just be set to an empty list.
	 * @param shards the fresh list of shards to be used.
	 * 
	 */
	public void setShards(final List<S> shards) {
		final int currentSize;

		readLock.lock();
		try {
			currentSize = this.shards.size();
			// Special use case :
			// The current shards list & the new one contains the same shards
			// ==> do nothing
			if(null != shards) {
				if(currentSize == shards.size() && this.shards.containsAll(shards)) {
					return;
				}
			}
		} finally {
			readLock.unlock();
		}
	
		// Special use case :
		// - current shards list not empty
		// - new shards list null or empty
		if(0 != currentSize && (null == shards || 0 == shards.size())) {
			clearShardsList();
		}
	
		// Default use case, the shards are not the same ...
		if(null != shards) {
			setShardsList(shards);
		}

		notifyShardeds();
	}
	
	private void clearShardsList() {
		writeLock.lock();
		try {
			this.shards.clear();
			this.changed = true;
		} finally {
			writeLock.unlock();
		}
	}
	
	private void setShardsList(final List<S> shardsList) {
		writeLock.lock();
		try {
			this.shards.clear();
			this.shards.addAll(shardsList);
			this.changed = true;
		} finally {
			writeLock.unlock();
		}
	}
	
	public void addShard(final S newShard) {
		if(null == newShard) {
			return;
		}
		
		// Do nothing if the new shard is already present ...
		readLock.lock();
		try {
			if(this.shards.contains(newShard)) {
				return;
			}
		}finally {
			readLock.unlock();
		}
		
		// Add the new shard ...
		writeLock.lock();
		try {
			this.shards.add(newShard);
			this.changed = true;
		} finally {
			writeLock.unlock();
		}
		
		notifyShardeds();
	}

	public void removeShard(final S oldShard) {
		if(null == oldShard) {
			return;
		}

		// Do nothing if the new shard is not present ...
		readLock.lock();
		try {
			if(!this.shards.contains(oldShard)) {
				return;
			}
		}finally {
			readLock.unlock();
		}
		
		// Remove the shard ...
		writeLock.lock();
		try {
			this.shards.remove(oldShard);
			this.changed = true;
		} finally {
			writeLock.unlock();
		}
		
		notifyShardeds();
	}

	/**
	 * Register a Sharded to be notified when the shards are updated.
	 * @param sharded a Sharded to be notified when the shards are updated.
	 */
	public synchronized void register(final Sharded<R, S> sharded) {
		if(null != sharded) {
			if(!shardeds.contains(sharded)) {
				shardeds.add(sharded);
			}
		}
	}
	
	/**
	 * Unregister a previously registered Sharded that is no more interested in shards updates.
	 * @param sharded a previously registered Sharded that is no more interested in shards updates.
	 */
	public synchronized void unregister(final Sharded<R, S> sharded) {
		if(null != sharded) {
			shardeds.remove(sharded);
		}
	}

	/**
	 * Unregister all the previously registered Sharded.
	 */
	public synchronized void unregisterAll() {
		shardeds.clear();
	}
	
	/**
	 * Notify the registered Shardeds that the shards have been updated.
	 */
	@SuppressWarnings("unchecked")
	public void notifyShardeds() {
		Object[] arrLocal;
		synchronized (this) {
		    if (!changed) {
	                return;
		    } else {
	            arrLocal = shardeds.toArray();
	            changed = false;
	        }
	
	        for (int i = arrLocal.length-1; i>=0; i--) {
	            ((Sharded<R, S>)arrLocal[i]).dynamicUpdate(this);
	        }
		}
	}
}
