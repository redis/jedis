package redis.clients.jedis;

/**
 * 
 * @author briangxchen@gmail.com
 * 
 * 
 */

public interface RedisCallback {

	Object doInRedis(Jedis jedis);

	String getKey();

}