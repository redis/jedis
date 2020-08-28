package redis.clients.jedis.tests.caching;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.caching.ClientSideCaching;
import redis.clients.jedis.params.ClientTrackingParams;

import java.util.HashMap;
import java.util.Map;

public class ClientSideCachingTest {

    //@Test
    public void simpleCacheTest(){


        System.out.println("START");

        JedisPool pool = new JedisPool("localhost", 8888);


        try (Jedis jedis = pool.getResource()) {

            ClientTrackingParams params = ClientTrackingParams.clientTrackingParams()
                   // .prefix("foo")
                   // .noloop()
                    ;

            ClientSideCaching csc = new ClientSideCaching(true, jedis, pool, params);

            csc.set("foo", "bar");
            csc.set("demo", "valueDemo");

            Map<String, String> values = new HashMap<>();
            values.put("f1", "v1");
            csc.hset("hash-test", values);


            System.out.println("foo "+ csc.get("foo"));
            System.out.println("demo "+ csc.get("demo"));
            System.out.println(csc.hgetAll("hash-test"));


            boolean loop = true;
            int counter = 0;

            while(loop) {
                System.out.println("foo "+ csc.get("foo"));
                //System.out.println("demo "+ csc.get("demo"));

                Thread.sleep(2500);

                counter++;

                if ((counter % 5) == 0) {
                    csc.set( "foo", "loop "+ counter );
                }

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("END");


    }

}
