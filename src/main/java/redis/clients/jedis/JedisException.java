package redis.clients.jedis;

public class JedisException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -2946266495682282677L;

    public JedisException(String message) {
	super(message);
    }
}
