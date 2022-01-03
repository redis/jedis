package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;

public interface ListPipelineBinaryCommands {

  Response<Long> rpush(byte[] key, byte[]... args);

  Response<Long> lpush(byte[] key, byte[]... args);

  Response<Long> llen(byte[] key);

  Response<List<byte[]>> lrange(byte[] key, long start, long stop);

  Response<String> ltrim(byte[] key, long start, long stop);

  Response<byte[]> lindex(byte[] key, long index);

  Response<String> lset(byte[] key, long index, byte[] value);

  Response<Long> lrem(byte[] key, long count, byte[] value);

  Response<byte[]> lpop(byte[] key);

  Response<List<byte[]>> lpop(byte[] key, int count);

  Response<Long> lpos(byte[] key, byte[] element);

  Response<Long> lpos(byte[] key, byte[] element, LPosParams params);

  Response<List<Long>> lpos(byte[] key, byte[] element, LPosParams params, long count);

  Response<byte[]> rpop(byte[] key);

  Response<List<byte[]>> rpop(byte[] key, int count);

  Response<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value);

  Response<Long> lpushx(byte[] key, byte[]... arg);

  Response<Long> rpushx(byte[] key, byte[]... arg);

  Response<List<byte[]>> blpop(int timeout, byte[]... keys);

  Response<List<byte[]>> blpop(double timeout, byte[]... keys);

  Response<List<byte[]>> brpop(int timeout, byte[]... keys);

  Response<List<byte[]>> brpop(double timeout, byte[]... keys);

  Response<byte[]> rpoplpush(byte[] srckey, byte[] dstkey);

  Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout);

  Response<byte[]> lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to);

  Response<byte[]> blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout);

  /**
   * Pops one element from the first non-empty list key from the list of provided key names.
   *
   * @param from LEFT|RIGHT
   * @param keys key of list
   * @return element from the first non-empty list key from the list of provided key names
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT<a/>
   */
  Response<Map<byte[], List<byte[]>>> lmpop(ListDirection from, byte[]... keys);

  /**
   * Pops one or more elements from the first non-empty list key from the list of provided key names.
   *
   * @param from  LEFT|RIGHT
   * @param count count of pop elements
   * @param keys  key of list
   * @return elements from the first non-empty list key from the list of provided key names.
   * @see <a href="https://redis.io/commands/lmpop">LMPOP numkeys key [key ...] LEFT|RIGHT COUNT count<a/>
   */
  Response<Map<byte[], List<byte[]>>> lmpop(ListDirection from, int count, byte[]... keys);

}
