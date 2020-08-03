package redis.clients.jedis.exceptions;

public class AcquireLockException extends JedisLockException {
    private static final long serialVersionUID = -917686777280276954L;

    public AcquireLockException(String message) {
        super(message);
    }

    public AcquireLockException(Throwable e) {
        super(e);
    }

    public AcquireLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
