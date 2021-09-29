package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.*;
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

  public final CommandObject<Boolean> exists(byte[] key) {
    return new CommandObject<>(commandArguments(Command.EXISTS).addKeyObject(key), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> exists(byte[]... keys) {
    return new CommandObject<>(commandArguments(Command.EXISTS).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> persist(String key) {
    return new CommandObject<>(commandArguments(PERSIST).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> persist(byte[] key) {
    return new CommandObject<>(commandArguments(PERSIST).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<String> type(String key) {
    return new CommandObject<>(commandArguments(Command.TYPE).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> type(byte[] key) {
    return new CommandObject<>(commandArguments(Command.TYPE).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> dump(String key) {
    return new CommandObject<>(commandArguments(DUMP).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> dump(byte[] key) {
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

  public final CommandObject<String> restore(byte[] key, long ttl, byte[] serializedValue) {
    return new CommandObject<>(commandArguments(RESTORE).addKeyObject(key).addObject(ttl)
        .addObject(serializedValue), BuilderFactory.STRING);
  }

  public final CommandObject<String> restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params) {
    return new CommandObject<>(commandArguments(RESTORE).addKeyObject(key).addObject(ttl)
        .addObject(serializedValue).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<Long> expire(String key, long seconds) {
    return new CommandObject<>(commandArguments(EXPIRE).addKeyObject(key).addObject(seconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expire(byte[] key, long seconds) {
    return new CommandObject<>(commandArguments(EXPIRE).addKeyObject(key).addObject(seconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpire(String key, long milliseconds) {
    return new CommandObject<>(commandArguments(PEXPIRE).addKeyObject(key).addObject(milliseconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpire(byte[] key, long milliseconds) {
    return new CommandObject<>(commandArguments(PEXPIRE).addKeyObject(key).addObject(milliseconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expireAt(String key, long unixTime) {
    return new CommandObject<>(commandArguments(EXPIREAT).addKeyObject(key).addObject(unixTime), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expireAt(byte[] key, long unixTime) {
    return new CommandObject<>(commandArguments(EXPIREAT).addKeyObject(key).addObject(unixTime), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return new CommandObject<>(commandArguments(PEXPIREAT).addKeyObject(key).addObject(millisecondsTimestamp), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
    return new CommandObject<>(commandArguments(PEXPIREAT).addKeyObject(key).addObject(millisecondsTimestamp), BuilderFactory.LONG);
  }

  public final CommandObject<Long> ttl(String key) {
    return new CommandObject<>(commandArguments(TTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> ttl(byte[] key) {
    return new CommandObject<>(commandArguments(TTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pttl(String key) {
    return new CommandObject<>(commandArguments(PTTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pttl(byte[] key) {
    return new CommandObject<>(commandArguments(PTTL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String key) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String... keys) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(byte[] key) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(byte[]... keys) {
    return new CommandObject<>(commandArguments(TOUCH).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> sort(String key) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> sort(String key, SortingParams sortingParameters) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> sort(byte[] key) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> sort(String key, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(byte[] key, byte[] dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
    return new CommandObject<>(commandArguments(SORT).addKeyObject(key).addParams(sortingParameters)
        .addObject(STORE).addKeyObject(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String key) {
    return new CommandObject<>(commandArguments(DEL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String... keys) {
    return new CommandObject<>(commandArguments(DEL).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(byte[] key) {
    return new CommandObject<>(commandArguments(DEL).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(byte[]... keys) {
    return new CommandObject<>(commandArguments(DEL).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String key) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String... keys) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(byte[] key) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(byte[]... keys) {
    return new CommandObject<>(commandArguments(UNLINK).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    CommandArguments args = commandArguments(COPY).addKeyObject(srcKey).addKeyObject(dstKey);
    if (replace) {
      args.addObject(REPLACE);
    }
    return new CommandObject<>(args, BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> copy(byte[] srcKey, byte[] dstKey, boolean replace) {
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

  public final CommandObject<String> rename(byte[] oldkey, byte[] newkey) {
    return new CommandObject<>(commandArguments(RENAME).addKeyObject(oldkey).addKeyObject(newkey), BuilderFactory.STRING);
  }

  public final CommandObject<Long> renamenx(byte[] oldkey, byte[] newkey) {
    return new CommandObject<>(commandArguments(RENAMENX).addKeyObject(oldkey).addKeyObject(newkey), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> keys(String pattern) {
    return new CommandObject<>(commandArguments(Command.KEYS).addKeyObject(pattern), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> keys(byte[] pattern) {
    return new CommandObject<>(commandArguments(Command.KEYS).addKeyObject(pattern), BuilderFactory.BINARY_SET);
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

  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> memoryUsage(String key) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(String key, int samples) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key).addObject(samples), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(byte[] key) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(byte[] key, int samples) {
    return new CommandObject<>(commandArguments(MEMORY).addObject(USAGE).addKeyObject(key).addObject(samples), BuilderFactory.LONG);
  }

  public final CommandObject<String> randomKey() {
    return new CommandObject<>(commandArguments(RANDOMKEY), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> randomBinaryKey() {
    return new CommandObject<>(commandArguments(RANDOMKEY), BuilderFactory.BINARY);
  }
  // Key commands

  // String commands
  public final CommandObject<String> set(String key, String value) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(String key, String value, SetParams params) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(byte[] key, byte[] value, SetParams params) {
    return new CommandObject<>(commandArguments(Command.SET).addKeyObject(key).addObject(value).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> get(String key) {
    return new CommandObject<>(commandArguments(Command.GET).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> getDel(String key) {
    return new CommandObject<>(commandArguments(Command.GETDEL).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> getEx(String key, GetExParams params) {
    return new CommandObject<>(commandArguments(Command.GETEX).addKeyObject(key).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> get(byte[] key) {
    return new CommandObject<>(commandArguments(Command.GET).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> getDel(byte[] key) {
    return new CommandObject<>(commandArguments(Command.GETDEL).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> getEx(byte[] key, GetExParams params) {
    return new CommandObject<>(commandArguments(Command.GETEX).addKeyObject(key).addParams(params), BuilderFactory.BINARY);
  }

  public final CommandObject<String> getSet(String key, String value) {
    return new CommandObject<>(commandArguments(Command.GETSET).addKeyObject(key).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> getSet(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(Command.GETSET).addKeyObject(key).addObject(value), BuilderFactory.BINARY);
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

  public final CommandObject<Long> setnx(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(SETNX).addKeyObject(key).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> setex(byte[] key, long seconds, byte[] value) {
    return new CommandObject<>(commandArguments(SETEX).addKeyObject(key).addObject(seconds).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> psetex(byte[] key, long milliseconds, byte[] value) {
    return new CommandObject<>(commandArguments(PSETEX).addKeyObject(key).addObject(milliseconds).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<Boolean> setbit(String key, long offset, boolean value) {
    return new CommandObject<>(commandArguments(SETBIT).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> setbit(byte[] key, long offset, boolean value) {
    return new CommandObject<>(commandArguments(SETBIT).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> getbit(String key, long offset) {
    return new CommandObject<>(commandArguments(GETBIT).addKeyObject(key).addObject(offset), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> getbit(byte[] key, long offset) {
    return new CommandObject<>(commandArguments(GETBIT).addKeyObject(key).addObject(offset), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> setrange(String key, long offset, String value) {
    return new CommandObject<>(commandArguments(SETRANGE).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> setrange(byte[] key, long offset, byte[] value) {
    return new CommandObject<>(commandArguments(SETRANGE).addKeyObject(key).addObject(offset).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> getrange(String key, long startOffset, long endOffset) {
    return new CommandObject<>(commandArguments(GETRANGE).addKeyObject(key).addObject(startOffset).addObject(endOffset), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> getrange(byte[] key, long startOffset, long endOffset) {
    return new CommandObject<>(commandArguments(GETRANGE).addKeyObject(key).addObject(startOffset).addObject(endOffset), BuilderFactory.BINARY);
  }

  public final CommandObject<List<String>> mget(String... keys) {
    return new CommandObject<>(commandArguments(MGET).addKeyObjects(keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> mget(byte[]... keys) {
    return new CommandObject<>(commandArguments(MGET).addKeyObjects(keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> mset(String... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSET), keysvalues), BuilderFactory.STRING);
  }

  public final CommandObject<Long> msetnx(String... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSETNX), keysvalues), BuilderFactory.LONG);
  }

  public final CommandObject<String> mset(byte[]... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSET), keysvalues), BuilderFactory.STRING);
  }

  public final CommandObject<Long> msetnx(byte[]... keysvalues) {
    return new CommandObject<>(addFlatKeyValueArgs(commandArguments(MSETNX), keysvalues), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incr(String key) {
    return new CommandObject<>(commandArguments(Command.INCR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incrBy(String key, long increment) {
    return new CommandObject<>(commandArguments(INCRBY).addKeyObject(key).addObject(increment), BuilderFactory.LONG);
  }

  public final CommandObject<Double> incrByFloat(String key, double increment) {
    return new CommandObject<>(commandArguments(INCRBYFLOAT).addKeyObject(key).addObject(increment), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> incr(byte[] key) {
    return new CommandObject<>(commandArguments(Command.INCR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incrBy(byte[] key, long increment) {
    return new CommandObject<>(commandArguments(INCRBY).addKeyObject(key).addObject(increment), BuilderFactory.LONG);
  }

  public final CommandObject<Double> incrByFloat(byte[] key, double increment) {
    return new CommandObject<>(commandArguments(INCRBYFLOAT).addKeyObject(key).addObject(increment), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> decr(String key) {
    return new CommandObject<>(commandArguments(DECR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decrBy(String key, long decrement) {
    return new CommandObject<>(commandArguments(DECRBY).addKeyObject(key).addObject(decrement), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decr(byte[] key) {
    return new CommandObject<>(commandArguments(DECR).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decrBy(byte[] key, long decrement) {
    return new CommandObject<>(commandArguments(DECRBY).addKeyObject(key).addObject(decrement), BuilderFactory.LONG);
  }

  public final CommandObject<Long> append(String key, String value) {
    return new CommandObject<>(commandArguments(APPEND).addKeyObject(key).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> append(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(APPEND).addKeyObject(key).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> substr(String key, int start, int end) {
    return new CommandObject<>(commandArguments(SUBSTR).addKeyObject(key).addObject(start).addObject(end), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> substr(byte[] key, int start, int end) {
    return new CommandObject<>(commandArguments(SUBSTR).addKeyObject(key).addObject(start).addObject(end), BuilderFactory.BINARY);
  }

  public final CommandObject<Long> strlen(String key) {
    return new CommandObject<>(commandArguments(STRLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> strlen(byte[] key) {
    return new CommandObject<>(commandArguments(STRLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key, long start, long end) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key).addObject(start).addObject(end), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(byte[] key) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(byte[] key, long start, long end) {
    return new CommandObject<>(commandArguments(BITCOUNT).addKeyObject(key).addObject(start).addObject(end), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value, BitPosParams params) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(byte[] key, boolean value) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(byte[] key, boolean value, BitPosParams params) {
    return new CommandObject<>(commandArguments(BITPOS).addKeyObject(key).addObject(value ? 1 : 0).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> bitfield(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfieldReadonly(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD_RO).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfield(byte[] key, byte[]... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfieldReadonly(byte[] key, byte[]... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD_RO).addKeyObject(key).addObjects(arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    return new CommandObject<>(commandArguments(BITOP).addObject(op).addKeyObject(destKey).addKeyObjects(srcKeys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    return new CommandObject<>(commandArguments(BITOP).addObject(op).addKeyObject(destKey).addKeyObjects(srcKeys), BuilderFactory.LONG);
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).addObject(LCS).addObject(STRINGS)
        .addKeyObject(keyA).addKeyObject(keyB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSKeys(byte[] keyA, byte[] keyB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).addObject(LCS).addObject(STRINGS)
        .addKeyObject(keyA).addKeyObject(keyB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }
  // String commands

  // List commands
  public final CommandObject<Long> rpush(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSH).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpush(byte[] key, byte[]... args) {
    return new CommandObject<>(commandArguments(RPUSH).addKeyObject(key).addObjects(args), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpush(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSH).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpush(byte[] key, byte[]... args) {
    return new CommandObject<>(commandArguments(LPUSH).addKeyObject(key).addObjects(args), BuilderFactory.LONG);
  }

  public final CommandObject<Long> llen(String key) {
    return new CommandObject<>(commandArguments(LLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> llen(byte[] key) {
    return new CommandObject<>(commandArguments(LLEN).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> lrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LRANGE).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> lrange(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(LRANGE).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> ltrim(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LTRIM).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING);
  }

  public final CommandObject<String> ltrim(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(LTRIM).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING);
  }

  public final CommandObject<String> lindex(String key, long index) {
    return new CommandObject<>(commandArguments(LINDEX).addKeyObject(key).addObject(index), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> lindex(byte[] key, long index) {
    return new CommandObject<>(commandArguments(LINDEX).addKeyObject(key).addObject(index), BuilderFactory.BINARY);
  }

  public final CommandObject<String> lset(String key, long index, String value) {
    return new CommandObject<>(commandArguments(LSET).addKeyObject(key).addObject(index).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> lset(byte[] key, long index, byte[] value) {
    return new CommandObject<>(commandArguments(LSET).addKeyObject(key).addObject(index).addObject(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> lrem(String key, long count, String value) {
    return new CommandObject<>(commandArguments(LREM).addKeyObject(key).addObject(count).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lrem(byte[] key, long count, byte[] value) {
    return new CommandObject<>(commandArguments(LREM).addKeyObject(key).addObject(count).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> lpop(String key) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> lpop(String key, int count) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<byte[]> lpop(byte[] key) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<byte[]>> lpop(byte[] key, int count) {
    return new CommandObject<>(commandArguments(LPOP).addKeyObject(key).addObject(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> rpop(String key) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> rpop(String key, int count) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<byte[]> rpop(byte[] key) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<byte[]>> rpop(byte[] key, int count) {
    return new CommandObject<>(commandArguments(RPOP).addKeyObject(key).addObject(count), BuilderFactory.BINARY_LIST);
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

  public final CommandObject<Long> lpos(byte[] key, byte[] element) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpos(byte[] key, byte[] element, LPosParams params) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> lpos(byte[] key, byte[] element, LPosParams params, long count) {
    return new CommandObject<>(commandArguments(LPOS).addKeyObject(key).addObject(element)
        .addParams(params).addObject(count), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> linsert(String key, ListPosition where, String pivot, String value) {
    return new CommandObject<>(commandArguments(LINSERT).addKeyObject(key).addObject(where)
        .addObject(pivot).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
    return new CommandObject<>(commandArguments(LINSERT).addKeyObject(key).addObject(where)
        .addObject(pivot).addObject(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSHX).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSHX).addKeyObject(key).addObjects(string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpushx(byte[] key, byte[]... arg) {
    return new CommandObject<>(commandArguments(LPUSHX).addKeyObject(key).addObjects(arg), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpushx(byte[] key, byte[]... arg) {
    return new CommandObject<>(commandArguments(RPUSHX).addKeyObject(key).addObjects(arg), BuilderFactory.LONG);
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

  public final CommandObject<List<byte[]>> blpop(int timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObjects(keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> blpop(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().addObject(timeout).addKeyObjects(keys), BuilderFactory.BINARY_LIST);
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

  public final CommandObject<List<byte[]>> brpop(int timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BRPOP).addObject(timeout).addKeyObjects(keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> brpop(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BRPOP).addObject(timeout).addKeyObjects(keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> rpoplpush(String srckey, String dstkey) {
    return new CommandObject<>(commandArguments(RPOPLPUSH).addKeyObject(srckey).addKeyObject(dstkey), BuilderFactory.STRING);
  }

  public final CommandObject<String> brpoplpush(String source, String destination, int timeout) {
    return new CommandObject<>(commandArguments(BRPOPLPUSH).blocking().addKeyObject(source)
        .addKeyObject(destination).addObject(timeout), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> rpoplpush(byte[] srckey, byte[] dstkey) {
    return new CommandObject<>(commandArguments(RPOPLPUSH).addKeyObject(srckey).addKeyObject(dstkey), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout) {
    return new CommandObject<>(commandArguments(BRPOPLPUSH).blocking().addKeyObject(source)
        .addKeyObject(destination).addObject(timeout), BuilderFactory.BINARY);
  }

  public final CommandObject<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return new CommandObject<>(commandArguments(LMOVE).addKeyObject(srcKey).addKeyObject(dstKey)
        .addObject(from).addObject(to), BuilderFactory.STRING);
  }

  public final CommandObject<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return new CommandObject<>(commandArguments(BLMOVE).blocking().addKeyObject(srcKey)
        .addKeyObject(dstKey).addObject(from).addObject(to), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    return new CommandObject<>(commandArguments(LMOVE).addKeyObject(srcKey).addKeyObject(dstKey)
        .addObject(from).addObject(to), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout) {
    return new CommandObject<>(commandArguments(BLMOVE).blocking().addKeyObject(srcKey)
        .addKeyObject(dstKey).addObject(from).addObject(to), BuilderFactory.BINARY);
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

  public final CommandObject<Long> hset(byte[] key, byte[] field, byte[] value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hset(byte[] key, Map<byte[], byte[]> hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<byte[]> hget(byte[] key, byte[] field) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> hmget(byte[] key, byte[]... fields) {
    throw new UnsupportedOperationException("Not supported yet.");
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

  public final CommandObject<Long> hincrBy(byte[] key, byte[] field, long value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> hincrByFloat(byte[] key, byte[] field, double value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Boolean> hexists(byte[] key, byte[] field) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hdel(byte[] key, byte[]... field) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hlen(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> hkeys(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> hvals(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
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

  public final CommandObject<Map<byte[], byte[]>> hgetAll(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<byte[]> hrandfield(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> hrandfield(byte[] key, long count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Map<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<Map.Entry<byte[], byte[]>>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hstrlen(byte[] key, byte[] field) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Hash commands

  // Set commands
  public final CommandObject<Long> sadd(String key, String... member) {
    return new CommandObject<>(commandArguments(SADD).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sadd(byte[] key, byte[]... member) {
    return new CommandObject<>(commandArguments(SADD).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> smembers(String key) {
    return new CommandObject<>(commandArguments(SMEMBERS).addKeyObject(key), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> smembers(byte[] key) {
    return new CommandObject<>(commandArguments(SMEMBERS).addKeyObject(key), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> srem(String key, String... member) {
    return new CommandObject<>(commandArguments(SREM).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> srem(byte[] key, byte[]... member) {
    return new CommandObject<>(commandArguments(SREM).addKeyObject(key).addObjects(member), BuilderFactory.LONG);
  }

  public final CommandObject<String> spop(String key) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> spop(byte[] key) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<Set<String>> spop(String key, long count) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key).addObject(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> spop(byte[] key, long count) {
    return new CommandObject<>(commandArguments(SPOP).addKeyObject(key).addObject(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> scard(String key) {
    return new CommandObject<>(commandArguments(SCARD).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> scard(byte[] key) {
    return new CommandObject<>(commandArguments(SCARD).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> sismember(String key, String member) {
    return new CommandObject<>(commandArguments(SISMEMBER).addKeyObject(key).addObject(member), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> sismember(byte[] key, byte[] member) {
    return new CommandObject<>(commandArguments(SISMEMBER).addKeyObject(key).addObject(member), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<List<Boolean>> smismember(String key, String... members) {
    return new CommandObject<>(commandArguments(SMISMEMBER).addKeyObject(key).addObjects(members), BuilderFactory.BOOLEAN_LIST);
  }

  public final CommandObject<List<Boolean>> smismember(byte[] key, byte[]... members) {
    return new CommandObject<>(commandArguments(SMISMEMBER).addKeyObject(key).addObjects(members), BuilderFactory.BOOLEAN_LIST);
  }

  public final CommandObject<String> srandmember(String key) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> srandmember(byte[] key) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<String>> srandmember(String key, int count) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key).addObject(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> srandmember(byte[] key, int count) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).addKeyObject(key).addObject(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<ScanResult<String>> sscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<byte[]>> sscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sdiff(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sdiffstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> sdiff(byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sdiffstore(byte[] dstkey, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sinter(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sinterstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> sinter(byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sinterstore(byte[] dstkey, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sunion(String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sunionstore(String dstkey, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> sunion(byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> sunionstore(byte[] dstkey, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> smove(String srckey, String dstkey, String member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> smove(byte[] srckey, byte[] dstkey, byte[] member) {
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

  public final CommandObject<Long> zadd(byte[] key, double score, byte[] member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> zaddIncr(byte[] key, double score, byte[] member, ZAddParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member) {
    return new CommandObject<>(commandArguments(ZINCRBY).addKeyObject(key).addObject(increment).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
    return new CommandObject<>(commandArguments(ZADD).addKeyObject(key).addParams(params).addObject(increment).addObject(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(byte[] key, double increment, byte[] member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zrem(String key, String... members) {
    return new CommandObject<>(commandArguments(ZREM).addKeyObject(key).addObjects(members), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrem(byte[] key, byte[]... members) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZRANK).addKeyObject(key).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrevrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZREVRANK).addKeyObject(key).addObject(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrank(byte[] key, byte[] member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zrevrank(byte[] key, byte[] member) {
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

  public final CommandObject<byte[]> zrandmember(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrandmember(byte[] key, long count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrandmemberWithScores(byte[] key, long count) {
    throw new UnsupportedOperationException("Not supported yet.");
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

  public final CommandObject<Long> zcard(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> zscore(byte[] key, byte[] member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Double>> zmscore(byte[] key, byte[]... members) {
    throw new UnsupportedOperationException("Not supported yet.");
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

  public final CommandObject<Tuple> zpopmax(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zpopmax(byte[] key, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Tuple> zpopmin(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zpopmin(byte[] key, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<KeyedZSetElement> bzpopmax(double timeout, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<KeyedZSetElement> bzpopmin(double timeout, String... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> bzpopmax(double timeout, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> bzpopmin(double timeout, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zcount(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZCOUNT).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZCOUNT).addKeyObject(key).addObject(min).addObject(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(byte[] key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zcount(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).addKeyObject(key).addObject(start).addObject(stop), BuilderFactory.STRING_SET);
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

  public final CommandObject<ScanResult<Tuple>> zscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrange(byte[] key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrange(byte[] key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeWithScores(byte[] key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByRank(byte[] key, long start, long stop) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByScore(byte[] key, double min, double max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByScore(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zlexcount(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zremrangeByLex(byte[] key, byte[] min, byte[] max) {
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

  public final CommandObject<Set<byte[]>> zdiff(byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zdiffWithScores(byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zdiffStore(byte[] dstkey, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zinter(ZParams params, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zinterWithScores(ZParams params, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zinterstore(byte[] dstkey, byte[]... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<byte[]>> zunion(ZParams params, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<Tuple>> zunionWithScores(ZParams params, byte[]... keys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zunionstore(byte[] dstkey, byte[]... sets) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
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

  public final CommandObject<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> geoadd(byte[] key, GeoAddParams params, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> geodist(byte[] key, byte[] member1, byte[] member2) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> geohash(byte[] key, byte[]... members) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoCoordinate>> geopos(byte[] key, byte[]... members) {
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

  public final CommandObject<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<GeoRadiusResponse>> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> georadiusStore(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
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

  public final CommandObject<Long> pfadd(byte[] key, byte[]... elements) {
    return new CommandObject<>(commandArguments(PFADD).addKeyObject(key).addObjects(elements), BuilderFactory.LONG);
  }

  public final CommandObject<String> pfmerge(byte[] destkey, byte[]... sourcekeys) {
    return new CommandObject<>(commandArguments(PFMERGE).addKeyObject(destkey).addKeyObjects(sourcekeys), BuilderFactory.STRING);
  }

  public final CommandObject<Long> pfcount(String key) {
    return new CommandObject<>(commandArguments(PFCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(String... keys) {
    return new CommandObject<>(commandArguments(PFCOUNT).addKeyObjects(keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(byte[] key) {
    return new CommandObject<>(commandArguments(PFCOUNT).addKeyObject(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(byte[]... keys) {
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

  public final CommandObject<byte[]> xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash, long maxLen, boolean approximateLength) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<byte[]> xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xlen(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xack(byte[] key, byte[] group, byte[]... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> xgroupCreate(byte[] key, byte[] consumer, byte[] id, boolean makeStream) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<String> xgroupSetID(byte[] key, byte[] consumer, byte[] id) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xgroupDestroy(byte[] key, byte[] consumer) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xgroupDelConsumer(byte[] key, byte[] consumer, byte[] consumerName) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xdel(byte[] key, byte[]... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xtrim(byte[] key, long maxLen, boolean approximateLength) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> xtrim(byte[] key, XTrimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Object> xpending(byte[] key, byte[] groupname) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xpending(byte[] key, byte[] groupname, XPendingParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xclaim(byte[] key, byte[] groupname, byte[] consumername, long minIdleTime, long newIdleTime, int retries, boolean force, byte[]... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xclaim(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xclaimJustId(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Object> xinfoStreamBinary(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xinfoGroupBinary(byte[] key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<Object>> xinfoConsumersBinary(byte[] key, byte[] group) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<List<byte[]>> xreadGroup(byte[] groupname, byte[] consumer, XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  // Stream commands

  // Miscellaneous commands
  public final CommandObject<Boolean> copy(byte[] srcKey, byte[] dstKey, int db, boolean replace) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).addObject(LCS).addObject(STRINGS)
        .addObject(strA).addObject(strB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }
  // Miscellaneous commands

  private CommandArguments addFlatKeyValueArgs(CommandArguments args, String... keyvalues) {
    for (int i = 0; i < keyvalues.length; i += 2) {
      args.addKeyObject(keyvalues[i]).addObject(keyvalues[i + 1]);
    }
    return args;
  }

  private CommandArguments addFlatKeyValueArgs(CommandArguments args, byte[]... keyvalues) {
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
