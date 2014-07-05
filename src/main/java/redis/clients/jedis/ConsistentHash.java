package redis.clients.jedis;

/**
 * This class comes form Tom white's article
 * http://weblogs.java.net/blog/tomwhite/archive/2007/11/consistent_hash.html
 * 
 * @author Tom White
 * Modified by briangxchen@gmail.com
 * Support in runtime to remove the failed server from circle and rehash
 *
 */
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import redis.clients.util.Hashing;

public class ConsistentHash<T> {

	private final Hashing hashFunction;
	private final int numberOfReplicas;
	private final TreeMap<Long, T> circle = new TreeMap<Long, T>();

	public SortedMap<Long, T> getCircle() {
		return circle;
	}

	public ConsistentHash(Hashing hashFunction, int numberOfReplicas,
			Collection<T> nodes) {
		this.hashFunction = hashFunction;
		this.numberOfReplicas = numberOfReplicas;

		for (T node : nodes) {
			add(node);
		}
	}

	public void add(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.put(hashFunction.hash(node.toString() + i), node);
		}
	}

	public void remove(T node) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.remove(hashFunction.hash(node.toString() + i));
		}
	}
	
	public T get(String key) {
		return get(key, circle);
	}

	public T get(String key, TreeMap<Long, T> rCircle) {
		if (rCircle.isEmpty()) {
			return null;
		}
		long hash = hashFunction.hash(key);
		if (!rCircle.containsKey(hash)) {
			SortedMap<Long, T> tailMap = rCircle.tailMap(hash);
			hash = tailMap.isEmpty() ? rCircle.firstKey() : tailMap.firstKey();
		}
		return circle.get(hash);
	}

}