package redis.clients.jedis;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.util.Pool;

public class JedisPool extends Pool<Jedis> {

    public JedisPool(final Config poolConfig, final String host) {
        this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(String host, int port) {
        this(new Config(), host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final String host) {
        this(host, Protocol.DEFAULT_PORT);
    }

    public JedisPool(final Config poolConfig, final String host, int port,
            int timeout, final String password) {
        this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, final int port) {
        this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, final int port, final int timeout) {
        this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, int port, int timeout, final String password,
                     final int database) {
        super(poolConfig, new JedisFactory(host, port, timeout, password, database));
    }


    public void returnBrokenResource(final BinaryJedis resource) {
    	returnBrokenResourceObject(resource);
    }
    
    public void returnResource(final BinaryJedis resource) {
    	returnResourceObject(resource);
    }
    
    /**
     * PoolableObjectFactory custom impl.
     */
    private static class JedisFactory extends BasePoolableObjectFactory {
        private final String host;
        private final int port;
        private final int timeout;
        private final String password;
        private final int database;

        public JedisFactory(final String host, final int port,
                final int timeout, final String password, final int database) {
            super();
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            this.password = password;
            this.database = database;
        }

        public Object makeObject() throws Exception {
            final Jedis jedis = new Jedis(this.host, this.port, this.timeout);

            jedis.connect();
            if (null != this.password) {
                jedis.auth(this.password);
            }
            if( database != 0 ) {
                jedis.select(database);
            }
            
            return jedis;
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
