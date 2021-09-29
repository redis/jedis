package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.BitPosParams;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.LCSMatchResult;

public interface StringCommands {

  String set(String key, String value);

  String set(String key, String value, SetParams params);

  String get(String key);

  String getDel(String key);

  String getEx(String key, GetExParams params);

  boolean setbit(String key, long offset, boolean value);

  boolean getbit(String key, long offset);

  long setrange(String key, long offset, String value);

  String getrange(String key, long startOffset, long endOffset);

  String getSet(String key, String value);

  long setnx(String key, String value);

  String setex(String key, long seconds, String value);

  String psetex(String key, long milliseconds, String value);

  List<String> mget(String... keys);

  String mset(String... keysvalues);

  long msetnx(String... keysvalues);

  long incr(String key);

  long incrBy(String key, long increment);

  double incrByFloat(String key, double increment);

  long decr(String key);

  long decrBy(String key, long decrement);

  long append(String key, String value);

  String substr(String key, int start, int end);

  long strlen(String key);

  long bitcount(String key);

  long bitcount(String key, long start, long end);

  long bitpos(String key, boolean value);

  long bitpos(String key, boolean value, BitPosParams params);

  List<Long> bitfield(String key, String...arguments);

  List<Long> bitfieldReadonly(String key, String...arguments);

  long bitop(BitOP op, String destKey, String... srcKeys);

  LCSMatchResult strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params);

}
