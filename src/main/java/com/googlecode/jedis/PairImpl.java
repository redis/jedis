package com.googlecode.jedis;

import com.google.common.base.Objects;

/**
 * Immutable generic Pair class (2-Tupel).
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 * @param <F>
 *            Type of first element.
 * @param <S>
 *            Type of second element.
 */
public final class PairImpl<F, S> implements Pair<F, S> {

    /**
     * Pair factory method.
     * 
     * @param first
     *            element
     * @param second
     *            element
     * @return a new Pair
     */
    static public <F, S> Pair<F, S> newPair(final F first, final S second) {
	return new PairImpl<F, S>(first, second);
    }

    private final F first;
    private final S second;

    private PairImpl(final F first, final S second) {
	this.first = first;
	this.second = second;
    }

    @Override
    public boolean equals(final Object other) {
	if (other instanceof Pair) {
	    final Pair<?, ?> otherPair = (Pair<?, ?>) other;
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
     * @see com.googlecode.jedis.Pair#getFirst()
     */
    @Override
    public F getFirst() {
	return first;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Pair#getSecond()
     */
    @Override
    public S getSecond() {
	return second;
    }

    @Override
    public int hashCode() {
	final int hashFirst = first != null ? first.hashCode() : 0;
	final int hashSecond = second != null ? second.hashCode() : 0;
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
