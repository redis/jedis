package redis.clients.jedis.authentication;

import redis.clients.jedis.exceptions.JedisException;

public class JedisAuthenticationException extends JedisException {

    public JedisAuthenticationException(String message) {
        super(message);
    }

    public JedisAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
