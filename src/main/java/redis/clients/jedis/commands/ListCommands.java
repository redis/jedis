package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.resps.KeyedListElement;
import redis.clients.jedis.util.KeyValue;

public interface ListCommands {

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings data to push
   * @return The number of elements inside the list after the push operation
   */
  long rpush(String key, String... strings);

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings data to push
   * @return The number of elements inside the list after the push operation
   */
  long lpush(String key, String... strings);

  /**
   * Return the length of the list stored at the specified key. If the key does not exist zero is
   * returned (the same behaviour as for empty lists). If the value stored at key is not a list an
   * error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The length of the list
   */
  long llen(String key);

  /**
   * Return the specified elements of the list stored at the specified key. Start and end are
   * zero-based indexes. 0 is the first element of the list (the list head), 1 the next element and
   * so on.
   * <p>
   * For example LRANGE foobar 0 2 will return the first three elements of the list.
   * <p>
   * start and end can also be negative numbers indicating offsets from the end of the list. For
   * example -1 is the last element of the list, -2 the penultimate element and so on.
   * <p>
   * <b>Consistency with range functions in various programming languages</b>
   * <p>
   * Note that if you have a list of numbers from 0 to 100, LRANGE 0 10 will return 11 elements,
   * that is, rightmost item is included. This may or may not be consistent with behavior of
   * range-related functions in your programming language of choice (think Ruby's Range.new,
   * Array#slice or Python's range() function).
   * <p>
   * LRANGE behavior is consistent with one of Tcl.
   * <p>
   * <b>Out-of-range indexes</b>
   * <p>
   * Indexes out of range will not produce an error: if start is over the end of the list, or start
   * &gt; end, an empty list is returned. If end is over the end of the list Redis will threat it
   * just like the last element of the list.
   * <p>
   * Time complexity: O(start+n) (with n being the length of the range and start being the start
   * offset)
   * @param key
   * @param start
   * @param stop
   * @return A list of elements in the specified range
   */
  List<String> lrange(String key, long start, long stop);

  /**
   * Trim an existing list so that it will contain only the specified range of elements specified.
   * Start and end are zero-based indexes. 0 is the first element of the list (the list head), 1 the
   * next element and so on.
   * <p>
   * For example LTRIM foobar 0 2 will modify the list stored at foobar key so that only the first
   * three elements of the list will remain.
   * <p>
   * start and end can also be negative numbers indicating offsets from the end of the list. For
   * example -1 is the last element of the list, -2 the penultimate element and so on.
   * <p>
   * Indexes out of range will not produce an error: if start is over the end of the list, or start
   * &gt; end, an empty list is left as value. If end over the end of the list Redis will threat it
   * just like the last element of the list.
   * <p>
   * Hint: the obvious use of LTRIM is together with LPUSH/RPUSH. For example:
   * <p>
   * {@code lpush("mylist", "someelement"); ltrim("mylist", 0, 99); * }
   * <p>
   * The above two commands will push elements in the list taking care that the list will not grow
   * without limits. This is very useful when using Redis to store logs for example. It is important
   * to note that when used in this way LTRIM is an O(1) operation because in the average case just
   * one element is removed from the tail of the list.
   * <p>
   * Time complexity: O(n) (with n being len of list - len of range)
   * @param key
   * @param start
   * @param stop
   * @return OK
   */
  String ltrim(String key, long start, long stop);

  /**
   * Returns the element at index in the list stored at key.  0 is the first element, 1 the second
   * and so on. Negative indexes are supported, for example -1 is the last element, -2 the penultimate
   * and so on.
   * <p>
   * If the value stored at key is not of list type an error is returned. If the index is out of
   * range a 'nil' reply is returned.
   * <p>
   * Note that even if the average time complexity is O(n) asking for the first or the last element
   * of the list is O(1).
   * <p>
   * Time complexity: O(n) (with n being the length of the list)
   * @param key
   * @param index
   * @return The requested element
   */
  String lindex(String key, long index);

