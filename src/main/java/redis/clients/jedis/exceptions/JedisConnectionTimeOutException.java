package redis.clients.jedis.exceptions;

public class JedisConnectionTimeOutException extends JedisException {
	private static final long serialVersionUID = 3878126572474819403L;

	public JedisConnectionTimeOutException(String message) {
		super(message);
	}

	public JedisConnectionTimeOutException(Throwable cause) {
		super(cause);
	}

	public JedisConnectionTimeOutException(String message, Throwable cause) {
		super(message, cause);
	}
}
