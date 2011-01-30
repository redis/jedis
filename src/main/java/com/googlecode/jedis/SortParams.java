package com.googlecode.jedis;

import static com.googlecode.jedis.Protocol.DEFAULT_CHARSET;
import static com.googlecode.jedis.Protocol.Keyword.*;
import static java.lang.String.valueOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Builder Class for {@link Jedis#sort(String, SortParams) SORT} Parameters.
 * 
 */
public final class SortParams {

    /**
     * Convenient {@link #SortParams()} creator, e.g. for static import.
     * 
     * With this creator you can write a {@link Jedis#sort(String, SortParams)}
     * like this: {@code jedis.sort(key, newSortParams().limit(0, 20).asc())}
     * 
     * @return new instance
     */
    static public SortParams newSortParams() {
	return new SortParams();
    }

    private final List<byte[]> params = new ArrayList<byte[]>();

    /**
     * Sort lexicographicaly. Note that Redis is UTF-8 aware assuming you set
     * the right value for the LC_COLLATE environment variable.
     * 
     * @return the SortParams Object
     */
    public SortParams alpha() {
	params.add(ALPHA.raw);
	return this;
    }

    /**
     * Get the Sorting in Ascending Order. This is the default order and just
     * for completeness.
     * 
     * @return the SortParams Object
     */
    public SortParams asc() {
	params.add(ASC.raw);
	return this;
    }

    /**
     * Sort by weight in keys.
     * <p>
     * Takes a pattern that is used in order to generate the key names of the
     * weights used for sorting. Weight key names are obtained substituting the
     * first occurrence of * with the actual value of the elements on the list.
     * <p>
     * The pattern for a normal key/value pair is "keyname*" and for a value in
     * a hash "keyname*->fieldname".
     * 
     * @param pattern
     * @return the SortParams Object
     */
    public SortParams by(final byte[] pattern) {
	params.add(BY.raw);
	params.add(pattern);
	return this;
    }

    /**
     * Sort by weight in keys.
     * <p>
     * Takes a pattern that is used in order to generate the key names of the
     * weights used for sorting. Weight key names are obtained substituting the
     * first occurrence of * with the actual value of the elements on the list.
     * <p>
     * The pattern for a normal key/value pair is "keyname*" and for a value in
     * a hash "keyname*->fieldname".
     * 
     * @param pattern
     * @return the SortParams Object
     */
    public SortParams by(final String pattern) {
	return by(pattern.getBytes(DEFAULT_CHARSET));
    }

    /**
     * Get the Sorting in Descending Order.
     * 
     * @return the sortingParams Object
     */
    public SortParams desc() {
	params.add(DESC.raw);
	return this;
    }

    /**
     * Retrieving external keys from the result of the search.
     * <p>
     * Takes a pattern that is used in order to generate the key names of the
     * result of sorting. The key names are obtained substituting the first
     * occurrence of * with the actual value of the elements on the list.
     * <p>
     * The pattern for a normal key/value pair is "keyname*" and for a value in
     * a hash "keyname*->fieldname".
     * <p>
     * To get the list itself use the char # as pattern.
     * 
     * @param pattern1
     *            the first pattern
     * @param patternN
     *            all other pattern
     * @return the SortParams Object
     */
    public SortParams get(final byte[] pattern1, final byte[]... patternN) {
	params.add(GET.raw);
	params.add(pattern1);
	for (final byte[] pattern : patternN) {
	    params.add(GET.raw);
	    params.add(pattern);
	}
	return this;
    }

    /**
     * Retrieving external keys from the result of the search.
     * <p>
     * Takes a pattern that is used in order to generate the key names of the
     * result of sorting. The key names are obtained substituting the first
     * occurrence of * with the actual value of the elements on the list.
     * <p>
     * The pattern for a normal key/value pair is "keyname*" and for a value in
     * a hash "keyname*->fieldname".
     * <p>
     * To get the list itself use the char # as pattern.
     * 
     * @param pattern1
     *            the first pattern
     * @param patternN
     *            all other pattern
     * @return the SortParams Object
     */
    public SortParams get(final String pattern1, final String... patternN) {
	params.add(GET.raw);
	params.add(pattern1.getBytes(DEFAULT_CHARSET));
	for (final String pattern : patternN) {
	    params.add(GET.raw);
	    params.add(pattern.getBytes(DEFAULT_CHARSET));
	}
	return this;
    }

    public Collection<byte[]> getParams() {
	return Collections.unmodifiableCollection(params);
    }

    /**
     * Limit the Numbers of returned Elements.
     * 
     * @param offset
     *            is zero based
     * @param count
     *            of the limit
     * @return the SortParams Object
     */
    public SortParams limit(final long offset, final long count) {
	params.add(LIMIT.raw);
	params.add(valueOf(offset).getBytes(DEFAULT_CHARSET));
	params.add(valueOf(count).getBytes(DEFAULT_CHARSET));
	return this;
    }

    /**
     * No sorting.
     * <p>
     * This is useful if you want to retrieve a external key (using
     * {@link #get(String, String[]) GET}) but you don't want the sorting
     * overhead.
     * 
     * @return the SortParams Object
     */
    public SortParams nosort() {
	params.add(BY.raw);
	params.add(NOSORT.raw);
	return this;
    }
}
