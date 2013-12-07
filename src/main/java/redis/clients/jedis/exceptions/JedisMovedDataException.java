package redis.clients.jedis.exceptions;

public class JedisMovedDataException extends JedisDataException {
    private static final long serialVersionUID = 3878126572474819403L;

    public JedisMovedDataException(String message) {
        super(message);
    }

    public JedisMovedDataException(Throwable cause) {
        super(cause);
    }

    public JedisMovedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
