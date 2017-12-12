package redis.clients.jedis.exceptions;


public final class InvalidProxyException extends JedisException {
    public InvalidProxyException(String message) {
        super(message);
    }

    public InvalidProxyException(Throwable e) {
        super(e);
    }

    public InvalidProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
