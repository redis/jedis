package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;

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

  long lpushx(byte[] key, byte[]... arg);

  long rpushx(byte[] key, byte[]... arg);

}
