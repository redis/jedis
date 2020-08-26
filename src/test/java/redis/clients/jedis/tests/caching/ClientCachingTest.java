package redis.clients.jedis.tests.caching;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.ClientTrackingParams;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientCachingTest extends JedisCommandTestBase {

    //@Test
    public void clientTracking(){

        //jedis = new Jedis("127.0.0.1", 8888);
        System.out.println("Data Client "+ jedis.clientId());

        Jedis clientSideCachingConnection  = this.createJedis();

        Map<String,String> fooHash = new HashMap<>();
        fooHash.put("f1", "value-1-00");
        fooHash.put("f2", "value-2-00");

        jedis.set("foo", "value-00");
        jedis.hset("fooHash", fooHash);

        System.out.println("foo : "+ jedis.get("foo"));
        System.out.println("fooHash : "+ jedis.hgetAll("fooHash"));


        jedis.clientTracking(true, clientSideCachingConnection,  null);


        boolean loop = true;
        int counter = 0;


        while (loop) {

            System.out.println(jedis.get("foo")  + " -- "+ jedis.clientId());
            System.out.println("fooHash : "+ jedis.hgetAll("fooHash")+ " -- "+ jedis.clientId());

            if ((counter % 5) == 0) {
                System.out.println("2 loop");
                jedis.hset("fooHash", "f3", Integer.toString(counter) );
            }


            try {
                Thread.sleep(3000);
                counter++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }



    }


}
