package redis.clients.jedis;

import org.junit.Test;

public class ClientCacheTest {

    @Test
    public void testClientCache() throws InterruptedException {

        Jedis jedis = new Jedis("127.0.0.1", 6379);

        new Thread(()->{
            jedis.hello3();
            jedis.clientTrackingOnBCast(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println(channel);
                    System.out.println("invalidate key:" + message);
                }
            });
        }).start();

        Thread.currentThread().join();
    }
}
