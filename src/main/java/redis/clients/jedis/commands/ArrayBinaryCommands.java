package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.ArrayAggregate;
import redis.clients.jedis.args.ArrayBitwise;
import redis.clients.jedis.args.LongRange;
import redis.clients.jedis.params.ArgrepParams;
import redis.clients.jedis.resps.ArrayFullInfo;
import redis.clients.jedis.resps.ArrayInfo;
import redis.clients.jedis.util.KeyValue;

/**
 * Binary commands for the Redis <b>array</b> data type.
 */
public interface ArrayBinaryCommands {

  /**
   * <b><a href="https://redis.io/commands/arcount">ARCOUNT Command</a></b> Returns the number of
   * non-empty elements in an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARCOUNT key}
   * @param key the name of the key that holds the array
   * @return the number of non-empty elements, or {@code 0} if {@code key} does not exist
   * @since 8.0
   */
  long arcount(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/ardel">ARDEL Command</a></b> Deletes the element at a
   * single index in an array. If the index does not exist the array is unchanged.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARDEL key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index to delete
   * @return {@code 1} if an element was deleted, {@code 0} otherwise
   * @since 8.0
   */
  long ardel(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/ardel">ARDEL Command</a></b> Deletes elements at the
   * specified indices in an array. Indices that do not exist count as zero elements deleted and
   * leave the array unchanged.
   * <p>
   * Time complexity: O(N) where N is the number of indices supplied.
   * <p>
   * Wire: {@code ARDEL key index1 [index2 ...]}
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to delete
   * @return the number of elements actually deleted
   * @since 8.0
   */
  long ardel(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/ardelrange">ARDELRANGE Command</a></b> Deletes elements
   * in one or more inclusive {@link LongRange} ranges of indices. Each range is processed in
   * ascending order even if {@code start > end}; overlapping ranges count each element at most
   * once.
   * <p>
   * Time complexity: O(M + N) where M is the number of ranges and N the total number of elements
   * they cover.
   * <p>
   * Wire: {@code ARDELRANGE key r1.start r1.end [r2.start r2.end ...]}
   * @param key the name of the key that holds the array
   * @param ranges one or more inclusive index ranges
   * @return the number of elements deleted
   * @since 8.0
   */
  long ardelrange(byte[] key, LongRange... ranges);

  /**
   * <b><a href="https://redis.io/commands/ardelrange">ARDELRANGE Command</a></b> Convenience
   * single-range overload of {@link #ardelrange(byte[], LongRange...)} that accepts the inclusive
   * {@code [start, end]} range as primitives.
   * <p>
   * Wire: {@code ARDELRANGE key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @return the number of elements deleted
   * @since 8.0
   */
  long ardelrange(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arget">ARGET Command</a></b> Returns the value stored at
   * a single index in an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARGET key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index of the element to retrieve
   * @return the value at the given index, or {@code null} if the key or index does not exist
   * @since 8.0
   */
  byte[] arget(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/argetrange">ARGETRANGE Command</a></b> Returns the values
   * at a contiguous range of indices. When {@code start > end}, elements are returned in reverse
   * index order.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the range.
   * <p>
   * Wire: {@code ARGETRANGE key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index of the range
   * @param end zero-based end index of the range (inclusive)
   * @return the list of values in traversal order; empty slots appear as {@code null} entries
   * @since 8.0
   */
  List<byte[]> argetrange(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/argrep">ARGREP Command</a></b> Searches array elements
   * within an inclusive index range (carried by {@link ArgrepParams}) using one or more textual
   * predicates and returns the matching indices. Empty slots are skipped. Multiple predicates can
   * be combined with {@code AND}/{@code OR} via {@link ArgrepParams}. Use
   * {@link #argrepWithValues(byte[], ArgrepParams)} to also return the matching values.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code ARGREP key start end <params>}
   * @param key the name of the key that holds the array
   * @param params the search range, predicates and options to apply
   * @return the matching indices in traversal order; empty when no match
   * @since 8.0
   */
  @Experimental
  List<Long> argrep(byte[] key, ArgrepParams params);

  /**
   * <b><a href="https://redis.io/commands/argrep">ARGREP Command</a></b> Searches array elements
   * within an inclusive index range (carried by {@link ArgrepParams}) and returns the matching
   * index/value pairs. Empty slots are skipped. This overload appends the {@code WITHVALUES}
   * keyword to the wire arguments; do not also request it on {@link ArgrepParams}.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code ARGREP key start end <params> WITHVALUES}
   * @param key the name of the key that holds the array
   * @param params the search range, predicates and options to apply
   * @return the matching index/value pairs in traversal order; empty when no match
   * @since 8.0
   */
  @Experimental
  List<KeyValue<Long, byte[]>> argrepWithValues(byte[] key, ArgrepParams params);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b> Returns metadata
   * describing an array (counts, length, insertion cursor). Use {@link #arinfoFull(byte[])} for the
   * per-slice statistics.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARINFO key}
   * @param key the name of the key that holds the array
   * @return a typed {@link ArrayInfo}, or {@code null} if the key does not exist
   * @since 8.0
   */
  ArrayInfo arinfo(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b> Returns the {@code FULL}
   * variant of the array metadata: the top-level fields plus the per-slice statistics block.
   * <p>
   * Time complexity: O(S) where S is the number of slices.
   * <p>
   * Wire: {@code ARINFO key FULL}
   * @param key the name of the key that holds the array
   * @return a typed {@link ArrayFullInfo}, or {@code null} if the key does not exist
   * @since 8.0
   */
  ArrayFullInfo arinfoFull(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arinsert">ARINSERT Command</a></b> Inserts one or more
   * values at consecutive indices, beginning at the current insert cursor position. The cursor
   * advances by one for each value inserted.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * <p>
   * Wire: {@code ARINSERT key value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param values one or more values to insert
   * @return the last index at which a value was inserted
   * @since 8.0
   */
  long arinsert(byte[] key, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arinsert">ARINSERT Command</a></b> Convenience
   * single-value overload of {@link #arinsert(byte[], byte[]...)}.
   * <p>
   * Wire: {@code ARINSERT key value}
   * @param key the name of the key that holds the array
   * @param value the value to insert at the current insert cursor
   * @return the index at which the value was inserted
   * @since 8.0
   */
  long arinsert(byte[] key, byte[] value);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b> Returns the most
   * recently inserted elements in oldest-first order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * <p>
   * Wire: {@code ARLASTITEMS key count}
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @return the list of last-inserted values
   * @since 8.0
   */
  List<byte[]> arlastitems(byte[] key, long count);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b> Returns the most
   * recently inserted elements. When {@code rev} is {@code true}, elements are returned in reverse
   * chronological order (most recent first) instead of the default oldest-first order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * <p>
   * Wire: {@code ARLASTITEMS key count [REV]} ({@code REV} is appended when {@code rev} is
   * {@code true}).
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @param rev whether to request the {@code REV} variant
   * @return the list of last-inserted values
   * @since 8.0
   */
  List<byte[]> arlastitems(byte[] key, long count, boolean rev);

  /**
   * <b><a href="https://redis.io/commands/arlen">ARLEN Command</a></b> Returns the length of an
   * array (max index + 1).
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARLEN key}
   * @param key the name of the key that holds the array
   * @return the array length, or {@code 0} if the key does not exist
   * @since 8.0
   */
  long arlen(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/armget">ARMGET Command</a></b> Returns the values at
   * multiple indices. The reply preserves the order of the requested indices and contains
   * {@code null} for any index that is not set.
   * <p>
   * Time complexity: O(N) where N is the number of requested indices.
   * <p>
   * Wire: {@code ARMGET key index1 [index2 ...]}
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to retrieve
   * @return the list of values aligned with {@code indices}
   * @since 8.0
   */
  List<byte[]> armget(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/armset">ARMSET Command</a></b> Sets multiple index-value
   * pairs. Pairs may be non-contiguous and in any order; the map's iteration order is used as the
   * wire order.
   * <p>
   * Time complexity: O(N) where N is the number of pairs.
   * <p>
   * Wire: {@code ARMSET key index1 value1 [index2 value2 ...]}
   * @param key the name of the key that holds the array
   * @param indexValueMap the index-to-value pairs to write
   * @return the number of slots that were previously empty
   * @since 8.0
   */
  long armset(byte[] key, Map<Long, byte[]> indexValueMap);

  /**
   * <b><a href="https://redis.io/commands/arnext">ARNEXT Command</a></b> Returns the next index
   * that {@code ARINSERT} would use.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARNEXT key}
   * @param key the name of the key that holds the array
   * @return {@link OptionalLong#of(long) OptionalLong.of(0)} for a missing key or when no insert
   *         has happened yet; the next insert index wrapped in {@link OptionalLong} when one is
   *         available; {@link OptionalLong#empty()} when the insertion cursor is exhausted
   * @since 8.0
   */
  OptionalLong arnext(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Applies a bitwise operation
   * ({@link ArrayBitwise#AND AND}, {@link ArrayBitwise#OR OR}, {@link ArrayBitwise#XOR XOR}) over
   * the non-empty elements in the inclusive {@code [start, end]} range.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end AND|OR|XOR}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @param op the bitwise operator to apply
   * @return the operation's numeric result
   * @since 8.0
   */
  long aropBitwise(byte[] key, long start, long end, ArrayBitwise op);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Applies a numeric aggregate
   * ({@link ArrayAggregate#SUM SUM}, {@link ArrayAggregate#MIN MIN}, {@link ArrayAggregate#MAX
   * MAX}) over the non-empty elements in the inclusive {@code [start, end]} range.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end SUM|MIN|MAX}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @param op the aggregate operator to apply
   * @return the aggregate value as raw bytes, or {@code null} when the range is empty
   * @since 8.0
   */
  byte[] aropAggregate(byte[] key, long start, long end, ArrayAggregate op);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Counts the number of non-empty
   * elements in the inclusive {@code [start, end]} range using the {@code USED} subcommand.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end USED}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @return the count of non-empty elements
   * @since 8.0
   */
  long aropCount(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Counts elements in the
   * inclusive {@code [start, end]} range whose value equals {@code match} using the {@code MATCH}
   * subcommand.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end MATCH match}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @param match the value to match
   * @return the count of matching elements
   * @since 8.0
   */
  long aropCount(byte[] key, long start, long end, byte[] match);

  /**
   * <b><a href="https://redis.io/commands/arring">ARRING Command</a></b> Inserts one or more values
   * into an array as a fixed-size ring buffer. Each value is placed at the next ring position and
   * the cursor advances accordingly.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * <p>
   * Wire: {@code ARRING key size value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param size the ring buffer window size
   * @param values one or more values to insert
   * @return the last index at which a value was inserted
   * @since 8.0
   */
  long arring(byte[] key, long size, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arring">ARRING Command</a></b> Convenience single-value
   * overload of {@link #arring(byte[], long, byte[]...)}.
   * <p>
   * Wire: {@code ARRING key size value}
   * @param key the name of the key that holds the array
   * @param size the ring buffer window size
   * @param value the value to insert
   * @return the index at which the value was inserted
   * @since 8.0
   */
  long arring(byte[] key, long size, byte[] value);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b> Iterates existing elements
   * in an inclusive index range and returns the index/value pairs in traversal order. Empty slots
   * are excluded.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements.
   * <p>
   * Wire: {@code ARSCAN key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the iteration is reversed
   * @param end zero-based end index (inclusive)
   * @return the populated {@code (index, value)} pairs; empty when the key does not exist
   * @since 8.0
   */
  List<KeyValue<Long, byte[]>> arscan(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b> Iterates existing elements
   * in an inclusive index range with a cap on the number of pairs returned. Empty slots are
   * excluded.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements returned.
   * <p>
   * Wire: {@code ARSCAN key start end LIMIT limit}
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the iteration is reversed
   * @param end zero-based end index (inclusive)
   * @param limit cap on the number of returned populated elements
   * @return the populated {@code (index, value)} pairs
   * @since 8.0
   */
  List<KeyValue<Long, byte[]>> arscan(byte[] key, long start, long end, long limit);

  /**
   * <b><a href="https://redis.io/commands/arseek">ARSEEK Command</a></b> Sets the insert cursor of
   * an array to a specific index.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARSEEK key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index to set as the new insert cursor
   * @return {@code 1} if the cursor was set, {@code 0} if the key does not exist
   * @since 8.0
   */
  long arseek(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/arset">ARSET Command</a></b> Sets one or more contiguous
   * values starting at {@code index}. When multiple values are provided, they are stored at
   * consecutive indices.
   * <p>
   * Time complexity: O(N) where N is the number of values written.
   * <p>
   * Wire: {@code ARSET key index value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param index zero-based starting index
   * @param values one or more values to write at consecutive indices
   * @return the number of slots that were previously empty
   * @since 8.0
   */
  long arset(byte[] key, long index, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arset">ARSET Command</a></b> Convenience single-value
   * overload of {@link #arset(byte[], long, byte[]...)}.
   * <p>
   * Wire: {@code ARSET key index value}
   * @param key the name of the key that holds the array
   * @param index zero-based index at which to store the value
   * @param value the value to write
   * @return the number of slots that were previously empty (0 or 1)
   * @since 8.0
   */
  long arset(byte[] key, long index, byte[] value);

}
