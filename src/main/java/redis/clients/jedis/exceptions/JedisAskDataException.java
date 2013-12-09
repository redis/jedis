package redis.clients.jedis.exceptions;

public class JedisAskDataException extends JedisDataException {
    private static final long serialVersionUID = 3878126572474819403L;

    public JedisAskDataException(String message) {
        super(message);
    }

    public JedisAskDataException(Throwable cause) {
        super(cause);
    }

    public JedisAskDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
