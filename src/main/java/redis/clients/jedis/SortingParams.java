package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Builder Class for {@link Jedis#sort(String, SortingParams) SORT} Parameters.
 * 
 */
public class SortingParams {
    private List<String> params = new ArrayList<String>();

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
     * @return the SortingParams Object
     */
    public SortingParams by(String pattern) {
	params.add("BY");
	params.add(pattern);
	return this;
    }

    /**
     * No sorting.
     * <p>
     * This is useful if you want to retrieve a external key (using
     * {@link #get(String...) GET}) but you don't want the sorting overhead.
     * 
     * @return the SortingParams Object
     */
    public SortingParams nosort() {
	params.add("BY nosort");
	return this;
    }

    public Collection<String> getParams() {
	return Collections.unmodifiableCollection(params);
    }

    /**
     * Get the Sorting in Descending Order.
     * 
     * @return the sortingParams Object
     */
    public SortingParams desc() {
	params.add("DESC");
	return this;
    }

    /**
     * Get the Sorting in Ascending Order. This is the default order.
     * 
     * @return the SortingParams Object
     */
    public SortingParams asc() {
	params.add("ASC");
	return this;
    }

    /**
     * Limit the Numbers of returned Elements.
     * 
     * @param start
     *            is zero based
     * @param count
     * @return the SortingParams Object
     */
    public SortingParams limit(int start, int count) {
	params.add("LIMIT");
	params.add(String.valueOf(start));
	params.add(String.valueOf(count));
	return this;
    }

    /**
     * Sort lexicographicaly. Note that Redis is utf-8 aware assuming you set
     * the right value for the LC_COLLATE environment variable.
     * 
     * @return the SortingParams Object
     */
    public SortingParams alpha() {
	params.add("ALPHA");
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
     * @param patterns
     * @return the SortingParams Object
     */
    public SortingParams get(String... patterns) {
	for (String pattern : patterns) {
	    params.add("GET");
	    params.add(pattern);
	}
	return this;
    }
}