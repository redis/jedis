package redis.clients.jedis;

/**
 * 
 * @author briangxchen@gmail.com
 * 
 * 
 */

public interface RedisCallback<T> {

	T doInRedis(Jedis jedis);

	String getKey();

}