  /**
   * Set a new value as the element at index position of the List at key.
   * <p>
   * Out of range indexes will generate an error.
   * <p>
   * Similarly to other list commands accepting indexes, the index can be negative to access
   * elements starting from the end of the list. So -1 is the last element, -2 is the penultimate,
   * and so forth.
   * <p>
   * Time Complexity O(N) when N being the length of the list. For the first or last elements of
   * the list is O(1)
   * @param key
   * @param index
   * @param value
   * @return OK
   */
  String lset(String key, long index, String value);

  /**
   * Remove the first count occurrences of the value element from the list. If count is zero all the
   * elements are removed. If count is negative elements are removed from tail to head, instead to
   * go from head to tail that is the normal behaviour. So for example LREM with count -2 and hello
   * as value to remove against the list (a,b,c,hello,x,hello,hello) will leave the list
   * (a,b,c,hello,x). The number of removed elements is returned as an integer, see below for more
   * information about the returned value. Note that non existing keys are considered like empty
   * lists by LREM, so LREM against non existing keys will always return 0.
   * <p>
   * Time complexity: O(N) (with N being the length of the list)
   * @param key
   * @param count
   * @param value
   * @return The number of removed elements if the operation succeeded
   */
  long lrem(String key, long count, String value);

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned.
   * @param key
   * @return The popped element
   */
  String lpop(String key);

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * @param key
   * @param count
   * @return A list of popped elements, or 'nil' when key does not exist
   */
  List<String> lpop(String key, int count);

  /**
   * Returns the index of the first matching element inside a redis list. If the element is found,
   * its index (the zero-based position in the list) is returned. Otherwise, if no match is found,
   * 'nil' is returned.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the list
   * @param key
   * @param element
   * @return The index of first matching element in the list. Value will be 'nil' when the element
   * is not present in the list
   */
  Long lpos(String key, String element);

  /**
   * In case there are multiple matches Rank option specifies the "rank" of the element to return.
   * A rank of 1 returns the first match, 2 to return the second match, and so forth.
   * If list `foo` has elements ("a","b","c","1","2","3","c","c"), The function call to get the
   * index of second occurrence of "c" will be as follows lpos("foo","c", LPosParams.lPosParams().rank(2)).
   * <p>
   * Maxlen option compares the element provided only with a given maximum number of list items.
   * A value of 1000 will make sure that the command performs only 1000 comparisons. The
   * comparison is made for the first part or the last part depending on the fact we use a positive or
   * negative rank.
   * Following is how we could use the Maxlen option lpos("foo", "b", LPosParams.lPosParams().rank(1).maxlen(2)).
   * @param key
   * @param element
   * @param params {@link LPosParams}
   * @return The integer representing the matching element, or 'nil' if there is no match
   */
  Long lpos(String key, String element, LPosParams params);

  /**
   * Returns the index of matching elements inside a Redis list. If the element is found, its index
   * (the zero-based position in the list) is returned. Otherwise, if no match is found, nil is returned.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the list
   * @param key
   * @param element
   * @param params {@link LPosParams}
   * @param count
   * @return A list containing position of the matching elements inside the list
   */
  List<Long> lpos(String key, String element, LPosParams params, long count);

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * @param key
   * @return The popped element
   */
  String rpop(String key);

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * @param key
   * @param count return up to count elements
   * @return A list of count popped elements, or 'nil' when key does not exist.
   */
  List<String> rpop(String key, int count);

  /**
   * Inserts element in the list stored at key either before or after the reference value pivot.
   * <p>
   * When key does not exist, it is considered an empty list and no operation is performed.
   * @param key
   * @param where can be  BEFORE or AFTER
   * @param pivot reference value
   * @param value the value
   * @return The length of the list after the insert operation, or -1 when the value pivot was not found
   */
  long linsert(String key, ListPosition where, String pivot, String value);

  /**
   * Inserts specified values at the head of the list stored at key. In contrary to {@link ListBinaryCommands#lpush(byte[], byte[]...) LPUSH},
   * no operation will be performed when key does not yet exist.
   * @param key
   * @param strings the strings to push
   * @return The length of the list after the push operation
   */
  long lpushx(String key, String... strings);

