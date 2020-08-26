package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.util.SafeEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to manage the client side caching service
 *  This class starts a new thread to listen to invalitation event RESP2
 */
public class RedisClientCache {

    protected static Logger log = LoggerFactory.getLogger(RedisClientCache.class.getName());

    public final static String INVALIDATION_CHANNEL = "__redis__:invalidate";

    private Map<String, Object> simpleCache = new HashMap();

    // TODO : need to check what is the best way to get it from pool/configuration
    BinaryJedis jedis = null; // the connection to Redis
    Long clientId = null;

    /**
     * Create a new client cache
     * For RESP2, the application must create a new connection on the same instances
     * to get the Pub/Sub on invalidate channel
     * @param client
     */
    public RedisClientCache(BinaryJedis jedis) {
        this.jedis = jedis;
        clientId = jedis.clientId();
        this.startInvalidationListener();
    }

    public Long getRedisCacheClientId(){
        if (jedis != null && clientId == null) {
            clientId = jedis.clientId();
        }
        return clientId;
    }

    /**
     * RESP2 Create a new thread that will listen to invalidation event
     *
     */
    private void startInvalidationListener() {
        new Thread(new Runnable() {

            @Override
            public void run() {

                jedis.subscribe(new BinaryJedisPubSub() {



                    @Override
                    public void onMessage(byte[] channel, List<byte[]> messages) {
                        log.info("Invalidate cache value for {} ", SafeEncoder.encode(messages));
                        // iterate on each and remove it from the cache
                        for (byte[] key : messages) {
                            simpleCache.remove(SafeEncoder.encode(key)); // TODO : cleanup
                        }
                    }

                },  SafeEncoder.encode(INVALIDATION_CHANNEL));

            }
        }, "ClientCacheThread-"+ clientId).start();


    }

    public void put(String key, Object value) {
        simpleCache.put(key, value);
    }

    public Object get(String key) {
        return simpleCache.get(key);
    }

}
