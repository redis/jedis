package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.resps.KeyedListElement;

public interface ListPipelineCommands {

  Response<Long> rpush(String key, String... string);

  Response<Long> lpush(String key, String... string);

  Response<Long> llen(String key);

  Response<List<String>> lrange(String key, long start, long stop);

  Response<String> ltrim(String key, long start, long stop);

  Response<String> lindex(String key, long index);

  Response<String> lset(String key, long index, String value);

  Response<Long> lrem(String key, long count, String value);

  Response<String> lpop(String key);

  Response<List<String>> lpop(String key, int count);

  Response<Long> lpos(String key, String element);

  Response<Long> lpos(String key, String element, LPosParams params);

  Response<List<Long>> lpos(String key, String element, LPosParams params, long count);

  Response<String> rpop(String key);

  Response<List<String>> rpop(String key, int count);

  Response<Long> linsert(String key, ListPosition where, String pivot, String value);

  Response<Long> lpushx(String key, String... string);

  Response<Long> rpushx(String key, String... string);

  Response<List<String>> blpop(int timeout, String key);

  Response<KeyedListElement> blpop(double timeout, String key);

  Response<List<String>> brpop(int timeout, String key);

  Response<KeyedListElement> brpop(double timeout, String key);

  Response<List<String>> blpop(int timeout, String... keys);

  Response<KeyedListElement> blpop(double timeout, String... keys);

  Response<List<String>> brpop(int timeout, String... keys);

  Response<KeyedListElement> brpop(double timeout, String... keys);

  Response<String> rpoplpush(String srckey, String dstkey);

  Response<String> brpoplpush(String source, String destination, int timeout);

  Response<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  Response<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout);

  /**
   * Pops one element from the first non-empty list key from the list of provided key names.
   *
   * @param from LEFT|RIGHT
   * @param keys key of list
   * @return element from the first non-empty list key from the list of provided key names
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT<a/>
   */
  Response<Map<String, List<String>>> lmpop(ListDirection from, String... keys);

  /**
   * Pops one or more elements from the first non-empty list key from the list of provided key names.
   *
   * @param from  LEFT|RIGHT
   * @param count count of pop elements
   * @param keys  key of list
   * @return elements from the first non-empty list key from the list of provided key names.
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT COUNT count<a/>
   */
  Response<Map<String, List<String>>> lmpop(ListDirection from, int count, String... keys);

}
