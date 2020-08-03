package redis.clients.jedis.exceptions;

public class JedisLockException extends JedisException {
    private static final long serialVersionUID = -1059062867308606225L;

    public JedisLockException(String message) {
        super(message);
    }

    public JedisLockException(Throwable e) {
        super(e);
    }

    public JedisLockException(String message, Throwable cause) {
        super(message, cause);
    }
}