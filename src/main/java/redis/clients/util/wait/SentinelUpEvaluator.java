package redis.clients.util.wait;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * An evaluator which checks if it is possible to connect to Sentinel.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class SentinelUpEvaluator implements IEvaluator {

    public static final int CONN_TIMEOUT = 1000;

    /**
     * The Sentinel host
     */
    private String host;

    /**
     * The Sentinel port
     */
    private int port;

    /**
     * Redis client
     */
    private Jedis client;


    /**
     * Connection timeout
     */
    private int timeout;


    /**
     * Default Ctor
     *
     * @param host
     * @param port
     */
    public SentinelUpEvaluator(String host, int port) {

       this(host,port, CONN_TIMEOUT);
    }

    /**
     * Full Ctor
     *
     * @param host
     * @param port
     * @param timeout
     */
    public SentinelUpEvaluator(String host, int port, int timeout) {

        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.client = new Jedis(host, port);
    }



    /**
     * Check if it's possible to connect to Sentinel.
     *
     * @return True if a connection was possible
     */
    @Override
    public boolean check() {

        try {

            client = new Jedis(this.host, this.port, this.timeout);
            client.connect();
            client.disconnect();
            return true;

        } catch (JedisConnectionException e) {

            return false;
        }


    }
}
