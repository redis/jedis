package redis.clients.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Allow shards custom providers, inspired from {@linkplain Observable}.
 */
public abstract class AbstractDynamicShardsProvider<R, S extends ShardInfo<R>> {
	private final ArrayList<Sharded<R, S>> shardeds;
	private final List<S> shards;
	private boolean changed = false;

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
	}

	/**
	 * Gets all the shards currently available and useable.
	 * @return all the shards currently available and useable.
	 */
	public List<S> getShards() {
		synchronized (shards) {
			return shards;
		}
	}
	
	public void setShards(final List<S> shards) {
		synchronized (this.shards) {
			if(null != shards) {
				if(this.shards.size() == shards.size() && this.shards.containsAll(shards)) {
					// Nothing has changed
					return;
				} else {
					this.shards.clear();
					this.shards.addAll(shards);
					this.changed = true;
				}
			} else {
				this.shards.clear();
				this.changed = true;
			}
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
