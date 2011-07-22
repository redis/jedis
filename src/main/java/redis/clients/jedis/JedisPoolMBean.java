package redis.clients.jedis;

public interface JedisPoolMBean {

    // Attributes
    public String getHost();
    public int getPort();
    public int getTimeout();

    // Operations
    public void updateHostAndPort(final String host, final int port);
}