  /**
   * Inserts specified values at the tail of the list stored at key. In contrary to {@link ListBinaryCommands#rpush(byte[], byte[]...) RPUSH},
   * no operation will be performed when key does not yet exist.
   * @param key
   * @param strings the strings to push
   * @return The length of the list after the push operation
   */
  long rpushx(String key, String... strings);

  /**
   * The blocking version of {@link ListCommands#lpop(String)} LPOP} because it blocks the connection
   * when there are no elements to pop from any of the given lists. An element is popped from the head of
   * the first list that is non-empty, with the given keys being checked in the order that they are given.
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @param keys
   */
  List<String> blpop(int timeout, String... keys);

  /**
   * @see ListCommands#blpop(int, String...)
   */
  List<String> blpop(int timeout, String key);

  /**
   * The blocking version of {@link ListCommands#lpop(String)} LPOP} because it blocks the connection
   * when there are no elements to pop from any of the given lists. An element is popped from the head of
   * the first list that is non-empty, with the given keys being checked in the order that they are given.
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @param keys
   */
  KeyedListElement blpop(double timeout, String... keys);


  /**
   * @see ListCommands#blpop(double, String...)
   */
  KeyedListElement blpop(double timeout, String key);

  /**
   * The blocking version of {@link ListCommands#rpop(String)} RPOP} because it blocks the connection
   * when there are no elements to pop from any of the given lists. An element is popped from the tail of
   * the first list that is non-empty, with the given keys being checked in the order that they are given.
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @param keys
   */
  List<String> brpop(int timeout, String... keys);

  /**
   * @see ListCommands#brpop(int, String...)
   */
  List<String> brpop(int timeout, String key);

  /**
   * The blocking version of {@link ListCommands#rpop(String)} RPOP} because it blocks the connection
   * when there are no elements to pop from any of the given lists. An element is popped from the tail of
   * the first list that is non-empty, with the given keys being checked in the order that they are given.
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @param keys
   */
  KeyedListElement brpop(double timeout, String... keys);

  /**
   * @see ListCommands#brpop(double, String...)
   */
  KeyedListElement brpop(double timeout, String key);

  /**
   * Atomically return and remove the last (tail) element of the srckey list, and push the element
   * as the first (head) element of the dstkey list. For example if the source list contains the
   * elements "a","b","c" and the destination list contains the elements "foo","bar" after an
   * RPOPLPUSH command the content of the two lists will be "a","b" and "c","foo","bar".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned. If
   * the srckey and dstkey are the same the operation is equivalent to removing the last element
   * from the list and pushing it as first element of the list, so it's a "list rotation" command.
   * <p>
   * Time complexity: O(1)
   * @param srckey
   * @param dstkey
   * @return Bulk reply
   */
  String rpoplpush(String srckey, String dstkey);

  /**
   * The blocking variant of {@link ListCommands#rpoplpush(String, String)}. When source is
   * empty, Redis will block the connection until another client pushes to it or until timeout is
   * reached. A timeout of zero can be used to block indefinitely.
   * <p>
   * Time complexity: O(1)
   * @param source
   * @param destination
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @return The element being popped from source and pushed to destination
   */
  String brpoplpush(String source, String destination, int timeout);

  /**
   * Pop an element from a list, push it to another list and return it
   * @param srcKey
   * @param dstKey
   * @param from can be LEFT or RIGHT
   * @param to can be LEFT or RIGHT
   * @return The element being popped and pushed
   */
  String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  /**
   * Pop an element from a list, push it to another list and return it; or block until one is available
   * @param srcKey
   * @param dstKey
   * @param from can be LEFT or RIGHT
   * @param to can be LEFT or RIGHT
   * @param timeout the timeout argument is interpreted as a double value specifying the maximum number of
   *               seconds to block. A timeout of zero can be used to block indefinitely.
   * @return The element being popped and pushed
   */
  String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout);

  KeyValue<String, List<String>> lmpop(ListDirection direction, String... keys);

  KeyValue<String, List<String>> lmpop(ListDirection direction, int count, String... keys);

  KeyValue<String, List<String>> blmpop(long timeout, ListDirection direction, String... keys);

  KeyValue<String, List<String>> blmpop(long timeout, ListDirection direction, int count, String... keys);
}
