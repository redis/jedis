package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.KeyValue;

public interface ListBinaryCommands {

  long rpush(byte[] key, byte[]... args);

  long lpush(byte[] key, byte[]... args);

  long llen(byte[] key);

  List<byte[]> lrange(byte[] key, long start, long stop);

  String ltrim(byte[] key, long start, long stop);

  byte[] lindex(byte[] key, long index);

  String lset(byte[] key, long index, byte[] value);

  long lrem(byte[] key, long count, byte[] value);

  byte[] lpop(byte[] key);

  List<byte[]> lpop(byte[] key, int count);

  Long lpos(byte[] key, byte[] element);

  Long lpos(byte[] key, byte[] element, LPosParams params);

  List<Long> lpos(byte[] key, byte[] element, LPosParams params, long count);

  byte[] rpop(byte[] key);

  List<byte[]> rpop(byte[] key, int count);

  long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value);

  long lpushx(byte[] key, byte[]... args);

  long rpushx(byte[] key, byte[]... args);

  List<byte[]> blpop(int timeout, byte[]... keys);

  List<byte[]> blpop(double timeout, byte[]... keys);

  List<byte[]> brpop(int timeout, byte[]... keys);

  List<byte[]> brpop(double timeout, byte[]... keys);

  byte[] rpoplpush(byte[] srckey, byte[] dstkey);

  byte[] brpoplpush(byte[] source, byte[] destination, int timeout);

  byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to);

  byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout);

  KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, byte[]... keys);

  KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, int count, byte[]... keys);

  KeyValue<byte[], List<byte[]>> blmpop(long timeout, ListDirection direction, byte[]... keys);

  KeyValue<byte[], List<byte[]>> blmpop(long timeout, ListDirection direction, int count, byte[]... keys);
}
