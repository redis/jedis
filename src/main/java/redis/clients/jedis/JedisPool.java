package redis.clients.jedis;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.util.Pool;

public class JedisPool extends Pool<Jedis> implements JedisPoolMBean {

    protected static Logger logger = LoggerFactory.getLogger(JedisPool.class);
    private JedisFactory factory;

    public JedisPool(final Config poolConfig, final String host) {
        this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(String host, int port) {
        this(new Config(), host, port, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final String host) {
        this(host, Protocol.DEFAULT_PORT);
    }
    
    /**
     * Same as {@link #JedisPool(String, int)} except this
     * constructor will also register itself to jmx with the poolName
     * 
     * @param host
     * @param port
     * @param poolName
     */
    public JedisPool(final String host, final int port, final String poolName) {
        this(host, port);
        register(poolName);
    }

    public JedisPool(final Config poolConfig, final String host, final int port,
            int timeout, final String password) {
        super();
        factory = new JedisFactory(host, port,
                ((timeout > 0) ? timeout : -1), password, Protocol.DEFAULT_DATABASE);
        init(poolConfig, factory);
    }

    /**
     * Same as {@link #JedisPool(Config, String, int, int, String)} except this
     * constructor will also register itself to jmx with the poolName
     * 
     * @param poolConfig
     * @param host
     * @param port
     * @param timeout
     * @param password
     * @param poolName
     */
    public JedisPool(final Config poolConfig, final String host, int port,
            int timeout, final String password, final String poolName) {
        this(poolConfig, host, port, timeout, password, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, int port,
            int timeout, final String password, final String poolName, final int database) {
        this(poolConfig, host, port, timeout, password, database);
        register(poolName);
    }

    public JedisPool(final Config poolConfig, final String host, final int port, final int timeout) {
        this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE);
    }

    public JedisPool(final Config poolConfig, final String host, int port, int timeout, final String password,
                     final int database) {
        super(poolConfig, new JedisFactory(host, port, timeout, password, database));
    }
    
    /**
     * Register itself to jmx
     * @param poolName
     */
    private void register(final String poolName) {
        final String beanName = this.getClass().getPackage().getName() + ":name=" + poolName;
        logger.info("Registering JMX " + beanName);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = null;
        try {
            on = new ObjectName(beanName);
        } catch (MalformedObjectNameException e) {
            logger.warn("Unable to register " + beanName, e);
            return;
        } catch (NullPointerException e) {
            logger.warn("Unable to register " + beanName, e);
            return;
        }

        if (!mbs.isRegistered(on)) {
            try {
                mbs.registerMBean(this, on);
            } catch (InstanceAlreadyExistsException e) {
                logger.warn("Unable to register " + beanName, e);
            } catch (MBeanRegistrationException e) {
                logger.warn("Unable to register " + beanName, e);
            } catch (NotCompliantMBeanException e) {
                logger.warn("Unable to register " + beanName, e);
            }
        }
    }
    
    @Override
    public String getHost() {
        return factory.getHost();
    }

    @Override
    public int getPort() {
        return factory.getPort();
    }

    @Override
    public int getTimeout() {
        return factory.getTimeout();
    }

    /**
     * Gracefully reset new host and port
     * 
     * @param host
     * @param port
     */
    @Override
    public void updateHostAndPort(final String host, final int port) {
        // update facotry
        factory.updateHostAndPort(host, port);

        // Remove the idle Jedis
        clear();

        // the active ones will be either validated off or timed out if
        // all of the TestOn* was set to false. 
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    private static class JedisFactory extends BasePoolableObjectFactory {
        private String host;
        private int port;
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

        public void updateHostAndPort(final String host, final int port) {
            synchronized (this) {
                this.host = host;
                this.port = port;
            }
        }

        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
        
        public int getTimeout() {
            return timeout;
        }

        @Override
        public Object makeObject() throws Exception {
            final Jedis jedis;
            if (timeout > 0) {
                jedis = new Jedis(host, port, timeout);
            } else {
                jedis = new Jedis(host, port);
            }

            jedis.connect();
            if (null != password) {
                jedis.auth(password);
            }
            if( database != 0 ) {
                jedis.select(database);
            }
            
            return jedis;
        }

        @Override
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

        @Override
        public boolean validateObject(final Object obj) {
            if (obj instanceof Jedis) {
                final Jedis jedis = (Jedis) obj;
                String currentHost = host + ":" + port;
                if (!jedis.connectedTo().equals(currentHost)) {
                    return false;
                }

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
