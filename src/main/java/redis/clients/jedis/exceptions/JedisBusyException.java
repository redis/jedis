package redis.clients.jedis.exceptions;

public class JedisBusyException extends JedisDataException {

    private static final long serialVersionUID = 3992655220229243478L;

    public JedisBusyException(final String message) {
        super(message);
    }

    public JedisBusyException(final Throwable cause) {
        super(cause);
    }

    public JedisBusyException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
