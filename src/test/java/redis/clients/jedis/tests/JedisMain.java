package redis.clients.jedis.tests;

import redis.clients.jedis.Jedis;

public class JedisMain {
	public static void main(String[] args) {
		
		Jedis jedis = new Jedis("localhost");
		jedis.set("foo", "bar");
		jedis.hset("hSet1", "f1", "ha");
		
		Object result = jedis.sendGenericCmdList("KEYS","*");
		System.out.println("Result Generic Command[keys]: " + result);
		
		result = jedis.sendGenericCmdString("GET","foo");
		System.out.println("Result Generic Command[GET]: " + result);
		
		result = jedis.sendGenericCmdInteger("INCR","X");
		System.out.println("Result Generic Command[INCR]: " + result);
		
		result = jedis.sendGenericCmdString("INFO");
		System.out.println("Result Generic Command[INFO]: " + result);
		
		jedis.close();
	}
}
