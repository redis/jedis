package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.resps.KeyedListElement;

public interface ListCommands {

  long rpush(String key, String... string);

  long lpush(String key, String... string);

  long llen(String key);

  List<String> lrange(String key, long start, long stop);

  String ltrim(String key, long start, long stop);

  String lindex(String key, long index);

  String lset(String key, long index, String value);

  long lrem(String key, long count, String value);

  String lpop(String key);

  List<String> lpop(String key, int count);

  Long lpos(String key, String element);

  Long lpos(String key, String element, LPosParams params);

  List<Long> lpos(String key, String element, LPosParams params, long count);

  String rpop(String key);

  List<String> rpop(String key, int count);

  long linsert(String key, ListPosition where, String pivot, String value);

  long lpushx(String key, String... string);

  long rpushx(String key, String... string);

  List<String> blpop(int timeout, String key);

  KeyedListElement blpop(double timeout, String key);

  List<String> brpop(int timeout, String key);

  KeyedListElement brpop(double timeout, String key);

  List<String> blpop(int timeout, String... keys);

  KeyedListElement blpop(double timeout, String... keys);

  List<String> brpop(int timeout, String... keys);

  KeyedListElement brpop(double timeout, String... keys);

  String rpoplpush(String srckey, String dstkey);

  String brpoplpush(String source, String destination, int timeout);

  String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout);

  /**
   * Pops one element from the first non-empty list key from the list of provided key names.
   *
   * @param from LEFT|RIGHT
   * @param keys key of list
   * @return element from the first non-empty list key from the list of provided key names
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT<a/>
   */
  Map<String, List<String>> lmpop(ListDirection from, String... keys);

  /**
   * Pops one or more elements from the first non-empty list key from the list of provided key names.
   *
   * @param from  LEFT|RIGHT
   * @param count count of pop elements
   * @param keys  key of list
   * @return elements from the first non-empty list key from the list of provided key names.
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT COUNT count<a/>
   */
  Map<String, List<String>> lmpop(ListDirection from, int count, String... keys);

}
