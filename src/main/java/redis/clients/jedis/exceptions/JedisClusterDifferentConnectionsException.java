package redis.clients.jedis.exceptions;

public class JedisClusterDifferentConnectionsException extends JedisDataException {
    private static final long serialVersionUID = 3878126572474819403L;

    public JedisClusterDifferentConnectionsException(Throwable cause) {
    super(cause);
    }

    public JedisClusterDifferentConnectionsException(String message, Throwable cause) {
    super(message, cause);
    }

    public JedisClusterDifferentConnectionsException(String message) {
    super(message);
    }
}