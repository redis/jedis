package redis.clients.jedis.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class RedisClientSideCache {

    protected static Logger log = LoggerFactory.getLogger(RedisClientSideCache.class.getName());

    // Use a simple Map as a cache
    // TODO: work on a stronger and pluggable solution if necessary
    private Map<String, Object> simpleClientCache = new ConcurrentHashMap<>();

    // in RESP2 the invalidation of the cache is done using Pub/Sub approach
    // a specific connection is necessary for this
    // this means that we have 2 connections when working with RESP2:
    //  * jedis "data connection" that is use to manipulate the data, that would be in the main ClientSideCaching class
    //  * jedis "invalidation connection", this private member
    private Jedis invalidationJedis = null;

    private Long invalidationClientId = null;

    public final static String INVALIDATION_CHANNEL = "__redis__:invalidate";

    public RedisClientSideCache(Jedis jedis) {
        this.invalidationJedis = jedis;
        this.invalidationClientId = invalidationJedis.clientId();

        this.startInvalidationListenerThread();
    }

    public Long getClientId() {
        return invalidationClientId;
    }

    public Object getValueFromCache(String key) {
        return simpleClientCache.get(key);
    }

    public Object putValueInCache(String key, Object value) {
        return simpleClientCache.put(key,value);
    }

    public void removeKeyFromCache(String key) {
        simpleClientCache.remove(key);
    }

    /**
     * Start a new thread to listen for invalidation event
     */
    private void startInvalidationListenerThread() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                invalidationJedis.subscribe(new JedisPubSub() {

                    @Override
                    public void onMessage(String channel, List<String> messages) {
                        log.info("Invalidate cache value for {} ", messages);
                        // iterate on each and remove it from the cache
                        for (String key : messages) {
                            simpleClientCache.remove(key);
                        }
                    }

                },  INVALIDATION_CHANNEL);

            }
        }, "ClientCacheThread").start();

    }
}
