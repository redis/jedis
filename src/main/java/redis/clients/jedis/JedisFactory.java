package redis.clients.jedis;

import org.apache.commons.pool.BasePoolableObjectFactory;

/**
 * PoolableObjectFactory custom impl.
 */
class JedisFactory extends BasePoolableObjectFactory<Jedis> {
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

    public Jedis makeObject() throws Exception {
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
    
    @Override
    public void activateObject(Jedis jedis) throws Exception {
            if (jedis.getDB() != database) {
            	jedis.select(database);
            }
    }

    public void destroyObject(Jedis jedis) throws Exception {
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

    public boolean validateObject(Jedis jedis) {
            try {
                return jedis.isConnected() && jedis.ping().equals("PONG");
            } catch (final Exception e) {
                return false;
            }
    }
}