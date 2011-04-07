package redis.clients.jedis;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.util.Pool;

public class JedisPool extends Pool<Jedis> {

	public JedisPool(final GenericObjectPool.Config poolConfig,
            final String host) {
        this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null);
    }

    public JedisPool(String host, int port) {
        this(new Config(), host, port);
    }
    
    public JedisPool(final Config poolConfig, final String host, final int port) {
        this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT);
    }

    public JedisPool(final Config poolConfig, final String host, final int port, 
    		final int timeout) {
        this(poolConfig, host, port, timeout, null);
    }
    
    public JedisPool(final Config poolConfig, final String host, int port,
            int timeout, final String password) {
    	this(poolConfig, new JedisShardInfo(host, port, password, timeout));
    }
    
    public JedisPool(final Config poolConfig, JedisShardInfo shardInfo) {
    	super(poolConfig, new JedisFactory(shardInfo));
    }
    
    @Override
    public void destroy() {
    	super.destroy();
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    private static class JedisFactory extends BasePoolableObjectFactory {

    	private final JedisShardInfo shardInfo;

        public JedisFactory(JedisShardInfo shardInfo){
        	super();
        	this.shardInfo = shardInfo;
        }

        public Object makeObject() throws Exception {
            return shardInfo.createResource();
        }

        public void destroyObject(final Object obj) throws Exception {
            if (obj instanceof Jedis) {
                final Jedis jedis = (Jedis) obj;
                if (jedis.isConnected()) {
                    try {
                        try {
                            jedis.quit();
                        } catch (Exception e) {
                        }
                        jedis.disconnect();
                    } catch (Exception e) {

                    }
                }
            }
        }

        public boolean validateObject(final Object obj) {
            if (obj instanceof Jedis) {
                final Jedis jedis = (Jedis) obj;
                try {
                    return jedis.isConnected() && jedis.ping().equals("PONG");
                } catch (final Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }

    }
}
