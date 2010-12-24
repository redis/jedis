package redis.clients.jedis;

import com.google.common.base.Objects;

/**
 * Immutable generic Pair class (2-Tupel).
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 * @param <A>
 *            Type of first element.
 * @param <B>
 *            Type of second element.
 */
public final class PairImpl<A, B> implements Pair<A, B> {

    /**
     * Pair factory method.
     * 
     * @param <A>
     *            Type of first element.
     * @param <B>
     *            Type of second element.
     * @param first
     *            element
     * @param second
     *            element
     * @return a new Pair
     */
    static public <A, B> Pair<A, B> newPair(A first, B second) {
	return new PairImpl<A, B>(first, second);
    }

    private final A first;
    private final B second;

    private PairImpl(A first, B second) {
	this.first = first;
	this.second = second;
    }

    @Override
    public boolean equals(Object other) {
	if (other instanceof Pair) {
	    Pair<?, ?> otherPair = (Pair<?, ?>) other;
	    return ((this.first == otherPair.getFirst() || (this.first != null
		    && otherPair.getFirst() != null && this.first
		    .equals(otherPair.getFirst()))) && (this.second == otherPair
		    .getSecond() || (this.second != null
		    && otherPair.getFirst() != null && this.second
		    .equals(otherPair.getSecond()))));
	}

	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.Pair#getFirst()
     */
    public A getFirst() {
	return first;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.Pair#getSecond()
     */
    public B getSecond() {
	return second;
    }

    @Override
    public int hashCode() {
	int hashFirst = first != null ? first.hashCode() : 0;
	int hashSecond = second != null ? second.hashCode() : 0;
	return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    @Override
    public String toString() {
	return Objects.toStringHelper(PairImpl.class)
		.add("A", first.getClass().getName())
		.add("fist", first.toString())
		.add("B", second.getClass().getName())
		.add("second", second.toString()).toString();
    }

}
