package redis.clients.jedis;


public interface JedisClusterConnectionHandler {
	
	Jedis getConnection();

}
