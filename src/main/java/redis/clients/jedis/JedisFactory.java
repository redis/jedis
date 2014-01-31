package redis.clients.jedis;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * PoolableObjectFactory custom impl.
 */
class JedisFactory implements PooledObjectFactory<Jedis> {
    private final String host;
    private final int port;
    private final int timeout;
    private final String password;
    private final int database;
    private final String clientName;

    public JedisFactory(final String host, final int port, final int timeout,
	    final String password, final int database) {
	this(host, port, timeout, password, database, null);
    }

    public JedisFactory(final String host, final int port, final int timeout,
	    final String password, final int database, final String clientName) {
	super();
	this.host = host;
	this.port = port;
	this.timeout = timeout;
	this.password = password;
	this.database = database;
	this.clientName = clientName;
    }

    @Override
    public void activateObject(PooledObject<Jedis> pooledJedis)
	    throws Exception {
	final BinaryJedis jedis = pooledJedis.getObject();
	if (jedis.getDB() != database) {
	    jedis.select(database);
	}

    }

    @Override
    public void destroyObject(PooledObject<Jedis> pooledJedis) throws Exception {
	final BinaryJedis jedis = pooledJedis.getObject();
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

    @Override
    public PooledObject<Jedis> makeObject() throws Exception {
	final Jedis jedis = new Jedis(this.host, this.port, this.timeout);

	jedis.connect();
	if (null != this.password) {
	    jedis.auth(this.password);
	}
	if (database != 0) {
	    jedis.select(database);
	}
	if (clientName != null) {
	    jedis.clientSetname(clientName);
	}

	return new DefaultPooledObject<Jedis>(jedis);
    }

    @Override
    public void passivateObject(PooledObject<Jedis> pooledJedis)
	    throws Exception {
	// TODO maybe should select db 0? Not sure right now.
    }

    @Override
    public boolean validateObject(PooledObject<Jedis> pooledJedis) {
	final BinaryJedis jedis = pooledJedis.getObject();
	try {
	    return jedis.isConnected() && jedis.ping().equals("PONG");
	} catch (final Exception e) {
	    return false;
	}
    }
}