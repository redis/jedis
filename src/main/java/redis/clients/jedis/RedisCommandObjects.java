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
    return new CommandObject<>(commandArguments(Command.EXISTS).key(key), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> exists(String... keys) {
    return new CommandObject<>(commandArguments(Command.EXISTS).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> exists(byte[] key) {
    return new CommandObject<>(commandArguments(Command.EXISTS).key(key), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> exists(byte[]... keys) {
    return new CommandObject<>(commandArguments(Command.EXISTS).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> persist(String key) {
    return new CommandObject<>(commandArguments(PERSIST).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> persist(byte[] key) {
    return new CommandObject<>(commandArguments(PERSIST).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<String> type(String key) {
    return new CommandObject<>(commandArguments(Command.TYPE).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> type(byte[] key) {
    return new CommandObject<>(commandArguments(Command.TYPE).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> dump(String key) {
    return new CommandObject<>(commandArguments(DUMP).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> dump(byte[] key) {
    return new CommandObject<>(commandArguments(DUMP).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<String> restore(String key, long ttl, byte[] serializedValue) {
    return new CommandObject<>(commandArguments(RESTORE).key(key).add(ttl)
        .add(serializedValue), BuilderFactory.STRING);
  }

  public final CommandObject<String> restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return new CommandObject<>(commandArguments(RESTORE).key(key).add(ttl)
        .add(serializedValue).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> restore(byte[] key, long ttl, byte[] serializedValue) {
    return new CommandObject<>(commandArguments(RESTORE).key(key).add(ttl)
        .add(serializedValue), BuilderFactory.STRING);
  }

  public final CommandObject<String> restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params) {
    return new CommandObject<>(commandArguments(RESTORE).key(key).add(ttl)
        .add(serializedValue).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<Long> expire(String key, long seconds) {
    return new CommandObject<>(commandArguments(EXPIRE).key(key).add(seconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expire(byte[] key, long seconds) {
    return new CommandObject<>(commandArguments(EXPIRE).key(key).add(seconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpire(String key, long milliseconds) {
    return new CommandObject<>(commandArguments(PEXPIRE).key(key).add(milliseconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpire(byte[] key, long milliseconds) {
    return new CommandObject<>(commandArguments(PEXPIRE).key(key).add(milliseconds), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expireAt(String key, long unixTime) {
    return new CommandObject<>(commandArguments(EXPIREAT).key(key).add(unixTime), BuilderFactory.LONG);
  }

  public final CommandObject<Long> expireAt(byte[] key, long unixTime) {
    return new CommandObject<>(commandArguments(EXPIREAT).key(key).add(unixTime), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return new CommandObject<>(commandArguments(PEXPIREAT).key(key).add(millisecondsTimestamp), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
    return new CommandObject<>(commandArguments(PEXPIREAT).key(key).add(millisecondsTimestamp), BuilderFactory.LONG);
  }

  public final CommandObject<Long> ttl(String key) {
    return new CommandObject<>(commandArguments(TTL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> ttl(byte[] key) {
    return new CommandObject<>(commandArguments(TTL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pttl(String key) {
    return new CommandObject<>(commandArguments(PTTL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pttl(byte[] key) {
    return new CommandObject<>(commandArguments(PTTL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String key) {
    return new CommandObject<>(commandArguments(TOUCH).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(String... keys) {
    return new CommandObject<>(commandArguments(TOUCH).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(byte[] key) {
    return new CommandObject<>(commandArguments(TOUCH).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> touch(byte[]... keys) {
    return new CommandObject<>(commandArguments(TOUCH).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> sort(String key) {
    return new CommandObject<>(commandArguments(SORT).key(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> sort(String key, SortingParams sortingParameters) {
    return new CommandObject<>(commandArguments(SORT).key(key).addParams(sortingParameters), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> sort(byte[] key) {
    return new CommandObject<>(commandArguments(SORT).key(key), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
    return new CommandObject<>(commandArguments(SORT).key(key).addParams(sortingParameters), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> sort(String key, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).key(key)
        .add(STORE).key(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
    return new CommandObject<>(commandArguments(SORT).key(key).addParams(sortingParameters)
        .add(STORE).key(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(byte[] key, byte[] dstkey) {
    return new CommandObject<>(commandArguments(SORT).key(key)
        .add(STORE).key(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
    return new CommandObject<>(commandArguments(SORT).key(key).addParams(sortingParameters)
        .add(STORE).key(dstkey), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String key) {
    return new CommandObject<>(commandArguments(DEL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(String... keys) {
    return new CommandObject<>(commandArguments(DEL).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(byte[] key) {
    return new CommandObject<>(commandArguments(DEL).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> del(byte[]... keys) {
    return new CommandObject<>(commandArguments(DEL).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String key) {
    return new CommandObject<>(commandArguments(UNLINK).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(String... keys) {
    return new CommandObject<>(commandArguments(UNLINK).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(byte[] key) {
    return new CommandObject<>(commandArguments(UNLINK).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> unlink(byte[]... keys) {
    return new CommandObject<>(commandArguments(UNLINK).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    CommandArguments args = commandArguments(COPY).key(srcKey).key(dstKey);
    if (replace) {
      args.add(REPLACE);
    }
    return new CommandObject<>(args, BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> copy(byte[] srcKey, byte[] dstKey, boolean replace) {
    CommandArguments args = commandArguments(COPY).key(srcKey).key(dstKey);
    if (replace) {
      args.add(REPLACE);
    }
    return new CommandObject<>(args, BuilderFactory.BOOLEAN);
  }

  public final CommandObject<String> rename(String oldkey, String newkey) {
    return new CommandObject<>(commandArguments(RENAME).key(oldkey).key(newkey), BuilderFactory.STRING);
  }

  public final CommandObject<Long> renamenx(String oldkey, String newkey) {
    return new CommandObject<>(commandArguments(RENAMENX).key(oldkey).key(newkey), BuilderFactory.LONG);
  }

  public final CommandObject<String> rename(byte[] oldkey, byte[] newkey) {
    return new CommandObject<>(commandArguments(RENAME).key(oldkey).key(newkey), BuilderFactory.STRING);
  }

  public final CommandObject<Long> renamenx(byte[] oldkey, byte[] newkey) {
    return new CommandObject<>(commandArguments(RENAMENX).key(oldkey).key(newkey), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> keys(String pattern) {
    return new CommandObject<>(commandArguments(Command.KEYS).key(pattern), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> keys(byte[] pattern) {
    return new CommandObject<>(commandArguments(Command.KEYS).key(pattern), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params) {
    return scan(cursor, params, null);
  }

  public final CommandObject<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params) {
    return scan(cursor, params, null);
  }

  public final CommandObject<ScanResult<byte[]>> scan(byte[] cursor, ScanParams params, byte[] type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> memoryUsage(String key) {
    return new CommandObject<>(commandArguments(MEMORY).add(USAGE).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(String key, int samples) {
    return new CommandObject<>(commandArguments(MEMORY).add(USAGE).key(key).add(samples), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(byte[] key) {
    return new CommandObject<>(commandArguments(MEMORY).add(USAGE).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> memoryUsage(byte[] key, int samples) {
    return new CommandObject<>(commandArguments(MEMORY).add(USAGE).key(key).add(samples), BuilderFactory.LONG);
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
    return new CommandObject<>(commandArguments(Command.SET).key(key).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(String key, String value, SetParams params) {
    return new CommandObject<>(commandArguments(Command.SET).key(key).add(value).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(Command.SET).key(key).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> set(byte[] key, byte[] value, SetParams params) {
    return new CommandObject<>(commandArguments(Command.SET).key(key).add(value).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<String> get(String key) {
    return new CommandObject<>(commandArguments(Command.GET).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> getDel(String key) {
    return new CommandObject<>(commandArguments(Command.GETDEL).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<String> getEx(String key, GetExParams params) {
    return new CommandObject<>(commandArguments(Command.GETEX).key(key).addParams(params), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> get(byte[] key) {
    return new CommandObject<>(commandArguments(Command.GET).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> getDel(byte[] key) {
    return new CommandObject<>(commandArguments(Command.GETDEL).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> getEx(byte[] key, GetExParams params) {
    return new CommandObject<>(commandArguments(Command.GETEX).key(key).addParams(params), BuilderFactory.BINARY);
  }

  public final CommandObject<String> getSet(String key, String value) {
    return new CommandObject<>(commandArguments(Command.GETSET).key(key).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> getSet(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(Command.GETSET).key(key).add(value), BuilderFactory.BINARY);
  }

  public final CommandObject<Long> setnx(String key, String value) {
    return new CommandObject<>(commandArguments(SETNX).key(key).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> setex(String key, long seconds, String value) {
    return new CommandObject<>(commandArguments(SETEX).key(key).add(seconds).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> psetex(String key, long milliseconds, String value) {
    return new CommandObject<>(commandArguments(PSETEX).key(key).add(milliseconds).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> setnx(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(SETNX).key(key).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> setex(byte[] key, long seconds, byte[] value) {
    return new CommandObject<>(commandArguments(SETEX).key(key).add(seconds).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> psetex(byte[] key, long milliseconds, byte[] value) {
    return new CommandObject<>(commandArguments(PSETEX).key(key).add(milliseconds).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<Boolean> setbit(String key, long offset, boolean value) {
    return new CommandObject<>(commandArguments(SETBIT).key(key).add(offset).add(value), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> setbit(byte[] key, long offset, boolean value) {
    return new CommandObject<>(commandArguments(SETBIT).key(key).add(offset).add(value), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> getbit(String key, long offset) {
    return new CommandObject<>(commandArguments(GETBIT).key(key).add(offset), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> getbit(byte[] key, long offset) {
    return new CommandObject<>(commandArguments(GETBIT).key(key).add(offset), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> setrange(String key, long offset, String value) {
    return new CommandObject<>(commandArguments(SETRANGE).key(key).add(offset).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> setrange(byte[] key, long offset, byte[] value) {
    return new CommandObject<>(commandArguments(SETRANGE).key(key).add(offset).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> getrange(String key, long startOffset, long endOffset) {
    return new CommandObject<>(commandArguments(GETRANGE).key(key).add(startOffset).add(endOffset), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> getrange(byte[] key, long startOffset, long endOffset) {
    return new CommandObject<>(commandArguments(GETRANGE).key(key).add(startOffset).add(endOffset), BuilderFactory.BINARY);
  }

  public final CommandObject<List<String>> mget(String... keys) {
    return new CommandObject<>(commandArguments(MGET).keys((Object[]) keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> mget(byte[]... keys) {
    return new CommandObject<>(commandArguments(MGET).keys((Object[]) keys), BuilderFactory.BINARY_LIST);
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
    return new CommandObject<>(commandArguments(Command.INCR).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incrBy(String key, long increment) {
    return new CommandObject<>(commandArguments(INCRBY).key(key).add(increment), BuilderFactory.LONG);
  }

  public final CommandObject<Double> incrByFloat(String key, double increment) {
    return new CommandObject<>(commandArguments(INCRBYFLOAT).key(key).add(increment), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> incr(byte[] key) {
    return new CommandObject<>(commandArguments(Command.INCR).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> incrBy(byte[] key, long increment) {
    return new CommandObject<>(commandArguments(INCRBY).key(key).add(increment), BuilderFactory.LONG);
  }

  public final CommandObject<Double> incrByFloat(byte[] key, double increment) {
    return new CommandObject<>(commandArguments(INCRBYFLOAT).key(key).add(increment), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> decr(String key) {
    return new CommandObject<>(commandArguments(DECR).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decrBy(String key, long decrement) {
    return new CommandObject<>(commandArguments(DECRBY).key(key).add(decrement), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decr(byte[] key) {
    return new CommandObject<>(commandArguments(DECR).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> decrBy(byte[] key, long decrement) {
    return new CommandObject<>(commandArguments(DECRBY).key(key).add(decrement), BuilderFactory.LONG);
  }

  public final CommandObject<Long> append(String key, String value) {
    return new CommandObject<>(commandArguments(APPEND).key(key).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> append(byte[] key, byte[] value) {
    return new CommandObject<>(commandArguments(APPEND).key(key).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> substr(String key, int start, int end) {
    return new CommandObject<>(commandArguments(SUBSTR).key(key).add(start).add(end), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> substr(byte[] key, int start, int end) {
    return new CommandObject<>(commandArguments(SUBSTR).key(key).add(start).add(end), BuilderFactory.BINARY);
  }

  public final CommandObject<Long> strlen(String key) {
    return new CommandObject<>(commandArguments(STRLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> strlen(byte[] key) {
    return new CommandObject<>(commandArguments(STRLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key) {
    return new CommandObject<>(commandArguments(BITCOUNT).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(String key, long start, long end) {
    return new CommandObject<>(commandArguments(BITCOUNT).key(key).add(start).add(end), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(byte[] key) {
    return new CommandObject<>(commandArguments(BITCOUNT).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitcount(byte[] key, long start, long end) {
    return new CommandObject<>(commandArguments(BITCOUNT).key(key).add(start).add(end), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value) {
    return new CommandObject<>(commandArguments(BITPOS).key(key).add(value ? 1 : 0), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(String key, boolean value, BitPosParams params) {
    return new CommandObject<>(commandArguments(BITPOS).key(key).add(value ? 1 : 0).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(byte[] key, boolean value) {
    return new CommandObject<>(commandArguments(BITPOS).key(key).add(value ? 1 : 0), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitpos(byte[] key, boolean value, BitPosParams params) {
    return new CommandObject<>(commandArguments(BITPOS).key(key).add(value ? 1 : 0).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> bitfield(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD).key(key).addObjects((Object[]) arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfieldReadonly(String key, String... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD_RO).key(key).addObjects((Object[]) arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfield(byte[] key, byte[]... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD).key(key).addObjects((Object[]) arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<List<Long>> bitfieldReadonly(byte[] key, byte[]... arguments) {
    return new CommandObject<>(commandArguments(BITFIELD_RO).key(key).addObjects((Object[]) arguments), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    return new CommandObject<>(commandArguments(BITOP).add(op).key(destKey).keys((Object[]) srcKeys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    return new CommandObject<>(commandArguments(BITOP).add(op).key(destKey).keys((Object[]) srcKeys), BuilderFactory.LONG);
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).add(LCS).add(STRINGS)
        .key(keyA).key(keyB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }

  public final CommandObject<LCSMatchResult> strAlgoLCSKeys(byte[] keyA, byte[] keyB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).add(LCS).add(STRINGS)
        .key(keyA).key(keyB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }
  // String commands

  // List commands
  public final CommandObject<Long> rpush(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSH).key(key).addObjects((Object[]) string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpush(byte[] key, byte[]... args) {
    return new CommandObject<>(commandArguments(RPUSH).key(key).addObjects((Object[]) args), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpush(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSH).key(key).addObjects((Object[]) string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpush(byte[] key, byte[]... args) {
    return new CommandObject<>(commandArguments(LPUSH).key(key).addObjects((Object[]) args), BuilderFactory.LONG);
  }

  public final CommandObject<Long> llen(String key) {
    return new CommandObject<>(commandArguments(LLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> llen(byte[] key) {
    return new CommandObject<>(commandArguments(LLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> lrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LRANGE).key(key).add(start).add(stop), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> lrange(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(LRANGE).key(key).add(start).add(stop), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> ltrim(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(LTRIM).key(key).add(start).add(stop), BuilderFactory.STRING);
  }

  public final CommandObject<String> ltrim(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(LTRIM).key(key).add(start).add(stop), BuilderFactory.STRING);
  }

  public final CommandObject<String> lindex(String key, long index) {
    return new CommandObject<>(commandArguments(LINDEX).key(key).add(index), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> lindex(byte[] key, long index) {
    return new CommandObject<>(commandArguments(LINDEX).key(key).add(index), BuilderFactory.BINARY);
  }

  public final CommandObject<String> lset(String key, long index, String value) {
    return new CommandObject<>(commandArguments(LSET).key(key).add(index).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<String> lset(byte[] key, long index, byte[] value) {
    return new CommandObject<>(commandArguments(LSET).key(key).add(index).add(value), BuilderFactory.STRING);
  }

  public final CommandObject<Long> lrem(String key, long count, String value) {
    return new CommandObject<>(commandArguments(LREM).key(key).add(count).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lrem(byte[] key, long count, byte[] value) {
    return new CommandObject<>(commandArguments(LREM).key(key).add(count).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> lpop(String key) {
    return new CommandObject<>(commandArguments(LPOP).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> lpop(String key, int count) {
    return new CommandObject<>(commandArguments(LPOP).key(key).add(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<byte[]> lpop(byte[] key) {
    return new CommandObject<>(commandArguments(LPOP).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<byte[]>> lpop(byte[] key, int count) {
    return new CommandObject<>(commandArguments(LPOP).key(key).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> rpop(String key) {
    return new CommandObject<>(commandArguments(RPOP).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> rpop(String key, int count) {
    return new CommandObject<>(commandArguments(RPOP).key(key).add(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<byte[]> rpop(byte[] key) {
    return new CommandObject<>(commandArguments(RPOP).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<byte[]>> rpop(byte[] key, int count) {
    return new CommandObject<>(commandArguments(RPOP).key(key).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> lpos(String key, String element) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpos(String key, String element, LPosParams params) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> lpos(String key, String element, LPosParams params, long count) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element)
        .addParams(params).add(count), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> lpos(byte[] key, byte[] element) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpos(byte[] key, byte[] element, LPosParams params) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<List<Long>> lpos(byte[] key, byte[] element, LPosParams params, long count) {
    return new CommandObject<>(commandArguments(LPOS).key(key).add(element)
        .addParams(params).add(count), BuilderFactory.LONG_LIST);
  }

  public final CommandObject<Long> linsert(String key, ListPosition where, String pivot, String value) {
    return new CommandObject<>(commandArguments(LINSERT).key(key).add(where)
        .add(pivot).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
    return new CommandObject<>(commandArguments(LINSERT).key(key).add(where)
        .add(pivot).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(LPUSHX).key(key).addObjects((Object[]) string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpushx(String key, String... string) {
    return new CommandObject<>(commandArguments(RPUSHX).key(key).addObjects((Object[]) string), BuilderFactory.LONG);
  }

  public final CommandObject<Long> lpushx(byte[] key, byte[]... arg) {
    return new CommandObject<>(commandArguments(LPUSHX).key(key).addObjects((Object[]) arg), BuilderFactory.LONG);
  }

  public final CommandObject<Long> rpushx(byte[] key, byte[]... arg) {
    return new CommandObject<>(commandArguments(RPUSHX).key(key).addObjects((Object[]) arg), BuilderFactory.LONG);
  }

  public final CommandObject<List<String>> blpop(int timeout, String key) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).key(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> blpop(int timeout, String... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).keys((Object[]) keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<KeyedListElement> blpop(double timeout, String key) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).key(key), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<KeyedListElement> blpop(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).keys((Object[]) keys), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<List<byte[]>> blpop(int timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).keys((Object[]) keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> blpop(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BLPOP).blocking().add(timeout).keys((Object[]) keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<String>> brpop(int timeout, String key) {
    return new CommandObject<>(commandArguments(BRPOP).blocking().add(timeout).key(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<String>> brpop(int timeout, String... keys) {
    return new CommandObject<>(commandArguments(BRPOP).add(timeout).keys((Object[]) keys), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<KeyedListElement> brpop(double timeout, String key) {
    return new CommandObject<>(commandArguments(BRPOP).add(timeout).key(key), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<KeyedListElement> brpop(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BRPOP).add(timeout).keys((Object[]) keys), BuilderFactory.KEYED_LIST_ELEMENT);
  }

  public final CommandObject<List<byte[]>> brpop(int timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BRPOP).add(timeout).keys((Object[]) keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> brpop(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BRPOP).add(timeout).keys((Object[]) keys), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<String> rpoplpush(String srckey, String dstkey) {
    return new CommandObject<>(commandArguments(RPOPLPUSH).key(srckey).key(dstkey), BuilderFactory.STRING);
  }

  public final CommandObject<String> brpoplpush(String source, String destination, int timeout) {
    return new CommandObject<>(commandArguments(BRPOPLPUSH).blocking().key(source)
        .key(destination).add(timeout), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> rpoplpush(byte[] srckey, byte[] dstkey) {
    return new CommandObject<>(commandArguments(RPOPLPUSH).key(srckey).key(dstkey), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout) {
    return new CommandObject<>(commandArguments(BRPOPLPUSH).blocking().key(source)
        .key(destination).add(timeout), BuilderFactory.BINARY);
  }

  public final CommandObject<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return new CommandObject<>(commandArguments(LMOVE).key(srcKey).key(dstKey)
        .add(from).add(to), BuilderFactory.STRING);
  }

  public final CommandObject<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return new CommandObject<>(commandArguments(BLMOVE).blocking().key(srcKey)
        .key(dstKey).add(from).add(to), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    return new CommandObject<>(commandArguments(LMOVE).key(srcKey).key(dstKey)
        .add(from).add(to), BuilderFactory.BINARY);
  }

  public final CommandObject<byte[]> blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout) {
    return new CommandObject<>(commandArguments(BLMOVE).blocking().key(srcKey)
        .key(dstKey).add(from).add(to), BuilderFactory.BINARY);
  }
  // List commands

  // Hash commands
  public final CommandObject<Long> hset(String key, String field, String value) {
    return new CommandObject<>(commandArguments(HSET).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hset(String key, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HSET).key(key), hash), BuilderFactory.LONG);
  }

  public final CommandObject<String> hget(String key, String field) {
    return new CommandObject<>(commandArguments(HGET).key(key).add(field), BuilderFactory.STRING);
  }

  public final CommandObject<Long> hsetnx(String key, String field, String value) {
    return new CommandObject<>(commandArguments(HSETNX).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> hmset(String key, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HMSET).key(key), hash), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> hmget(String key, String... fields) {
    return new CommandObject<>(commandArguments(HMGET).key(key).addObjects((Object[]) fields), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Long> hset(byte[] key, byte[] field, byte[] value) {
    return new CommandObject<>(commandArguments(HSET).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hset(byte[] key, Map<byte[], byte[]> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HSET).key(key), hash), BuilderFactory.LONG);
  }

  public final CommandObject<byte[]> hget(byte[] key, byte[] field) {
    return new CommandObject<>(commandArguments(HGET).key(key).add(field), BuilderFactory.BINARY);
  }

  public final CommandObject<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
    return new CommandObject<>(commandArguments(HSETNX).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(HMSET).key(key), hash), BuilderFactory.STRING);
  }

  public final CommandObject<List<byte[]>> hmget(byte[] key, byte[]... fields) {
    return new CommandObject<>(commandArguments(HMGET).key(key).addObjects((Object[]) fields), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> hincrBy(String key, String field, long value) {
    return new CommandObject<>(commandArguments(HINCRBY).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Double> hincrByFloat(String key, String field, double value) {
    return new CommandObject<>(commandArguments(HINCRBYFLOAT).key(key).add(field).add(value), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Boolean> hexists(String key, String field) {
    return new CommandObject<>(commandArguments(HEXISTS).key(key).add(field), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> hdel(String key, String... field) {
    return new CommandObject<>(commandArguments(HDEL).key(key).addObjects((Object[]) field), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hlen(String key) {
    return new CommandObject<>(commandArguments(HLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hincrBy(byte[] key, byte[] field, long value) {
    return new CommandObject<>(commandArguments(HINCRBY).key(key).add(field).add(value), BuilderFactory.LONG);
  }

  public final CommandObject<Double> hincrByFloat(byte[] key, byte[] field, double value) {
    return new CommandObject<>(commandArguments(HINCRBYFLOAT).key(key).add(field).add(value), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Boolean> hexists(byte[] key, byte[] field) {
    return new CommandObject<>(commandArguments(HEXISTS).key(key).add(field), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Long> hdel(byte[] key, byte[]... field) {
    return new CommandObject<>(commandArguments(HDEL).key(key).addObjects((Object[]) field), BuilderFactory.LONG);
  }

  public final CommandObject<Long> hlen(byte[] key) {
    return new CommandObject<>(commandArguments(HLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> hkeys(String key) {
    return new CommandObject<>(commandArguments(HKEYS).key(key), BuilderFactory.STRING_SET);
  }

  public final CommandObject<List<String>> hvals(String key) {
    return new CommandObject<>(commandArguments(HVALS).key(key), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Set<byte[]>> hkeys(byte[] key) {
    return new CommandObject<>(commandArguments(HKEYS).key(key), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<List<byte[]>> hvals(byte[] key) {
    return new CommandObject<>(commandArguments(HVALS).key(key), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Map<String, String>> hgetAll(String key) {
    return new CommandObject<>(commandArguments(HGETALL).key(key), BuilderFactory.STRING_MAP);
  }

  public final CommandObject<String> hrandfield(String key) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<List<String>> hrandfield(String key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key).add(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<Map<String, String>> hrandfieldWithValues(String key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key).add(count).add(WITHVALUES), BuilderFactory.STRING_MAP);
  }

  public final CommandObject<Map<byte[], byte[]>> hgetAll(byte[] key) {
    return new CommandObject<>(commandArguments(HGETALL).key(key), BuilderFactory.BINARY_MAP);
  }

  public final CommandObject<byte[]> hrandfield(byte[] key) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<byte[]>> hrandfield(byte[] key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Map<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count) {
    return new CommandObject<>(commandArguments(HRANDFIELD).key(key).add(count).add(WITHVALUES), BuilderFactory.BINARY_MAP);
  }

  public final CommandObject<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hstrlen(String key, String field) {
    return new CommandObject<>(commandArguments(HSTRLEN).key(key).add(field), BuilderFactory.LONG);
  }

  public final CommandObject<ScanResult<Map.Entry<byte[], byte[]>>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Long> hstrlen(byte[] key, byte[] field) {
    return new CommandObject<>(commandArguments(HSTRLEN).key(key).add(field), BuilderFactory.LONG);
  }
  // Hash commands

  // Set commands
  public final CommandObject<Long> sadd(String key, String... member) {
    return new CommandObject<>(commandArguments(SADD).key(key).addObjects((Object[]) member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> sadd(byte[] key, byte[]... member) {
    return new CommandObject<>(commandArguments(SADD).key(key).addObjects((Object[]) member), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> smembers(String key) {
    return new CommandObject<>(commandArguments(SMEMBERS).key(key), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> smembers(byte[] key) {
    return new CommandObject<>(commandArguments(SMEMBERS).key(key), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> srem(String key, String... member) {
    return new CommandObject<>(commandArguments(SREM).key(key).addObjects((Object[]) member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> srem(byte[] key, byte[]... member) {
    return new CommandObject<>(commandArguments(SREM).key(key).addObjects((Object[]) member), BuilderFactory.LONG);
  }

  public final CommandObject<String> spop(String key) {
    return new CommandObject<>(commandArguments(SPOP).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> spop(byte[] key) {
    return new CommandObject<>(commandArguments(SPOP).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<Set<String>> spop(String key, long count) {
    return new CommandObject<>(commandArguments(SPOP).key(key).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<byte[]>> spop(byte[] key, long count) {
    return new CommandObject<>(commandArguments(SPOP).key(key).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> scard(String key) {
    return new CommandObject<>(commandArguments(SCARD).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> scard(byte[] key) {
    return new CommandObject<>(commandArguments(SCARD).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Boolean> sismember(String key, String member) {
    return new CommandObject<>(commandArguments(SISMEMBER).key(key).add(member), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<Boolean> sismember(byte[] key, byte[] member) {
    return new CommandObject<>(commandArguments(SISMEMBER).key(key).add(member), BuilderFactory.BOOLEAN);
  }

  public final CommandObject<List<Boolean>> smismember(String key, String... members) {
    return new CommandObject<>(commandArguments(SMISMEMBER).key(key).addObjects((Object[]) members), BuilderFactory.BOOLEAN_LIST);
  }

  public final CommandObject<List<Boolean>> smismember(byte[] key, byte[]... members) {
    return new CommandObject<>(commandArguments(SMISMEMBER).key(key).addObjects((Object[]) members), BuilderFactory.BOOLEAN_LIST);
  }

  public final CommandObject<String> srandmember(String key) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<byte[]> srandmember(byte[] key) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<List<String>> srandmember(String key, int count) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).key(key).add(count), BuilderFactory.STRING_LIST);
  }

  public final CommandObject<List<byte[]>> srandmember(byte[] key, int count) {
    return new CommandObject<>(commandArguments(SRANDMEMBER).key(key).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<ScanResult<String>> sscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<byte[]>> sscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> sdiff(String... keys) {
    return new CommandObject<>(commandArguments(SDIFF).keys((Object[]) keys), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> sdiffstore(String dstkey, String... keys) {
    return new CommandObject<>(commandArguments(SDIFFSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> sdiff(byte[]... keys) {
    return new CommandObject<>(commandArguments(SDIFF).keys((Object[]) keys), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> sdiffstore(byte[] dstkey, byte[]... keys) {
    return new CommandObject<>(commandArguments(SDIFFSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> sinter(String... keys) {
    return new CommandObject<>(commandArguments(SINTER).keys((Object[]) keys), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> sinterstore(String dstkey, String... keys) {
    return new CommandObject<>(commandArguments(SINTERSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> sinter(byte[]... keys) {
    return new CommandObject<>(commandArguments(SINTER).keys((Object[]) keys), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> sinterstore(byte[] dstkey, byte[]... keys) {
    return new CommandObject<>(commandArguments(SINTERSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> sunion(String... keys) {
    return new CommandObject<>(commandArguments(SUNION).keys((Object[]) keys), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> sunionstore(String dstkey, String... keys) {
    return new CommandObject<>(commandArguments(SUNIONSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> sunion(byte[]... keys) {
    return new CommandObject<>(commandArguments(SUNION).keys((Object[]) keys), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> sunionstore(byte[] dstkey, byte[]... keys) {
    return new CommandObject<>(commandArguments(SUNIONSTORE).key(dstkey).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> smove(String srckey, String dstkey, String member) {
    return new CommandObject<>(commandArguments(SMOVE).key(srckey).key(dstkey).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> smove(byte[] srckey, byte[] dstkey, byte[] member) {
    return new CommandObject<>(commandArguments(SMOVE).key(srckey).key(dstkey).add(member), BuilderFactory.LONG);
  }
  // Set commands

  // Sorted Set commands
  public final CommandObject<Long> zadd(String key, double score, String member) {
    return new CommandObject<>(commandArguments(ZADD).key(key).add(score).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, double score, String member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).addParams(params)
        .add(score).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, Map<String, Double> scoreMembers) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).key(key), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).key(key).addParams(params), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zaddIncr(String key, double score, String member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).add(Keyword.INCR)
        .addParams(params).add(score).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> zadd(byte[] key, double score, byte[] member) {
    return new CommandObject<>(commandArguments(ZADD).key(key).add(score).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).addParams(params)
        .add(score).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).key(key), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(ZADD).key(key).addParams(params), scoreMembers), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zaddIncr(byte[] key, double score, byte[] member, ZAddParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).add(Keyword.INCR)
        .addParams(params).add(score).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member) {
    return new CommandObject<>(commandArguments(ZINCRBY).key(key).add(increment).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).addParams(params).add(increment).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(byte[] key, double increment, byte[] member) {
    return new CommandObject<>(commandArguments(ZINCRBY).key(key).add(increment).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Double> zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
    return new CommandObject<>(commandArguments(ZADD).key(key).addParams(params).add(increment).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<Long> zrem(String key, String... members) {
    return new CommandObject<>(commandArguments(ZREM).key(key).addObjects((Object[]) members), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrem(byte[] key, byte[]... members) {
    return new CommandObject<>(commandArguments(ZREM).key(key).addObjects((Object[]) members), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZRANK).key(key).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrevrank(String key, String member) {
    return new CommandObject<>(commandArguments(ZREVRANK).key(key).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrank(byte[] key, byte[] member) {
    return new CommandObject<>(commandArguments(ZRANK).key(key).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zrevrank(byte[] key, byte[] member) {
    return new CommandObject<>(commandArguments(ZREVRANK).key(key).add(member), BuilderFactory.LONG);
  }

  public final CommandObject<String> zrandmember(String key) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key), BuilderFactory.STRING);
  }

  public final CommandObject<Set<String>> zrandmember(String key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zrandmemberWithScores(String key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<byte[]> zrandmember(byte[] key) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key), BuilderFactory.BINARY);
  }

  public final CommandObject<Set<byte[]>> zrandmember(byte[] key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zrandmemberWithScores(byte[] key, long count) {
    return new CommandObject<>(commandArguments(ZRANDMEMBER).key(key).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zcard(String key) {
    return new CommandObject<>(commandArguments(ZCARD).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zscore(String key, String member) {
    return new CommandObject<>(commandArguments(ZSCORE).key(key).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<List<Double>> zmscore(String key, String... members) {
    return new CommandObject<>(commandArguments(ZMSCORE).key(key).addObjects((Object[]) members), BuilderFactory.DOUBLE_LIST);
  }

  public final CommandObject<Long> zcard(byte[] key) {
    return new CommandObject<>(commandArguments(ZCARD).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Double> zscore(byte[] key, byte[] member) {
    return new CommandObject<>(commandArguments(ZSCORE).key(key).add(member), BuilderFactory.DOUBLE);
  }

  public final CommandObject<List<Double>> zmscore(byte[] key, byte[]... members) {
    return new CommandObject<>(commandArguments(ZMSCORE).key(key).addObjects((Object[]) members), BuilderFactory.DOUBLE_LIST);
  }

  public final CommandObject<Tuple> zpopmax(String key) {
    return new CommandObject<>(commandArguments(ZPOPMAX).key(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmax(String key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMAX).key(key).add(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Tuple> zpopmin(String key) {
    return new CommandObject<>(commandArguments(ZPOPMIN).key(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmin(String key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMIN).key(key).add(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Tuple> zpopmax(byte[] key) {
    return new CommandObject<>(commandArguments(ZPOPMAX).key(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmax(byte[] key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMAX).key(key).add(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Tuple> zpopmin(byte[] key) {
    return new CommandObject<>(commandArguments(ZPOPMIN).key(key), BuilderFactory.TUPLE);
  }

  public final CommandObject<Set<Tuple>> zpopmin(byte[] key, int count) {
    return new CommandObject<>(commandArguments(ZPOPMIN).key(key).add(count), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<KeyedZSetElement> bzpopmax(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BZPOPMAX).keys((Object[]) keys).add(timeout), BuilderFactory.KEYED_ZSET_ELEMENT);
  }

  public final CommandObject<KeyedZSetElement> bzpopmin(double timeout, String... keys) {
    return new CommandObject<>(commandArguments(BZPOPMIN).keys((Object[]) keys).add(timeout), BuilderFactory.KEYED_ZSET_ELEMENT);
  }

  public final CommandObject<List<byte[]>> bzpopmax(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BZPOPMAX).keys((Object[]) keys).add(timeout), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> bzpopmin(double timeout, byte[]... keys) {
    return new CommandObject<>(commandArguments(BZPOPMIN).keys((Object[]) keys).add(timeout), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> zcount(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(byte[] key, double min, double max) {
    return new CommandObject<>(commandArguments(ZCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zcount(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).key(key).add(start).add(stop), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrange(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREVRANGE).key(key).add(start).add(stop), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).key(key)
        .add(start).add(stop).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREVRANGE).key(key)
        .add(start).add(stop).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, double max, double min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, String max, String min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }
  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<byte[]>> zrange(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).key(key).add(start).add(stop), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrange(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREVRANGE).key(key).add(start).add(stop), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zrangeWithScores(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZRANGE).key(key)
        .add(start).add(stop).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREVRANGE).key(key)
        .add(start).add(stop).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYSCORE).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYSCORE).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zremrangeByRank(String key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYRANK).key(key).add(start).add(stop), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(String key, double min, double max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByRank(byte[] key, long start, long stop) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYRANK).key(key).add(start).add(stop), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(byte[] key, double min, double max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zremrangeByScore(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYSCORE).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zlexcount(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZLEXCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zrangeByLex(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZRANGEBYLEX).key(key).add(min).add(max), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYLEX).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByLex(String key, String max, String min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYLEX).key(key).add(max).add(min), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYLEX).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Long> zremrangeByLex(String key, String min, String max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYLEX).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zlexcount(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZLEXCOUNT).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZRANGEBYLEX).key(key).add(min).add(max), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return new CommandObject<>(commandArguments(ZRANGEBYLEX).key(key).add(min).add(max)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYLEX).key(key).add(max).add(min), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return new CommandObject<>(commandArguments(ZREVRANGEBYLEX).key(key).add(max).add(min)
        .add(LIMIT).add(offset).add(count), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Long> zremrangeByLex(byte[] key, byte[] min, byte[] max) {
    return new CommandObject<>(commandArguments(ZREMRANGEBYLEX).key(key).add(min).add(max), BuilderFactory.LONG);
  }

  public final CommandObject<ScanResult<Tuple>> zscan(String key, String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<ScanResult<Tuple>> zscan(byte[] key, byte[] cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public final CommandObject<Set<String>> zdiff(String... keys) {
    return new CommandObject<>(commandArguments(ZDIFF).add(keys.length).keys((Object[]) keys), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zdiffWithScores(String... keys) {
    return new CommandObject<>(commandArguments(ZDIFF).add(keys.length).keys((Object[]) keys)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zdiffStore(String dstkey, String... keys) {
    return new CommandObject<>(commandArguments(ZDIFFSTORE).key(dstkey).add(keys.length).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> zdiff(byte[]... keys) {
    return new CommandObject<>(commandArguments(ZDIFF).add(keys.length).keys((Object[]) keys), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zdiffWithScores(byte[]... keys) {
    return new CommandObject<>(commandArguments(ZDIFF).add(keys.length).keys((Object[]) keys)
        .add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zdiffStore(byte[] dstkey, byte[]... keys) {
    return new CommandObject<>(commandArguments(ZDIFFSTORE).key(dstkey)
        .add(keys.length).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zinterstore(String dstkey, String... sets) {
    return new CommandObject<>(commandArguments(ZINTERSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zinterstore(String dstkey, ZParams params, String... sets) {
    return new CommandObject<>(commandArguments(ZINTERSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zinter(ZParams params, String... keys) {
    return new CommandObject<>(commandArguments(ZINTER).add(keys.length).keys((Object[]) keys)
        .addParams(params), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zinterWithScores(ZParams params, String... keys) {
    return new CommandObject<>(commandArguments(ZINTER).add(keys.length).keys((Object[]) keys)
        .addParams(params).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zinterstore(byte[] dstkey, byte[]... sets) {
    return new CommandObject<>(commandArguments(ZINTERSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return new CommandObject<>(commandArguments(ZINTERSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> zinter(ZParams params, byte[]... keys) {
    return new CommandObject<>(commandArguments(ZINTER).add(keys.length).keys((Object[]) keys)
        .addParams(params), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zinterWithScores(ZParams params, byte[]... keys) {
    return new CommandObject<>(commandArguments(ZINTER).add(keys.length).keys((Object[]) keys)
        .addParams(params).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zunionstore(String dstkey, String... sets) {
    return new CommandObject<>(commandArguments(ZUNIONSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zunionstore(String dstkey, ZParams params, String... sets) {
    return new CommandObject<>(commandArguments(ZUNIONSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Set<String>> zunion(ZParams params, String... keys) {
    return new CommandObject<>(commandArguments(ZUNION).add(keys.length).keys((Object[]) keys)
        .addParams(params), BuilderFactory.STRING_SET);
  }

  public final CommandObject<Set<Tuple>> zunionWithScores(ZParams params, String... keys) {
    return new CommandObject<>(commandArguments(ZUNION).add(keys.length).keys((Object[]) keys)
        .addParams(params).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
  }

  public final CommandObject<Long> zunionstore(byte[] dstkey, byte[]... sets) {
    return new CommandObject<>(commandArguments(ZUNIONSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets), BuilderFactory.LONG);
  }

  public final CommandObject<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return new CommandObject<>(commandArguments(ZUNIONSTORE).key(dstkey)
        .add(sets.length).keys((Object[]) sets).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Set<byte[]>> zunion(ZParams params, byte[]... keys) {
    return new CommandObject<>(commandArguments(ZUNION).add(keys.length).keys((Object[]) keys)
        .addParams(params), BuilderFactory.BINARY_SET);
  }

  public final CommandObject<Set<Tuple>> zunionWithScores(ZParams params, byte[]... keys) {
    return new CommandObject<>(commandArguments(ZUNION).add(keys.length).keys((Object[]) keys)
        .addParams(params).add(WITHSCORES), BuilderFactory.TUPLE_ZSET);
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
    return new CommandObject<>(commandArguments(PFADD).key(key).addObjects((Object[]) elements), BuilderFactory.LONG);
  }

  public final CommandObject<String> pfmerge(String destkey, String... sourcekeys) {
    return new CommandObject<>(commandArguments(PFMERGE).key(destkey).keys((Object[]) sourcekeys), BuilderFactory.STRING);
  }

  public final CommandObject<Long> pfadd(byte[] key, byte[]... elements) {
    return new CommandObject<>(commandArguments(PFADD).key(key).addObjects((Object[]) elements), BuilderFactory.LONG);
  }

  public final CommandObject<String> pfmerge(byte[] destkey, byte[]... sourcekeys) {
    return new CommandObject<>(commandArguments(PFMERGE).key(destkey).keys((Object[]) sourcekeys), BuilderFactory.STRING);
  }

  public final CommandObject<Long> pfcount(String key) {
    return new CommandObject<>(commandArguments(PFCOUNT).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(String... keys) {
    return new CommandObject<>(commandArguments(PFCOUNT).keys((Object[]) keys), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(byte[] key) {
    return new CommandObject<>(commandArguments(PFCOUNT).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<Long> pfcount(byte[]... keys) {
    return new CommandObject<>(commandArguments(PFCOUNT).keys((Object[]) keys), BuilderFactory.LONG);
  }
  // Hyper Log Log commands

  // Stream commands
  public final CommandObject<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(XADD).key(key).add(id), hash),
        BuilderFactory.STREAM_ENTRY_ID);
  }

  public final CommandObject<StreamEntryID> xadd(String key, Map<String, String> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  public final CommandObject<StreamEntryID> xadd(String key, XAddParams params, Map<String, String> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(XADD).key(key).addParams(params), hash),
        BuilderFactory.STREAM_ENTRY_ID);
  }

  public final CommandObject<Long> xlen(String key) {
    return new CommandObject<>(commandArguments(XLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<byte[]> xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash) {
    return new CommandObject<>(addFlatMapArgs(commandArguments(XADD).key(key).addParams(params), hash),
        BuilderFactory.BINARY);
  }

  public final CommandObject<Long> xlen(byte[] key) {
    return new CommandObject<>(commandArguments(XLEN).key(key), BuilderFactory.LONG);
  }

  public final CommandObject<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return new CommandObject<>(commandArguments(XRANGE).key(key).add(start).add(end),
        BuilderFactory.STREAM_ENTRY_LIST);
  }

  public final CommandObject<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return new CommandObject<>(commandArguments(XRANGE).key(key).add(start).add(end)
        .add(COUNT).add(count), BuilderFactory.STREAM_ENTRY_LIST);
  }

  public final CommandObject<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return new CommandObject<>(commandArguments(XREVRANGE).key(key).add(start).add(end),
        BuilderFactory.STREAM_ENTRY_LIST);
  }

  public final CommandObject<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    return new CommandObject<>(commandArguments(XREVRANGE).key(key).add(start).add(end)
        .add(COUNT).add(count), BuilderFactory.STREAM_ENTRY_LIST);
  }

  public final CommandObject<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end) {
    return new CommandObject<>(commandArguments(XRANGE).key(key).add(start).add(end),
        BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end, int count) {
    return new CommandObject<>(commandArguments(XRANGE).key(key).add(start).add(end)
        .add(COUNT).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start) {
    return new CommandObject<>(commandArguments(XREVRANGE).key(key).add(start).add(end),
        BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    return new CommandObject<>(commandArguments(XREVRANGE).key(key).add(start).add(end)
        .add(COUNT).add(count), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<Long> xack(String key, String group, StreamEntryID... ids) {
    return new CommandObject<>(commandArguments(XACK).key(key).add(group).addObjects((Object[]) ids), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xack(byte[] key, byte[] group, byte[]... ids) {
    return new CommandObject<>(commandArguments(XACK).key(key).add(group).addObjects((Object[]) ids), BuilderFactory.LONG);
  }

  public final CommandObject<String> xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
    CommandArguments args = commandArguments(XGROUP).add(CREATE).key(key)
        .add(groupname).add(id);
    if (makeStream) args.add(MKSTREAM);
    return new CommandObject<>(args, BuilderFactory.STRING);
  }

  public final CommandObject<String> xgroupSetID(String key, String groupname, StreamEntryID id) {
    return new CommandObject<>(commandArguments(XGROUP).add(SETID)
        .key(key).add(groupname).add(id), BuilderFactory.STRING);
  }

  public final CommandObject<Long> xgroupDestroy(String key, String groupname) {
    return new CommandObject<>(commandArguments(XGROUP).add(DESTROY)
        .key(key).add(groupname), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xgroupDelConsumer(String key, String groupname, String consumername) {
    return new CommandObject<>(commandArguments(XGROUP).add(DELCONSUMER)
        .key(key).add(groupname).add(consumername), BuilderFactory.LONG);
  }

  public final CommandObject<String> xgroupCreate(byte[] key, byte[] groupname, byte[] id, boolean makeStream) {
    CommandArguments args = commandArguments(XGROUP).add(CREATE).key(key)
        .add(groupname).add(id);
    if (makeStream) args.add(MKSTREAM);
    return new CommandObject<>(args, BuilderFactory.STRING);
  }

  public final CommandObject<String> xgroupSetID(byte[] key, byte[] groupname, byte[] id) {
    return new CommandObject<>(commandArguments(XGROUP).add(SETID)
        .key(key).add(groupname).add(id), BuilderFactory.STRING);
  }

  public final CommandObject<Long> xgroupDestroy(byte[] key, byte[] groupname) {
    return new CommandObject<>(commandArguments(XGROUP).add(DESTROY)
        .key(key).add(groupname), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xgroupDelConsumer(byte[] key, byte[] groupname, byte[] consumerName) {
    return new CommandObject<>(commandArguments(XGROUP).add(DELCONSUMER)
        .key(key).add(groupname).add(consumerName), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xdel(String key, StreamEntryID... ids) {
    return new CommandObject<>(commandArguments(XDEL).key(key).addObjects((Object[]) ids), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xtrim(String key, long maxLen, boolean approximate) {
    CommandArguments args = commandArguments(XTRIM).key(key).add(MAXLEN);
    if (approximate) args.add(Protocol.BYTES_TILDE);
    args.add(maxLen);
    return new CommandObject<>(args, BuilderFactory.LONG);
  }

  public final CommandObject<Long> xtrim(String key, XTrimParams params) {
    return new CommandObject<>(commandArguments(XDEL).key(key).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xdel(byte[] key, byte[]... ids) {
    return new CommandObject<>(commandArguments(XDEL).key(key).addObjects((Object[]) ids), BuilderFactory.LONG);
  }

  public final CommandObject<Long> xtrim(byte[] key, long maxLen, boolean approximateLength) {
    CommandArguments args = commandArguments(XTRIM).key(key).add(MAXLEN);
    if (approximateLength) args.add(Protocol.BYTES_TILDE);
    args.add(maxLen);
    return new CommandObject<>(args, BuilderFactory.LONG);
  }

  public final CommandObject<Long> xtrim(byte[] key, XTrimParams params) {
    return new CommandObject<>(commandArguments(XDEL).key(key).addParams(params), BuilderFactory.LONG);
  }

  public final CommandObject<StreamPendingSummary> xpending(String key, String groupname) {
    return new CommandObject<>(commandArguments(XPENDING).key(key).add(groupname),
        BuilderFactory.STREAM_PENDING_SUMMARY);
  }

  public final CommandObject<List<StreamPendingEntry>> xpending(String key, String groupname,
      StreamEntryID start, StreamEntryID end, int count, String consumername) {
    CommandArguments args = commandArguments(XPENDING).key(key).add(groupname)
        .add(start).add(end).add(count);
    if (consumername != null) args.add(consumername);
    return new CommandObject<>(args, BuilderFactory.STREAM_PENDING_ENTRY_LIST);
  }

  public final CommandObject<List<StreamPendingEntry>> xpending(String key, String groupname, XPendingParams params) {
    return new CommandObject<>(commandArguments(XPENDING).key(key).add(groupname)
        .addParams(params), BuilderFactory.STREAM_PENDING_ENTRY_LIST);
  }

  public final CommandObject<Object> xpending(byte[] key, byte[] groupname) {
    return new CommandObject<>(commandArguments(XPENDING).key(key).add(groupname),
        BuilderFactory.RAW_OBJECT);
  }

  public final CommandObject<List<Object>> xpending(byte[] key, byte[] groupname,
      byte[] start, byte[] end, int count, byte[] consumername) {
    CommandArguments args = commandArguments(XPENDING).key(key).add(groupname)
        .add(start).add(end).add(count);
    if (consumername != null) args.add(consumername);
    return new CommandObject<>(args, BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<List<Object>> xpending(byte[] key, byte[] groupname, XPendingParams params) {
    return new CommandObject<>(commandArguments(XPENDING).key(key).add(groupname)
        .addParams(params), BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<List<StreamEntry>> xclaim(String key, String group,
      String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return new CommandObject<>(commandArguments(XCLAIM).key(key).add(group)
        .add(consumername).add(minIdleTime).addObjects((Object[]) ids).addParams(params),
        BuilderFactory.STREAM_ENTRY_LIST);
  }

  public final CommandObject<List<StreamEntryID>> xclaimJustId(String key, String group,
      String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return new CommandObject<>(commandArguments(XCLAIM).key(key).add(group)
        .add(consumername).add(minIdleTime).addObjects((Object[]) ids).addParams(params)
        .add(JUSTID), BuilderFactory.STREAM_ENTRY_ID_LIST);
  }

  public final CommandObject<Map.Entry<StreamEntryID, List<StreamEntry>>> xautoclaim(String key,
      String group, String consumerName, long minIdleTime, StreamEntryID start,
      XAutoClaimParams params) {
    return new CommandObject<>(commandArguments(XAUTOCLAIM).key(key).add(group)
        .add(consumerName).add(minIdleTime).add(start).addParams(params),
        BuilderFactory.STREAM_AUTO_CLAIM_RESPONSE);
  }

  public final CommandObject<Map.Entry<StreamEntryID, List<StreamEntryID>>> xautoclaimJustId(
      String key, String group, String consumerName, long minIdleTime, StreamEntryID start,
      XAutoClaimParams params) {
    return new CommandObject<>(commandArguments(XAUTOCLAIM).key(key).add(group)
        .add(consumerName).add(minIdleTime).add(start).addParams(params)
        .add(JUSTID), BuilderFactory.STREAM_AUTO_CLAIM_ID_RESPONSE);
  }

  public final CommandObject<List<byte[]>> xclaim(byte[] key, byte[] group,
      byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    return new CommandObject<>(commandArguments(XCLAIM).key(key).add(group)
        .add(consumername).add(minIdleTime).addObjects((Object[]) ids).addParams(params),
        BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> xclaimJustId(byte[] key, byte[] group,
      byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    return new CommandObject<>(commandArguments(XCLAIM).key(key).add(group)
        .add(consumername).add(minIdleTime).addObjects((Object[]) ids).addParams(params)
        .add(JUSTID), BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<Object>> xautoclaim(byte[] key, byte[] groupName,
      byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return new CommandObject<>(commandArguments(XAUTOCLAIM).key(key).add(groupName)
        .add(consumerName).add(minIdleTime).add(start).addParams(params),
        BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<List<Object>> xautoclaimJustId(byte[] key, byte[] groupName,
      byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return new CommandObject<>(commandArguments(XAUTOCLAIM).key(key).add(groupName)
        .add(consumerName).add(minIdleTime).add(start).addParams(params)
        .add(JUSTID), BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<StreamInfo> xinfoStream(String key) {
    return new CommandObject<>(commandArguments(XINFO).add(STREAM).key(key), BuilderFactory.STREAM_INFO);
  }

  public final CommandObject<List<StreamGroupInfo>> xinfoGroup(String key) {
    return new CommandObject<>(commandArguments(XINFO).add(GROUPS).key(key), BuilderFactory.STREAM_GROUP_INFO_LIST);
  }

  public final CommandObject<List<StreamConsumersInfo>> xinfoConsumers(String key, String group) {
    return new CommandObject<>(commandArguments(XINFO).add(CONSUMERS).key(key).add(group), BuilderFactory.STREAM_CONSUMERS_INFO_LIST);
  }

  public final CommandObject<Object> xinfoStream(byte[] key) {
    return new CommandObject<>(commandArguments(XINFO).add(STREAM).key(key), BuilderFactory.RAW_OBJECT);
  }

  public final CommandObject<List<Object>> xinfoGroup(byte[] key) {
    return new CommandObject<>(commandArguments(XINFO).add(GROUPS).key(key), BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<List<Object>> xinfoConsumers(byte[] key, byte[] group) {
    return new CommandObject<>(commandArguments(XINFO).add(CONSUMERS).key(key).add(group), BuilderFactory.RAW_OBJECT_LIST);
  }

  public final CommandObject<List<Map.Entry<String, List<StreamEntry>>>> xread(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    CommandArguments args = commandArguments(XREAD).addParams(xReadParams).add(STREAMS);
    Set<Map.Entry<String, StreamEntryID>> entrySet = streams.entrySet();
    entrySet.forEach(entry ->  args.key(entry.getKey()));
    entrySet.forEach(entry ->  args.add(entry.getValue()));
    return new CommandObject<>(args, BuilderFactory.STREAM_READ_RESPONSE);
  }

  public final CommandObject<List<Map.Entry<String, List<StreamEntry>>>> xreadGroup(
      String groupname, String consumer, XReadGroupParams xReadGroupParams,
      Map<String, StreamEntryID> streams) {
    CommandArguments args = commandArguments(XREADGROUP)
        .add(GROUP).add(groupname).add(consumer)
        .addParams(xReadGroupParams).add(STREAMS);
    Set<Map.Entry<String, StreamEntryID>> entrySet = streams.entrySet();
    entrySet.forEach(entry ->  args.key(entry.getKey()));
    entrySet.forEach(entry ->  args.add(entry.getValue()));
    return new CommandObject<>(args, BuilderFactory.STREAM_READ_RESPONSE);
  }

  public final CommandObject<List<byte[]>> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams) {
    CommandArguments args = commandArguments(XREAD).addParams(xReadParams).add(STREAMS);
    for (Map.Entry<byte[], byte[]> entry : streams) {
      args.key(entry.getKey());
    }
    for (Map.Entry<byte[], byte[]> entry : streams) {
      args.add(entry.getValue());
    }
    return new CommandObject<>(args, BuilderFactory.BINARY_LIST);
  }

  public final CommandObject<List<byte[]>> xreadGroup(byte[] groupname, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams) {
    CommandArguments args = commandArguments(XREADGROUP)
        .add(GROUP).add(groupname).add(consumer)
        .addParams(xReadGroupParams).add(STREAMS);
    for (Map.Entry<byte[], byte[]> entry : streams) {
      args.key(entry.getKey());
    }
    for (Map.Entry<byte[], byte[]> entry : streams) {
      args.add(entry.getValue());
    }
    return new CommandObject<>(args, BuilderFactory.BINARY_LIST);
  }
  // Stream commands

  // Miscellaneous commands
  public final CommandObject<LCSMatchResult> strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params) {
    return new CommandObject<>(commandArguments(STRALGO).add(LCS).add(STRINGS)
        .add(strA).add(strB).addParams(params),
        BuilderFactory.STR_ALGO_LCS_RESULT);
  }
  // Miscellaneous commands

  private CommandArguments addFlatKeyValueArgs(CommandArguments args, String... keyvalues) {
    for (int i = 0; i < keyvalues.length; i += 2) {
      args.key(keyvalues[i]).add(keyvalues[i + 1]);
    }
    return args;
  }

  private CommandArguments addFlatKeyValueArgs(CommandArguments args, byte[]... keyvalues) {
    for (int i = 0; i < keyvalues.length; i += 2) {
      args.key(keyvalues[i]).add(keyvalues[i + 1]);
    }
    return args;
  }

  private CommandArguments addFlatMapArgs(CommandArguments args, Map<?, ?> map) {
    for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
      args.add(entry.getKey());
      args.add(entry.getValue());
    }
    return args;
  }
}
