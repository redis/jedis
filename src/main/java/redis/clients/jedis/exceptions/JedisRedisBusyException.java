package redis.clients.jedis.exceptions;

public class JedisRedisBusyException extends JedisDataException {

    private static final long serialVersionUID = 3992655220229243478L;

    public JedisRedisBusyException(final String message) {
        super(message);
    }

    public JedisRedisBusyException(final Throwable cause) {
        super(cause);
    }

    public JedisRedisBusyException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
