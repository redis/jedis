package redis.clients.jedis.authentication;

public class JedisAuthenticationException extends RuntimeException {

    public JedisAuthenticationException(String message) {
        super(message);
    }

    public JedisAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
