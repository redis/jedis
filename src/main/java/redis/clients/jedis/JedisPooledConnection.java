package redis.clients.jedis;

public class JedisPooledConnection extends Jedis
{
    private final JedisPool pool;

    public JedisPooledConnection(JedisPool pool, String host, int port, int timeout)
    {
        super(host, port, timeout);
        this.pool = pool;
    }

    public void returnResource()
    {
        pool.returnResource(this);
    }
}
