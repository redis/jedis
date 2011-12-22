import java.io.IOException;

import redis.clients.jedis.unix.UnixDomainJedis;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		UnixDomainJedis connection = new UnixDomainJedis("/var/run/redis/redis.sock");
		connection.flushAll();
		System.out.println(connection.set("A", "HI!"));
		System.out.println(connection.get("A"));
	}

}
