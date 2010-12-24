package redis.clients.jedis;

/**
 * Pair Interface for use with Jedis.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 * @param <A>
 *            first element type
 * @param <B>
 *            second element type
 */
public interface Pair<A, B> {

    /**
     * Get first element.
     * 
     * @return first element
     */
    public A getFirst();

    /**
     * Get second element.
     * 
     * @return second element
     */
    public B getSecond();

}