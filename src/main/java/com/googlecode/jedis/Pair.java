package com.googlecode.jedis;

/**
 * Pair Interface for use with Jedis.
 * 
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 * @param <F>
 *            first element type
 * @param <S>
 *            second element type
 */
public interface Pair<F, S> {

    /**
     * Get first element.
     * 
     * @return first element
     */
    public F getFirst();

    /**
     * Get second element.
     * 
     * @return second element
     */
    public S getSecond();

}