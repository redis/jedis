package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.util.JedisURIHelper;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

public class ProxyJedisTest {



    @Test
    public  void  proxyTest() throws URISyntaxException {


        URI jedisUri=new URI("redis://:redispassword@10.10.x.x:6379?proxy=socks5://user:password@proxyhost:1080");
        Jedis jedis = new Jedis(jedisUri);
        String rst= jedis.set("__test_set_over_proxy","aha");
        System.out.println(rst);

    }

}
