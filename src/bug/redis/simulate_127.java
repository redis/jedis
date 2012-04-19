package bug.redis;

import java.util.HashMap;
import redis.clients.jedis.*;

class simulate_127 {

	public static void main(String args[])
	{
		JedisPool MyPool = new JedisPool("home-host", 6379);
		
		Jedis myDb = MyPool.getResource();

		Pipeline p = myDb.pipelined();

		HashMap<String, Integer> mydata = new HashMap<String, Integer>();

		mydata.put("test1", 1);
		mydata.put("test2", 1);
		mydata.put("test3", 1);
		mydata.put("test4", 1);
		mydata.put("test5", 1);

	    for (String txtfield : mydata.keySet())
		{
			p.zadd("somekey", mydata.get(txtfield), txtfield );
			p.zincrby("SUPERUNION", mydata.get(txtfield), txtfield );
			p.sadd("setkey", "setstr1");
			p.sadd("setkey", "setstr2");
			p.sadd("setkey", "setstr1");
		}
		p.sync();

		MyPool.returnResource(myDb);
	}
}
