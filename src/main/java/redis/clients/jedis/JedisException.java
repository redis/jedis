package redis.clients.jedis;

import java.io.IOException;

public class JedisException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = -2946266495682282677L;

    public JedisException(String message) {
	super(message);
    }

    public JedisException(IOException e) {
	super(e);
    }

	public JedisException(String message, Throwable cause) {
		super(message, cause);
	}
}
