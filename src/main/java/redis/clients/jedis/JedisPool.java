package redis.clients.jedis;

import java.net.URI;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.util.Pool;

public class JedisPool extends Pool<JedisPooledConnection>
{
    public JedisPool(final Config poolConfig, final String host)
    {
        this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
                Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(String host, int port)
    {
        this(new Config(), host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final String host)
    {
        URI uri = URI.create(host);
        if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
            String h = uri.getHost();
            int port = uri.getPort();
            String password = uri.getUserInfo().split(":", 2)[1];
            int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);

            JedisFactory factory =
                new JedisFactory(this, h, port, Protocol.DEFAULT_TIMEOUT, password, database);
            this.internalPool = new GenericObjectPool(factory, new Config());
        } else {
            JedisFactory factory =
                new JedisFactory(this, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
                        Protocol.DEFAULT_DATABASE);

            this.internalPool = new GenericObjectPool(factory, new Config());
        }
    }

    public JedisPool(final URI uri)
    {
        String h = uri.getHost();
        int port = uri.getPort();
        String password = uri.getUserInfo().split(":", 2)[1];
        int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);

        JedisFactory factory =
                new JedisFactory(this, h, port, Protocol.DEFAULT_TIMEOUT, password, database);

        this.internalPool = new GenericObjectPool(factory, new Config());
    }

    public JedisPool(final Config poolConfig, final String host, int port,
            int timeout, final String password)
    {
        this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, final int port)
    {
        this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, final int port,
            final int timeout)
    {
        this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, int port, int timeout,
            final String password, final int database)
    {
        JedisFactory factory = new JedisFactory(this, host, port, timeout, password, database);
        this.internalPool = new GenericObjectPool(factory, poolConfig);
    }

    public void returnBrokenResource(final BinaryJedis resource)
    {
        returnBrokenResourceObject(resource);
    }

    public void returnResource(final BinaryJedis resource)
    {
        returnResourceObject(resource);
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    private static class JedisFactory extends BasePoolableObjectFactory
    {
        private JedisPool pool;
        private final String host;
        private final int port;
        private final int timeout;
        private final String password;
        private final int database;

        public JedisFactory(JedisPool pool, final String host, final int port, final int timeout,
                final String password, final int database)
        {
            super();

            this.pool = pool;
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            this.password = password;
            this.database = database;
        }

        public Object makeObject() throws Exception
        {
            assert pool != null;
            final JedisPooledConnection jpc = new JedisPooledConnection(pool, this.host, this.port,
                    this.timeout);

            jpc.connect();
            if (null != this.password) {
                jpc.auth(this.password);
            }

            if (database != 0) {
                jpc.select(database);
            }

            return jpc;
        }

        public void destroyObject(final Object obj) throws Exception
        {
            if (obj instanceof Jedis) {
                final Jedis jedis = (Jedis) obj;
                if (jedis.isConnected()) {
                    try {
                        try {
                            jedis.quit();
                        } catch (Exception e) {
                            // Nothing to do.
                        }

                        jedis.disconnect();
                    } catch (Exception e) {

                    }
                }
            }
        }

        public boolean validateObject(final Object obj)
        {
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
