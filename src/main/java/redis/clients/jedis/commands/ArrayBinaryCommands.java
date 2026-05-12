package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.args.ArrayOp;
import redis.clients.jedis.params.ArgrepParams;

/**
 * Binary commands for the Redis <b>array</b> data type.
 */
public interface ArrayBinaryCommands {

  /**
   * <b><a href="https://redis.io/commands/arcount">ARCOUNT Command</a></b>
   * Returns the number of non-empty elements in an array.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @return the number of non-empty elements, or {@code 0} if {@code key} does not exist
   * @since 8.0
   */
  long arcount(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/ardel">ARDEL Command</a></b>
   * Deletes elements at the specified indices in an array. Indices that do
   * not exist count as zero elements deleted and leave the array unchanged.
   * <p>
   * Time complexity: O(N) where N is the number of indices supplied.
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to delete
   * @return the number of elements actually deleted
   * @since 8.0
   */
  long ardel(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/ardelrange">ARDELRANGE Command</a></b>
   * Deletes elements in one or more inclusive ranges of indices. Each range
   * is processed in ascending order even if {@code start > end}; overlapping
   * pairs count each element at most once.
   * <p>
   * Time complexity: O(M + N) where M is the number of ranges and N the
   * total number of elements they cover.
   * @param key the name of the key that holds the array
   * @param ranges one or more {@code [start, end]} pairs
   * @return the number of elements deleted
   * @since 8.0
   */
  long ardelrange(byte[] key, long[]... ranges);

  /**
   * <b><a href="https://redis.io/commands/arget">ARGET Command</a></b>
   * Returns the value stored at a single index in an array.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @param index the zero-based index of the element to retrieve
   * @return the value at the given index, or {@code null} if the key or
   *         index does not exist
   * @since 8.0
   */
  byte[] arget(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/argetrange">ARGETRANGE Command</a></b>
   * Returns the values at a contiguous range of indices. When
   * {@code start > end}, elements are returned in reverse index order.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the range.
   * @param key the name of the key that holds the array
   * @param start zero-based start index of the range
   * @param end zero-based end index of the range (inclusive)
   * @return the list of values in traversal order; empty slots appear as
   *         {@code null} entries
   * @since 8.0
   */
  List<byte[]> argetrange(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/argrep">ARGREP Command</a></b>
   * Searches array elements within an inclusive index range using one or
   * more textual predicates. Empty slots are skipped. Multiple predicates
   * can be combined with {@code AND}/{@code OR} via {@link ArgrepParams}.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive); when {@code start > end}
   *              the iteration is reversed
   * @param end zero-based end index (inclusive)
   * @param params the predicates and options to apply
   * @return matching indices in traversal order, or alternating
   *         index/value pairs when {@code WITHVALUES} is set; empty when no
   *         match
   * @since 8.0
   */
  List<Object> argrep(byte[] key, long start, long end, ArgrepParams params);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b>
   * Returns metadata describing an array (counts, slice geometry, insertion
   * cursor). Top-level fields are always returned.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @return a map of metadata fields to their values
   * @since 8.0
   */
  Map<String, Object> arinfo(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b>
   * Returns metadata describing an array. When {@code full} is {@code true},
   * the reply additionally includes per-slice statistics.
   * <p>
   * Time complexity: O(1) without {@code FULL}; O(S) with {@code FULL} where
   * S is the number of slices.
   * @param key the name of the key that holds the array
   * @param full whether to request the {@code FULL} variant
   * @return a map of metadata fields to their values
   * @since 8.0
   */
  Map<String, Object> arinfo(byte[] key, boolean full);

  /**
   * <b><a href="https://redis.io/commands/arinsert">ARINSERT Command</a></b>
   * Inserts one or more values at consecutive indices, beginning at the
   * current insert cursor position. The cursor advances by one for each
   * value inserted.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * @param key the name of the key that holds the array
   * @param values one or more values to insert
   * @return the last index at which a value was inserted
   * @since 8.0
   */
  long arinsert(byte[] key, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b>
   * Returns the most recently inserted elements in oldest-first order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @return the list of last-inserted values
   * @since 8.0
   */
  List<byte[]> arlastitems(byte[] key, long count);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b>
   * Returns the most recently inserted elements. When {@code rev} is
   * {@code true}, elements are returned in reverse chronological order
   * (most recent first) instead of the default oldest-first order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @param rev whether to request the {@code REV} variant
   * @return the list of last-inserted values
   * @since 8.0
   */
  List<byte[]> arlastitems(byte[] key, long count, boolean rev);

  /**
   * <b><a href="https://redis.io/commands/arlen">ARLEN Command</a></b>
   * Returns the length of an array (max index + 1).
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @return the array length, or {@code 0} if the key does not exist
   * @since 8.0
   */
  long arlen(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/armget">ARMGET Command</a></b>
   * Returns the values at multiple indices. The reply preserves the order of
   * the requested indices and contains {@code null} for any index that is
   * not set.
   * <p>
   * Time complexity: O(N) where N is the number of requested indices.
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to retrieve
   * @return the list of values aligned with {@code indices}
   * @since 8.0
   */
  List<byte[]> armget(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/armset">ARMSET Command</a></b>
   * Sets multiple index-value pairs. Pairs may be non-contiguous and in any
   * order; the map's iteration order is used as the wire order.
   * <p>
   * Time complexity: O(N) where N is the number of pairs.
   * @param key the name of the key that holds the array
   * @param indexValueMap the index-to-value pairs to write
   * @return the number of slots that were previously empty
   * @since 8.0
   */
  long armset(byte[] key, Map<Long, byte[]> indexValueMap);

  /**
   * <b><a href="https://redis.io/commands/arnext">ARNEXT Command</a></b>
   * Returns the next index that {@code ARINSERT} would use.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @return the next insert index; {@code 0} for missing keys or when no
   *         insert has happened yet; {@code null} when the insertion cursor
   *         is exhausted
   * @since 8.0
   */
  Long arnext(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b>
   * Applies an aggregate operation over the non-empty elements in
   * {@code [start, end]}. The command always scans from the lower to the
   * higher index regardless of argument order. Use this overload for
   * operations that take no extra argument: {@link ArrayOp#SUM},
   * {@link ArrayOp#MIN}, {@link ArrayOp#MAX}, {@link ArrayOp#AND},
   * {@link ArrayOp#OR}, {@link ArrayOp#XOR}, {@link ArrayOp#USED}.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * @param key the name of the key that holds the array
   * @param start zero-based start index of the range
   * @param end zero-based end index of the range (inclusive)
   * @param op the aggregate operation to apply
   * @return the operation's result; a byte[] for {@code SUM}, {@code MIN},
   *         {@code MAX}; a {@link Long} for {@code AND}, {@code OR},
   *         {@code XOR}, {@code USED}; or {@code null} when no matching
   *         elements are present
   * @since 8.0
   */
  Object arop(byte[] key, long start, long end, ArrayOp op);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b>
   * Counts elements equal to {@code value} within {@code [start, end]}
   * (equivalent to {@code AROP key start end MATCH value}). The command
   * always scans from the lower to the higher index regardless of argument
   * order.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * @param key the name of the key that holds the array
   * @param start zero-based start index of the range
   * @param end zero-based end index of the range (inclusive)
   * @param value the value to match
   * @return the count of matching elements
   * @since 8.0
   */
  long aropMatch(byte[] key, long start, long end, byte[] value);

  /**
   * <b><a href="https://redis.io/commands/arring">ARRING Command</a></b>
   * Inserts one or more values into an array as a fixed-size ring buffer.
   * Each value is placed at the next ring position and the cursor advances
   * accordingly.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * @param key the name of the key that holds the array
   * @param size the ring buffer window size
   * @param values one or more values to insert
   * @return the last index at which a value was inserted
   * @since 8.0
   */
  long arring(byte[] key, long size, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b>
   * Iterates existing elements in an inclusive index range and returns a
   * flat array of alternating index/value pairs in traversal order. Empty
   * slots are excluded.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements.
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the
   *              iteration is reversed
   * @param end zero-based end index (inclusive)
   * @return alternating {@code [idx1, val1, idx2, val2, ...]}; empty when
   *         the key does not exist
   * @since 8.0
   */
  List<Object> arscan(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b>
   * Iterates existing elements in an inclusive index range with a cap on
   * the number of elements returned. Empty slots are excluded.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements
   * returned.
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the
   *              iteration is reversed
   * @param end zero-based end index (inclusive)
   * @param limit cap on the number of returned populated elements
   * @return alternating {@code [idx1, val1, idx2, val2, ...]}
   * @since 8.0
   */
  List<Object> arscan(byte[] key, long start, long end, long limit);

  /**
   * <b><a href="https://redis.io/commands/arseek">ARSEEK Command</a></b>
   * Sets the insert cursor of an array to a specific index.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the array
   * @param index the zero-based index to set as the new insert cursor
   * @return {@code 1} if the cursor was set, {@code 0} if the key does not
   *         exist
   * @since 8.0
   */
  long arseek(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/arset">ARSET Command</a></b>
   * Sets one or more contiguous values starting at {@code index}. When
   * multiple values are provided, they are stored at consecutive indices.
   * <p>
   * Time complexity: O(N) where N is the number of values written.
   * @param key the name of the key that holds the array
   * @param index zero-based starting index
   * @param values one or more values to write at consecutive indices
   * @return the number of slots that were previously empty
   * @since 8.0
   */
  long arset(byte[] key, long index, byte[]... values);

}
