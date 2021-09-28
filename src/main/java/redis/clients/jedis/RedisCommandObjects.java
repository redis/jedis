package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.BitPosParams;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.stream.*;

public class RedisCommandObjects {

  protected CommandArguments commandArguments(ProtocolCommand command) {
    return new CommandArguments(command);
  }

  // Key commands
  public final CommandObject<Boolean> exists(String key) {
    return new CommandObject<>(commandArguments(Command.EXISTS).addKeyObject(key), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> exists(String... keys) {
    return new CommandObject<>(commandArguments(Command.EXISTS).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> persist(String key) {
    return new CommandObject<>(commandArguments(PERSIST).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<String> type(String key) {
    return new CommandObject<>(commandArguments(Command.TYPE).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> dump(String key) {
    return new CommandObject<>(commandArguments(DUMP).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<String> restore(String key, long ttl, byte[] serializedValue) {
    return new CommandObject<>(commandArguments(RESTORE).addKeyObject(key).addObject(ttl)
        .addObject(serializedValue), BuilderFactory.STRING);
  }

  public final CommandObject<String> restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return new CommandObject<>(commandArguments(RESTORE).addKeyObject(key).addObject(ttl)
        .addObject(serializedValue).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<Long> expire(String key, long seconds) {
    return new CommandObject<>(commandArguments(EXPIRE).addKeyObject(key).addObject(seconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpire(String key, long milliseconds) {
    return new CommandObject<>(commandArguments(PEXPIRE).addKeyObject(key).addObject(milliseconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expireAt(String key, long unixTime) {
    return new CommandObject<>(commandArguments(EXPIREAT).addKeyObject(key).addObject(unixTime), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return new CommandObject<>(commandArguments(PEXPIREAT).addKeyObject(key).addObject(millisecondsTimestamp), BuilderFactory.LONG);
  }

  public final CommandObject<Long> ttl(String key) {
    return new CommandObject<>(commandArguments(TTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pttl(String key) {
    return new CommandObject<>(commandArguments(PTTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String key) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String... keys) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> sort(String key) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> sort(String key, SortingParams sortingParameters) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Long> sort(String key, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String key) {
    return new CommandObject<>(commandArguments(DEL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String... keys) {
    return new CommandObject<>(commandArguments(DEL).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String key) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String... keys) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    CommandArguments args = commandArguments(COPY).addKeyObject(srcKey).addKeyObject(dstKey);
    if (replace) {
      args.addObject(REPLACE);
    }
    return new CommandObject<>(args, BuilderFactory.BOOLEAN);
  }

  public final CommandObject<String> rename(String oldkey, String newkey) {
    return new CommandObject<>(commandArguments(RENAME).addKeyObject(oldkey).addKeyObject(newkey), BuilderFactory.STRING);
  }

  public final CommandObject<Long> renamenx(String oldkey, String newkey) {
    return new CommandObject<>(commandArguments(RENAMENX).addKeyObject(oldkey).addKeyObject(newkey), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> keys(String pattern) {
    return new CommandObject<>(commandArguments(Command.KEYS).addKeyObject(pattern), BuilderFactory.STRING_SET);
  }

  public final CommandObject<ScanResult<String>> scan(String cursor) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> memoryUsage(String key) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(String key, int samples) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key).addObject(samples), BuilderFactory.LONG);
  }

  public final CommandObject<String> randomKey() {
    return new CommandObject<>(commandArguments(RANDOMKEY), BuilderFactory.STRING);
  }
  // Key commands

  // String commands
  public final CommandObject<String> set(String key, String value) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(String key, String value, SetParams params) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> mset(String... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSET), keysvalues), BuilderFactory.STRING);
  }

  public final CommandObject<Long> msetnx(String... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSETNX), keysvalues), BuilderFactory.LONG);
  }

  public final CommandObject<String> get(String key) {
    return new CommandObject<>(commandArguments(Command.GET).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> mget(String... keys) {
    return new CommandObject<>(commandArguments(MGET).addKeyObjects(keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<String> getDel(String key) {
    return new CommandObject<>(commandArguments(Command.GETDEL).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> getEx(String key, GetExParams params) {
    return new CommandObject<>(commandArguments(Command.GETEX).addKeyObject(key).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<Boolean> setbit(String key, long offset, boolean value) {
    return new CommandObject<>(commandArguments(SETBIT).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> getbit(String key, long offset) {
    return new CommandObject<>(commandArguments(GETBIT).addKeyObject(key).addObject(offset), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> setrange(String key, long offset, String value) {
    return new CommandObject<>(commandArguments(SETRANGE).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> getrange(String key, long startOffset, long endOffset) {
    return new CommandObject<>(commandArguments(GETRANGE).addKeyObject(key).addObject(startOffset).addObject(endOffset), BuilderFactory.STRING);
  }

  public final CommandObject<String> getSet(String key, String value) {
    return new CommandObject<>(commandArguments(Command.GETSET).addKeyObject(key).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> setnx(String key, String value) {
    return new CommandObject<>(commandArguments(SETNX).addKeyObject(key).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> setex(String key, long seconds, String value) {
    return new CommandObject<>(commandArguments(SETEX).addKeyObject(key).addObject(seconds).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> psetex(String key, long milliseconds, String value) {
    return new CommandObject<>(commandArguments(PSETEX).addKeyObject(key).addObject(milliseconds).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> decrBy(String key, long decrement) {
    return new CommandObject<>(commandArguments(DECRBY).addKeyObject(key).addObject(decrement), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decr(String key) {
    return new CommandObject<>(commandArguments(DECR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incrBy(String key, long increment) {
    return new CommandObject<>(commandArguments(INCRBY).addKeyObject(key).addObject(increment), BuilderFactory.LONG);
  }

  public final CommandObject<Double> incrByFloat(String key, double increment) {
    return new CommandObject<>(commandArguments(INCRBYFLOAT).addKeyObject(key).addObject(increment), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> incr(String key) {
    return new CommandObject<>(commandArguments(Command.INCR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> append(String key, String value) {
    return new CommandObject<>(commandArguments(APPEND).addKeyObject(key).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> substr(String key, int start, int end) {
    return new CommandObject<>(commandArguments(SUBSTR).addKeyObject(key).addObject(start).addObject(end), BuilderFactory.STRING);
  }

  public final CommandObject<Long> strlen(String key) {
    return new CommandObject<>(commandArguments(STRLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key, long start, long end) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value, BitPosParams params) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> bitfield(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfieldReadonly(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD_RO).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    return new CommandObject<>(commandArguments(BITOP).addObject(op).addKeyObject(destKey).addKeyObjects(srcKeys), BuilderFactory.LONG);
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).addObject(LCS).addObject(STRINGS)
        .addKeyObject(keyA).addKeyObject(keyB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT_BUILDER);
  }
  // String commands

  // List commands
  public final CommandObject<Long> rpush(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSH).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpush(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSH).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> llen(String key) {
    return new CommandObject<>(commandArguments(LLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> lrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LRANGE).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<String> ltrim(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LTRIM).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING);
  }

  public final CommandObject<String> lindex(String key, long index) {
    return new CommandObject<>(commandArguments(LINDEX).addKeyObject(key).addObject(index), BuilderFactory.STRING);
  }

  public final CommandObject<String> lset(String key, long index, String value) {
    return new CommandObject<>(commandArguments(LSET).addKeyObject(key).addObject(index).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> lrem(String key, long count, String value) {
    return new CommandObject<>(commandArguments(LREM).addKeyObject(key).addObject(count).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> lpop(String key) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> lpop(String key, int count) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Long> lpos(String key, String element) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpos(String key, String element, LPosParams params) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> lpos(String key, String element, LPosParams params, long count) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element)
        .addParams(params).addObject(count), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<String> rpop(String key) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> rpop(String key, int count) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Long> linsert(String key, ListPosition where, String pivot, String value) {
    return new CommandObject<>(commandArguments(LINSERT).addKeyObject(key).addObject(where)
        .addObject(pivot).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSHX).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSHX).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> blpop(int timeout, String key) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObject(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> blpop(int timeout, String... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObjects(keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<KeyedListElement> blpop(double timeout, String key) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObject(key), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<KeyedListElement> blpop(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObjects(keys), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<List<String>> brpop(int timeout, String key) {
    return new CommandObject<>(commandArguments(BRPOP).blocking().addObject(timeout).addKeyObject(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> brpop(int timeout, String... keys) {
    return new CommandObject<>(commandArguments(BRPOP).addObject(timeout).addKeyObjects(keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<KeyedListElement> brpop(double timeout, String key) {
    return new CommandObject<>(commandArguments(BRPOP).addObject(timeout).addKeyObject(key), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<KeyedListElement> brpop(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BRPOP).addObject(timeout).addKeyObjects(keys), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<String> rpoplpush(String srckey, String dstkey) {
    return new CommandObject<>(commandArguments(RPOPLPUSH).addKeyObject(srckey).addKeyObject(dstkey), BuilderFactory.STRING);
  }

  public final CommandObject<String> brpoplpush(String source, String destination, int timeout) {
    return new CommandObject<>(commandArguments(BRPOPLPUSH).blocking().addKeyObject(source)
        .addKeyObject(destination).addObject(timeout), BuilderFactory.STRING);
  }

  public final CommandObject<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return new CommandObject<>(commandArguments(LMOVE).addKeyObject(srcKey).addKeyObject(dstKey)
        .addObject(from).addObject(to), BuilderFactory.STRING);
  }

  public final CommandObject<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return new CommandObject<>(commandArguments(BLMOVE).blocking().addKeyObject(srcKey)
        .addKeyObject(dstKey).addObject(from).addObject(to), BuilderFactory.STRING);
  }
  // List commands

  // Hash commands
  public final CommandObject<Long> hset(String key, String field, String value) {
    return new CommandObject<>(commandArguments(HSET).addKeyObject(key).addObject(field).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hset(String key, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HSET).addKeyObject(key), hash), BuilderFactory.LONG);
  }

  public final CommandObject<String> hget(String key, String field) {
    return new CommandObject<>(commandArguments(HGET).addKeyObject(key).addObject(field), BuilderFactory.STRING);
  }

  public final CommandObject<Long> hsetnx(String key, String field, String value) {
    return new CommandObject<>(commandArguments(HSETNX).addKeyObject(key).addObject(field).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> hmset(String key, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HMSET).addKeyObject(key), hash), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> hmget(String key, String... fields) {
    return new CommandObject<>(commandArguments(HMGET).addKeyObject(key).addObjects(fields), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Long> hincrBy(String key, String field, long value) {
    return new CommandObject<>(commandArguments(HINCRBY).addKeyObject(key).addObject(field).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Double> hincrByFloat(String key, String field, double value) {
    return new CommandObject<>(commandArguments(HINCRBYFLOAT).addKeyObject(key).addObject(field).addObject(value), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Boolean> hexists(String key, String field) {
    return new CommandObject<>(commandArguments(HEXISTS).addKeyObject(key).addObject(field), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> hdel(String key, String... field) {
    return new CommandObject<>(commandArguments(HDEL).addKeyObject(key).addObjects(field), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hlen(String key) {
    return new CommandObject<>(commandArguments(HLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> hkeys(String key) {
    return new CommandObject<>(commandArguments(HKEYS).addKeyObject(key), BuilderFactory.STRING_SET);
  }

  public final CommandObject<List<String>> hvals(String key) {
    return new CommandObject<>(commandArguments(HVALS).addKeyObject(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Map<String, String>> hgetAll(String key) {
    return new CommandObject<>(commandArguments(HGETALL).addKeyObject(key), BuilderFactory.STRING_MAP);
  }

  public final CommandObject<String> hrandfield(String key) {
    return new CommandObject<>(commandArguments(HRANDFIELD).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> hrandfield(String key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Map<String, String>> hrandfieldWithValues(String key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).addKeyObject(key).addObject(count).addObject(WITHVALUES), BuilderFactory.STRING_MAP);
  }

  public final CommandObject<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hstrlen(String key, String field) {
    return new CommandObject<>(commandArguments(HSTRLEN).addKeyObject(key).addObject(field), BuilderFactory.LONG);
  }
  // Hash commands

  // Set commands
  public final CommandObject<Long> sadd(String key, String... member) {
    return new CommandObject<>(commandArguments(SADD).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> smembers(String key) {
    return new CommandObject<>(commandArguments(SMEMBERS).addKeyObject(key), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> srem(String key, String... member) {
    return new CommandObject<>(commandArguments(SREM).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<String> spop(String key) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<Set<String>> spop(String key, long count) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> scard(String key) {
    return new CommandObject<>(commandArguments(SCARD).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> sismember(String key, String member) {
    return new CommandObject<>(commandArguments(SISMEMBER).addKeyObject(key).addObject(member), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<List<Boolean>> smismember(String key, String... members) {
    return new CommandObject<>(commandArguments(SMISMEMBER).addKeyObject(key).addObjects(members), BuilderFactory.BOOLEAN_LIST);
  }

  public final CommandObject<String> srandmember(String key) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> srandmember(String key, int count) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<ScanResult<String>> sscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sdiff(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sdiffstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sinter(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sinterstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> smove(String srckey, String dstkey, String member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sunion(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sunionstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Set commands

  // Sorted Set commands
  public final CommandObject<Long> zadd(String key, double score, String member) {
    return new CommandObject<>(commandArguments(ZADD).addKeyObject(key).addObject(score).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, double score, String member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).addKeyObject(key).addParams(params)
        .addObject(score).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, Map<String, Double> scoreMembers) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).addKeyObject(key), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).addKeyObject(key).addParams(params), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zaddIncr(String key, double score, String member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).addKeyObject(key).addObject(Keyword.INCR)
        .addParams(params).addObject(score).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Set<String>> zrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> zrem(String key, String... members) {
    return new CommandObject<>(commandArguments(ZREM).addKeyObject(key).addObjects(members), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member) {
    return new CommandObject<>(commandArguments(ZINCRBY).addKeyObject(key).addObject(increment).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
    return new CommandObject<>(commandArguments(ZADD).addKeyObject(key).addParams(params).addObject(increment).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> zrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZRANK).addKeyObject(key).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrevrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZREVRANK).addKeyObject(key).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zrevrange(String key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).addKeyObject(key)
        .addObject(start).addObject(stop).addObject(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> zrandmember(String key) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<Set<String>> zrandmember(String key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).addKeyObject(key).addObject(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zrandmemberWithScores(String key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).addKeyObject(key).addObject(count).addObject(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zcard(String key) {
    return new CommandObject<>(commandArguments(ZCARD).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zscore(String key, String member) {
    return new CommandObject<>(commandArguments(ZSCORE).addKeyObject(key).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<List<Double>> zmscore(String key, String... members) {
    return new CommandObject<>(commandArguments(ZMSCORE).addKeyObject(key).addObjects(members), BuilderFactory.DOUBLE_LIST);
  }

  public final CommandObject<Tuple> zpopmax(String key) {
    return new CommandObject<>(commandArguments(ZPOPMAX).addKeyObject(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmax(String key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMAX).addKeyObject(key).addObject(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Tuple> zpopmin(String key) {
    return new CommandObject<>(commandArguments(ZPOPMIN).addKeyObject(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmin(String key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMIN).addKeyObject(key).addObject(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zcount(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZCOUNT).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZCOUNT).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, String min, String max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, double max, double min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, String max, String min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByRank(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYRANK).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zlexcount(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZLEXCOUNT).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zrangeByLex(String key, String min, String max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByLex(String key, String max, String min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByLex(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYLEX).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<ScanResult<Tuple>> zscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<KeyedZSetElement> bzpopmax(double timeout, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<KeyedZSetElement> bzpopmin(double timeout, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zdiff(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zdiffWithScores(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zdiffStore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zinterstore(String dstkey, String... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zinterstore(String dstkey, ZParams params, String... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zinter(ZParams params, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zinterWithScores(ZParams params, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zunion(ZParams params, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zunionWithScores(ZParams params, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zunionstore(String dstkey, String... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zunionstore(String dstkey, ZParams params, String... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Sorted Set commands

  // Geo commands
  public final CommandObject<Long> geoadd(String key, double longitude, double latitude, String member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> geodist(String key, String member1, String member2) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<String>> geohash(String key, String... members) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoCoordinate>> geopos(String key, String... members) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> georadiusByMemberStore(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Geo commands

  // Hyper Log Log commands
  public final CommandObject<Long> pfadd(String key, String... elements) {
    return new CommandObject<>(commandArguments(PFADD).addKeyObject(key).addObjects(elements), BuilderFactory.LONG);
  }

  public final CommandObject<String> pfmerge(String destkey, String... sourcekeys) {
    return new CommandObject<>(commandArguments(PFMERGE).addKeyObject(destkey).addKeyObjects(sourcekeys), BuilderFactory.STRING);
  }

  public final CommandObject<Long> pfcount(String key) {
    return new CommandObject<>(commandArguments(PFCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(String... keys) {
    return new CommandObject<>(commandArguments(PFCOUNT).addKeyObjects(keys), BuilderFactory.LONG);
  }
  // Hyper Log Log commands

  // Stream commands
  public final CommandObject<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<StreamEntryID> xadd(String key, Map<String, String> hash, XAddParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xlen(String key) {
    return new CommandObject<>(commandArguments(XLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xack(String key, String group, StreamEntryID... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> xgroupSetID(String key, String groupname, StreamEntryID id) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xgroupDestroy(String key, String groupname) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xgroupDelConsumer(String key, String groupname, String consumername) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<StreamPendingSummary> xpending(String key, String groupname) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamPendingEntry>> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamPendingEntry>> xpending(String key, String groupname, XPendingParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xdel(String key, StreamEntryID... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xtrim(String key, long maxLen, boolean approximate) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xtrim(String key, XTrimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntry>> xclaim(String key, String group, String consumername, long minIdleTime, long newIdleTime, int retries, boolean force, StreamEntryID... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntry>> xclaim(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamEntryID>> xclaimJustId(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Map.Entry<StreamEntryID, List<StreamEntry>>> xautoclaim(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Map.Entry<StreamEntryID, List<StreamEntryID>>> xautoclaimJustId(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<StreamInfo> xinfoStream(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamGroupInfo>> xinfoGroup(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<StreamConsumersInfo>> xinfoConsumers(String key, String group) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Map.Entry<String, List<StreamEntry>>>> xread(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Map.Entry<String, List<StreamEntry>>>> xreadGroup(String groupname, String consumer, XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Stream commands

  public final CommandObject<LCSMatchResult> strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).addObject(LCS).addObject(STRINGS)
        .addObject(strA).addObject(strB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT_BUILDER);
  }

  private CommandArguments addFlatKeyValueArgs(CommandArguments args, String... keyvalues) {
    for (int i = 0; i < keyvalues.length; i += 2) {
      args.addKeyObject(keyvalues[i]).addObject(keyvalues[i + 1]);
    }
    return args;
  }

  private CommandArguments addFlatMapArgs(CommandArguments args, Map<?, ?> map) {
    for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
      args.addObject(entry.getKey());
      args.addObject(entry.getValue());
    }
    return args;
  }
}
