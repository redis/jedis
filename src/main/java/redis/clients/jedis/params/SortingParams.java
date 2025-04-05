package redis.clients.jedis.params;

import java.util.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Builder Class for {@code SORT} command parameters.
 */
// TODO:
public class SortingParams implements IParams {

  private final List<Object> params = new ArrayList<>();

  /**
   * Sort by weight in keys.
   * <p>
   * Takes a pattern that is used in order to generate the key names of the weights used for
   * sorting. Weight key names are obtained substituting the first occurrence of * with the actual
   * value of the elements on the list.
   * <p>
   * The pattern for a normal key/value pair is "field*" and for a value in a hash
   * "field*-&gt;fieldname".
   * @param pattern
   * @return the SortingParams Object
   */
  public SortingParams by(final String pattern) {
    return by(SafeEncoder.encode(pattern));
  }

  /**
   * Sort by weight in keys.
   * <p>
   * Takes a pattern that is used in order to generate the key names of the weights used for
   * sorting. Weight key names are obtained substituting the first occurrence of * with the actual
   * value of the elements on the list.
   * <p>
   * The pattern for a normal key/value pair is "field*" and for a value in a hash
   * "field*-&gt;fieldname".
   * @param pattern
   * @return the SortingParams Object
   */
  public SortingParams by(final byte[] pattern) {
    params.add(Keyword.BY);
    params.add(pattern);
    return this;
  }

  /**
   * No sorting.
   * <p>
   * This is useful if you want to retrieve an external key (using {@link #get(String...) GET}) but
   * you don't want the sorting overhead.
   * @return the SortingParams Object
   */
  public SortingParams nosort() {
    params.add(Keyword.BY);
    params.add(Keyword.NOSORT);
    return this;
  }

  /**
   * Get the Sorting in Descending Order.
   * @return the sortingParams Object
   */
  public SortingParams desc() {
    return sortingOrder(SortingOrder.DESC);
  }

  /**
   * Get the Sorting in Ascending Order. This is the default order.
   * @return the SortingParams Object
   */
  public SortingParams asc() {
    return sortingOrder(SortingOrder.ASC);
  }

  /**
   * Get by the Sorting Order.
   * @param order the Sorting order
   * @return the SortingParams object
   */
  public SortingParams sortingOrder(SortingOrder order) {
    params.add(order.getRaw());
    return this;
  }

  /**
   * Limit the Numbers of returned Elements.
   * @param start is zero based
   * @param count
   * @return the SortingParams Object
   */
  public SortingParams limit(final int start, final int count) {
    params.add(Keyword.LIMIT);
    params.add(start);
    params.add(count);
    return this;
  }

  /**
   * Sort lexicographicaly. Note that Redis is utf-8 aware assuming you set the right value for the
   * LC_COLLATE environment variable.
   * @return the SortingParams Object
   */
  public SortingParams alpha() {
    params.add(Keyword.ALPHA);
    return this;
  }

  /**
   * Retrieving external keys from the result of the search.
   * <p>
   * Takes a pattern that is used in order to generate the key names of the result of sorting. The
   * key names are obtained substituting the first occurrence of * with the actual value of the
   * elements on the list.
   * <p>
   * The pattern for a normal key/value pair is "field*" and for a value in a hash
   * "field*-&gt;fieldname".
   * <p>
   * To get the list itself use the char # as pattern.
   * @param patterns
   * @return the SortingParams Object
   */
  public SortingParams get(String... patterns) {
    for (final String pattern : patterns) {
      params.add(Keyword.GET);
      params.add(pattern);
    }
    return this;
  }

  /**
   * Retrieving external keys from the result of the search.
   * <p>
   * Takes a pattern that is used in order to generate the key names of the result of sorting. The
   * key names are obtained substituting the first occurrence of * with the actual value of the
   * elements on the list.
   * <p>
   * The pattern for a normal key/value pair is "field*" and for a value in a hash
   * "field*-&gt;fieldname".
   * <p>
   * To get the list itself use the char # as pattern.
   * @param patterns
   * @return the SortingParams Object
   */
  public SortingParams get(byte[]... patterns) {
    for (final byte[] pattern : patterns) {
      params.add(Keyword.GET);
      params.add(pattern);
    }
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addObjects(params);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SortingParams that = (SortingParams) o;
    return Objects.equals(params, that.params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params);
  }
}
