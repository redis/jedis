import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisTimeout {

    public static void main(String[] args) {
	// set k,v pairs where k == v
	Jedis edis = new Jedis("127.0.0.1", 6379, 1000);
	for (int i = 0; i < 100000; i++) {
	    edis.set("" + i, "" + i);
	}
	edis.close();
	JedisPoolConfig conf = new JedisPoolConfig();
	conf.setMaxTotal(1);
	final JedisPool pool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379, 1);
	for (int t = 0; t < 100; t++) {
	    new Thread() {
		public void run() {
		    
		    final Jedis edis = pool.getResource();
								       // timeout
								       // = 1
								       // here.
		    for (int i = 0; i < 1000; i++) {
			try {
			    String key = "" + i;
			    String value = edis.get(key);
			    if (!key.equals(value)) {
				System.err.println(Thread.currentThread()
					.getName()
					+ "\t\t\t"
					+ key
					+ "<>"
					+ value);
			    }
			} catch (Exception e) {
			    System.err.println(">>>>>>>>>>" + e.toString());
			    e.printStackTrace();
			}
		    }
		    System.out.println("Done");
		    pool.returnResource(edis);
		};
	    }.start();
	}

    }

}