package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import redis.clients.jedis.Response;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.ArrayAggregate;
import redis.clients.jedis.args.ArrayBitwise;
import redis.clients.jedis.args.LongRange;
import redis.clients.jedis.params.ArgrepParams;
import redis.clients.jedis.resps.ArrayFullInfo;
import redis.clients.jedis.resps.ArrayInfo;
import redis.clients.jedis.util.KeyValue;

/**
 * Binary pipeline and transaction commands for the Redis <b>array</b> data type.
 */
public interface ArrayPipelineBinaryCommands {

  /**
   * <b><a href="https://redis.io/commands/arcount">ARCOUNT Command</a></b> Returns the number of
   * non-empty elements in an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARCOUNT key}
   * @param key the name of the key that holds the array
   * @return a {@link Response} that resolves to the number of non-empty elements, or {@code 0} if
   *         {@code key} does not exist
   * @since 8.0
   */
  Response<Long> arcount(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/ardel">ARDEL Command</a></b> Deletes the element at a
   * single index in an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARDEL key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index to delete
   * @return a {@link Response} that resolves to {@code 1} if an element was deleted, {@code 0}
   *         otherwise
   * @since 8.0
   */
  Response<Long> ardel(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/ardel">ARDEL Command</a></b> Deletes elements at the
   * specified indices in an array.
   * <p>
   * Time complexity: O(N) where N is the number of indices supplied.
   * <p>
   * Wire: {@code ARDEL key index1 [index2 ...]}
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to delete
   * @return a {@link Response} that resolves to the number of elements actually deleted
   * @since 8.0
   */
  Response<Long> ardel(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/ardelrange">ARDELRANGE Command</a></b> Deletes elements
   * in one or more inclusive {@link LongRange} ranges of indices.
   * <p>
   * Time complexity: O(M + N) where M is the number of ranges and N the total number of elements
   * they cover.
   * <p>
   * Wire: {@code ARDELRANGE key r1.start r1.end [r2.start r2.end ...]}
   * @param key the name of the key that holds the array
   * @param ranges one or more inclusive index ranges
   * @return a {@link Response} that resolves to the number of elements deleted
   * @since 8.0
   */
  Response<Long> ardelrange(byte[] key, LongRange... ranges);

  /**
   * <b><a href="https://redis.io/commands/ardelrange">ARDELRANGE Command</a></b> Convenience
   * single-range overload of {@link #ardelrange(byte[], LongRange...)}.
   * <p>
   * Wire: {@code ARDELRANGE key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @return a {@link Response} that resolves to the number of elements deleted
   * @since 8.0
   */
  Response<Long> ardelrange(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arget">ARGET Command</a></b> Returns the value stored at
   * a single index in an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARGET key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index of the element to retrieve
   * @return a {@link Response} that resolves to the value at the given index, or {@code null} if
   *         the key or index does not exist
   * @since 8.0
   */
  Response<byte[]> arget(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/argetrange">ARGETRANGE Command</a></b> Returns the values
   * at a contiguous range of indices.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the range.
   * <p>
   * Wire: {@code ARGETRANGE key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index of the range
   * @param end zero-based end index of the range (inclusive)
   * @return a {@link Response} that resolves to the list of values in traversal order; empty slots
   *         appear as {@code null} entries
   * @since 8.0
   */
  Response<List<byte[]>> argetrange(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/argrep">ARGREP Command</a></b> Searches array elements
   * within an inclusive index range (carried by {@link ArgrepParams}) using one or more textual
   * predicates and returns the matching indices.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code ARGREP key start end <params>}
   * @param key the name of the key that holds the array
   * @param params the search range, predicates and options to apply
   * @return a {@link Response} that resolves to the matching indices in traversal order
   * @since 8.0
   */
  @Experimental
  Response<List<Long>> argrep(byte[] key, ArgrepParams params);

  /**
   * <b><a href="https://redis.io/commands/argrep">ARGREP Command</a></b> Searches array elements
   * within an inclusive index range (carried by {@link ArgrepParams}) and returns the matching
   * index/value pairs.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code ARGREP key start end <params> WITHVALUES}
   * @param key the name of the key that holds the array
   * @param params the search range, predicates and options to apply
   * @return a {@link Response} that resolves to the matching index/value pairs
   * @since 8.0
   */
  @Experimental
  Response<List<KeyValue<Long, byte[]>>> argrepWithValues(byte[] key, ArgrepParams params);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b> Returns metadata
   * describing an array.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARINFO key}
   * @param key the name of the key that holds the array
   * @return a {@link Response} that resolves to a typed {@link ArrayInfo}, or {@code null} if the
   *         key does not exist
   * @since 8.0
   */
  Response<ArrayInfo> arinfo(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arinfo">ARINFO Command</a></b> Returns the {@code FULL}
   * variant of the array metadata.
   * <p>
   * Time complexity: O(S) where S is the number of slices.
   * <p>
   * Wire: {@code ARINFO key FULL}
   * @param key the name of the key that holds the array
   * @return a {@link Response} that resolves to a typed {@link ArrayFullInfo}, or {@code null} if
   *         the key does not exist
   * @since 8.0
   */
  Response<ArrayFullInfo> arinfoFull(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arinsert">ARINSERT Command</a></b> Inserts one or more
   * values at consecutive indices beginning at the current insert cursor position.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * <p>
   * Wire: {@code ARINSERT key value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param values one or more values to insert
   * @return a {@link Response} that resolves to the last index at which a value was inserted
   * @since 8.0
   */
  Response<Long> arinsert(byte[] key, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arinsert">ARINSERT Command</a></b> Convenience
   * single-value overload of {@link #arinsert(byte[], byte[]...)}.
   * <p>
   * Wire: {@code ARINSERT key value}
   * @param key the name of the key that holds the array
   * @param value the value to insert
   * @return a {@link Response} that resolves to the index at which the value was inserted
   * @since 8.0
   */
  Response<Long> arinsert(byte[] key, byte[] value);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b> Returns the most
   * recently inserted elements in oldest-first order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * <p>
   * Wire: {@code ARLASTITEMS key count}
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @return a {@link Response} that resolves to the list of last-inserted values
   * @since 8.0
   */
  Response<List<byte[]>> arlastitems(byte[] key, long count);

  /**
   * <b><a href="https://redis.io/commands/arlastitems">ARLASTITEMS Command</a></b> Returns the most
   * recently inserted elements. When {@code rev} is {@code true}, elements are returned in reverse
   * chronological order.
   * <p>
   * Time complexity: O(N) where N is the number of returned elements.
   * <p>
   * Wire: {@code ARLASTITEMS key count [REV]}
   * @param key the name of the key that holds the array
   * @param count the maximum number of elements to return
   * @param rev whether to request the {@code REV} variant
   * @return a {@link Response} that resolves to the list of last-inserted values
   * @since 8.0
   */
  Response<List<byte[]>> arlastitems(byte[] key, long count, boolean rev);

  /**
   * <b><a href="https://redis.io/commands/arlen">ARLEN Command</a></b> Returns the length of an
   * array (max index + 1).
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARLEN key}
   * @param key the name of the key that holds the array
   * @return a {@link Response} that resolves to the array length, or {@code 0} if the key does not
   *         exist
   * @since 8.0
   */
  Response<Long> arlen(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/armget">ARMGET Command</a></b> Returns the values at
   * multiple indices.
   * <p>
   * Time complexity: O(N) where N is the number of requested indices.
   * <p>
   * Wire: {@code ARMGET key index1 [index2 ...]}
   * @param key the name of the key that holds the array
   * @param indices one or more zero-based indices to retrieve
   * @return a {@link Response} that resolves to the list of values aligned with {@code indices};
   *         missing slots appear as {@code null}
   * @since 8.0
   */
  Response<List<byte[]>> armget(byte[] key, long... indices);

  /**
   * <b><a href="https://redis.io/commands/armset">ARMSET Command</a></b> Sets multiple index-value
   * pairs.
   * <p>
   * Time complexity: O(N) where N is the number of pairs.
   * <p>
   * Wire: {@code ARMSET key index1 value1 [index2 value2 ...]}
   * @param key the name of the key that holds the array
   * @param indexValueMap the index-to-value pairs to write
   * @return a {@link Response} that resolves to the number of slots that were previously empty
   * @since 8.0
   */
  Response<Long> armset(byte[] key, Map<Long, byte[]> indexValueMap);

  /**
   * <b><a href="https://redis.io/commands/arnext">ARNEXT Command</a></b> Returns the next index
   * that {@code ARINSERT} would use.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARNEXT key}
   * @param key the name of the key that holds the array
   * @return a {@link Response} that resolves to {@link OptionalLong#of(long) OptionalLong.of(0)}
   *         for missing keys, the next insert index wrapped in {@link OptionalLong} when one is
   *         available, or {@link OptionalLong#empty()} when the cursor is exhausted
   * @since 8.0
   */
  Response<Long> arnext(byte[] key);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Applies a bitwise operation
   * over the non-empty elements in the inclusive {@code [start, end]} range.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end AND|OR|XOR}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @param op the bitwise operator to apply
   * @return a {@link Response} that resolves to the operation's numeric result
   * @since 8.0
   */
  Response<Long> aropBitwise(byte[] key, long start, long end, ArrayBitwise op);

  /**
   * <b><a href="https://redis.io/commands/arop">AROP Command</a></b> Applies a numeric aggregate
   * over the non-empty elements in the inclusive {@code [start, end]} range.
   * <p>
   * Time complexity: O(N) where N is the number of elements scanned.
   * <p>
   * Wire: {@code AROP key start end SUM|MIN|MAX}
   * @param key the name of the key that holds the array
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @param op the aggregate operator to apply
   * @return a {@link Response} that resolves to the aggregate value as raw bytes, or {@code null}
   *         when the range is empty
   * @since 8.0
   */
  Response<byte[]> aropAggregate(byte[] key, long start, long end, ArrayAggregate op);

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
   * @return a {@link Response} that resolves to the count of non-empty elements
   * @since 8.0
   */
  Response<Long> aropCount(byte[] key, long start, long end);

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
   * @return a {@link Response} that resolves to the count of matching elements
   * @since 8.0
   */
  Response<Long> aropCount(byte[] key, long start, long end, byte[] match);

  /**
   * <b><a href="https://redis.io/commands/arring">ARRING Command</a></b> Inserts one or more values
   * into an array as a fixed-size ring buffer.
   * <p>
   * Time complexity: O(N) where N is the number of values inserted.
   * <p>
   * Wire: {@code ARRING key size value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param size the ring buffer window size
   * @param values one or more values to insert
   * @return a {@link Response} that resolves to the last index at which a value was inserted
   * @since 8.0
   */
  Response<Long> arring(byte[] key, long size, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arring">ARRING Command</a></b> Convenience single-value
   * overload of {@link #arring(byte[], long, byte[]...)}.
   * <p>
   * Wire: {@code ARRING key size value}
   * @param key the name of the key that holds the array
   * @param size the ring buffer window size
   * @param value the value to insert
   * @return a {@link Response} that resolves to the index at which the value was inserted
   * @since 8.0
   */
  Response<Long> arring(byte[] key, long size, byte[] value);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b> Iterates existing elements
   * in an inclusive index range and returns the index/value pairs.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements.
   * <p>
   * Wire: {@code ARSCAN key start end}
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the iteration is reversed
   * @param end zero-based end index (inclusive)
   * @return a {@link Response} that resolves to the populated {@code (index, value)} pairs
   * @since 8.0
   */
  Response<List<KeyValue<Long, byte[]>>> arscan(byte[] key, long start, long end);

  /**
   * <b><a href="https://redis.io/commands/arscan">ARSCAN Command</a></b> Iterates existing elements
   * in an inclusive index range with a cap on the number of pairs returned.
   * <p>
   * Time complexity: O(N) where N is the number of populated elements returned.
   * <p>
   * Wire: {@code ARSCAN key start end LIMIT limit}
   * @param key the name of the key that holds the array
   * @param start zero-based start index; when {@code start > end} the iteration is reversed
   * @param end zero-based end index (inclusive)
   * @param limit cap on the number of returned populated elements
   * @return a {@link Response} that resolves to the populated {@code (index, value)} pairs
   * @since 8.0
   */
  Response<List<KeyValue<Long, byte[]>>> arscan(byte[] key, long start, long end, long limit);

  /**
   * <b><a href="https://redis.io/commands/arseek">ARSEEK Command</a></b> Sets the insert cursor of
   * an array to a specific index.
   * <p>
   * Time complexity: O(1)
   * <p>
   * Wire: {@code ARSEEK key index}
   * @param key the name of the key that holds the array
   * @param index the zero-based index to set as the new insert cursor
   * @return a {@link Response} that resolves to {@code 1} if the cursor was set, {@code 0} if the
   *         key does not exist
   * @since 8.0
   */
  Response<Long> arseek(byte[] key, long index);

  /**
   * <b><a href="https://redis.io/commands/arset">ARSET Command</a></b> Sets one or more contiguous
   * values starting at {@code index}.
   * <p>
   * Time complexity: O(N) where N is the number of values written.
   * <p>
   * Wire: {@code ARSET key index value1 [value2 ...]}
   * @param key the name of the key that holds the array
   * @param index zero-based starting index
   * @param values one or more values to write at consecutive indices
   * @return a {@link Response} that resolves to the number of slots that were previously empty
   * @since 8.0
   */
  Response<Long> arset(byte[] key, long index, byte[]... values);

  /**
   * <b><a href="https://redis.io/commands/arset">ARSET Command</a></b> Convenience single-value
   * overload of {@link #arset(byte[], long, byte[]...)}.
   * <p>
   * Wire: {@code ARSET key index value}
   * @param key the name of the key that holds the array
   * @param index zero-based index at which to store the value
   * @param value the value to write
   * @return a {@link Response} that resolves to the number of slots that were previously empty
   * @since 8.0
   */
  Response<Long> arset(byte[] key, long index, byte[] value);

}
