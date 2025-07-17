package redis.clients.jedis;

import org.json.JSONArray;
import redis.clients.jedis.args.*;
import redis.clients.jedis.bloom.*;
import redis.clients.jedis.bloom.commands.RedisBloomCommands;
import redis.clients.jedis.commands.*;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.json.commands.RedisJsonV2Commands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.FtAggregateIteration;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.timeseries.*;
import redis.clients.jedis.util.KeyValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class that implements all Redis command interfaces.
 */
public abstract class BaseRedisClient
    implements JedisCommands, JedisBinaryCommands, SampleKeyedCommands, SampleBinaryKeyedCommands,
    RediSearchCommands, RedisJsonV2Commands, RedisTimeSeriesCommands, RedisBloomCommands, AutoCloseable {

  /**
   * Get the command objects factory for this client.
   * @return CommandObjects instance
   */
  protected abstract CommandObjects getCommandObjects();

  /**
   * Get the connection provider for this client.
   * @return ConnectionProvider instance
   */
  protected abstract ConnectionProvider getConnectionProvider();

  /**
   * Get the cache for this client.
   * @return Cache instance
   */
  public abstract Cache getCache();

  /**
   * Execute a Redis command.
   * @param <T> The return type of the command
   * @param commandObject The command to execute
   * @return The command result
   */
  public abstract <T> T executeCommand(CommandObject<T> commandObject);

  /**
   * Broadcast a Redis command to all nodes in a cluster.
   * @param <T> The return type of the command
   * @param commandObject The command to broadcast
   * @return The command result
   */
  public abstract <T> T broadcastCommand(CommandObject<T> commandObject);

  /**
   * Check if a command should be broadcast and execute accordingly.
   * @param <T> The return type of the command
   * @param commandObject The command to execute
   * @return The command result
   */
  protected abstract <T> T checkAndBroadcastCommand(CommandObject<T> commandObject);

  /**
   * Create a new pipeline for batching commands.
   * @return a new AbstractPipeline instance
   */
  public abstract AbstractPipeline pipelined();

  /**
   * Create a new transaction.
   * @return a new AbstractTransaction instance
   */
  public AbstractTransaction multi() {
    return transaction(true);
  }

  /**
   * Create a new transaction with optional MULTI command.
   * @param doMulti whether to execute MULTI command
   * @return a new AbstractTransaction instance
   */
  public abstract AbstractTransaction transaction(boolean doMulti);

  // Generic sendCommand methods for raw protocol commands
  public Object sendCommand(ProtocolCommand cmd) {
    return executeCommand(
      new CommandObject<>(getCommandObjects().commandArguments(cmd), BuilderFactory.RAW_OBJECT));
  }

  public Object sendCommand(ProtocolCommand cmd, byte[]... args) {
    return executeCommand(
      new CommandObject<>(getCommandObjects().commandArguments(cmd).addObjects((Object[]) args),
          BuilderFactory.RAW_OBJECT));
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, byte[]... args) {
    return executeCommand(new CommandObject<>(
        getCommandObjects().commandArguments(cmd).addObjects((Object[]) args).blocking(),
        BuilderFactory.RAW_OBJECT));
  }

  public Object sendCommand(ProtocolCommand cmd, String... args) {
    return executeCommand(
      new CommandObject<>(getCommandObjects().commandArguments(cmd).addObjects((Object[]) args),
          BuilderFactory.RAW_OBJECT));
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, String... args) {
    return executeCommand(new CommandObject<>(
        getCommandObjects().commandArguments(cmd).addObjects((Object[]) args).blocking(),
        BuilderFactory.RAW_OBJECT));
  }

  public Object sendCommand(byte[] sampleKey, ProtocolCommand cmd, byte[]... args) {
    return executeCommand(new CommandObject<>(
        getCommandObjects().commandArguments(cmd).addObjects((Object[]) args).processKey(sampleKey),
        BuilderFactory.RAW_OBJECT));
  }

  public Object sendBlockingCommand(byte[] sampleKey, ProtocolCommand cmd, byte[]... args) {
    return executeCommand(new CommandObject<>(getCommandObjects().commandArguments(cmd)
        .addObjects((Object[]) args).blocking().processKey(sampleKey), BuilderFactory.RAW_OBJECT));
  }

  public Object sendCommand(String sampleKey, ProtocolCommand cmd, String... args) {
    return executeCommand(new CommandObject<>(
        getCommandObjects().commandArguments(cmd).addObjects((Object[]) args).processKey(sampleKey),
        BuilderFactory.RAW_OBJECT));
  }

  public Object sendBlockingCommand(String sampleKey, ProtocolCommand cmd, String... args) {
    return executeCommand(new CommandObject<>(getCommandObjects().commandArguments(cmd)
        .addObjects((Object[]) args).blocking().processKey(sampleKey), BuilderFactory.RAW_OBJECT));
  }

  // Random node commands
  public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    try (Connection connection = getConnectionProvider().getConnection()) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  public void psubscribe(final JedisPubSub jedisPubSub, final String... patterns) {
    try (Connection connection = getConnectionProvider().getConnection()) {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    }
  }

  public void subscribe(BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    try (Connection connection = getConnectionProvider().getConnection()) {
      jedisPubSub.proceed(connection, channels);
    }
  }

  public void psubscribe(BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    try (Connection connection = getConnectionProvider().getConnection()) {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    }
  }
  // Random node commands

  // Redis command methods
  public String ping() {
    return checkAndBroadcastCommand(getCommandObjects().ping());
  }

  public String flushDB() {
    return checkAndBroadcastCommand(getCommandObjects().flushDB());
  }

  public String flushAll() {
    return checkAndBroadcastCommand(getCommandObjects().flushAll());
  }

  public String configSet(String parameter, String value) {
    return checkAndBroadcastCommand(getCommandObjects().configSet(parameter, value));
  }

  public String info() {
    return executeCommand(getCommandObjects().info());
  }

  public String info(String section) {
    return executeCommand(getCommandObjects().info(section));
  }

  // Key commands
  @Override
  public boolean exists(String key) {
    return executeCommand(getCommandObjects().exists(key));
  }

  @Override
  public long exists(String... keys) {
    return executeCommand(getCommandObjects().exists(keys));
  }

  @Override
  public long persist(String key) {
    return executeCommand(getCommandObjects().persist(key));
  }

  @Override
  public String type(String key) {
    return executeCommand(getCommandObjects().type(key));
  }

  @Override
  public boolean exists(byte[] key) {
    return executeCommand(getCommandObjects().exists(key));
  }

  @Override
  public long exists(byte[]... keys) {
    return executeCommand(getCommandObjects().exists(keys));
  }

  @Override
  public long persist(byte[] key) {
    return executeCommand(getCommandObjects().persist(key));
  }

  @Override
  public String type(byte[] key) {
    return executeCommand(getCommandObjects().type(key));
  }

  @Override
  public byte[] dump(String key) {
    return executeCommand(getCommandObjects().dump(key));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue) {
    return executeCommand(getCommandObjects().restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(getCommandObjects().restore(key, ttl, serializedValue, params));
  }

  @Override
  public byte[] dump(byte[] key) {
    return executeCommand(getCommandObjects().dump(key));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue) {
    return executeCommand(getCommandObjects().restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(getCommandObjects().restore(key, ttl, serializedValue, params));
  }

  @Override
  public long expire(String key, long seconds) {
    return executeCommand(getCommandObjects().expire(key, seconds));
  }

  @Override
  public long expire(String key, long seconds, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().expire(key, seconds, expiryOption));
  }

  @Override
  public long pexpire(String key, long milliseconds) {
    return executeCommand(getCommandObjects().pexpire(key, milliseconds));
  }

  @Override
  public long pexpire(String key, long milliseconds, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().pexpire(key, milliseconds, expiryOption));
  }

  @Override
  public long expireTime(String key) {
    return executeCommand(getCommandObjects().expireTime(key));
  }

  @Override
  public long pexpireTime(String key) {
    return executeCommand(getCommandObjects().pexpireTime(key));
  }

  @Override
  public long expireAt(String key, long unixTime) {
    return executeCommand(getCommandObjects().expireAt(key, unixTime));
  }

  @Override
  public long expireAt(String key, long unixTime, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().expireAt(key, unixTime, expiryOption));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp) {
    return executeCommand(getCommandObjects().pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().pexpireAt(key, millisecondsTimestamp, expiryOption));
  }

  @Override
  public long expire(byte[] key, long seconds) {
    return executeCommand(getCommandObjects().expire(key, seconds));
  }

  @Override
  public long expire(byte[] key, long seconds, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().expire(key, seconds, expiryOption));
  }

  @Override
  public long pexpire(byte[] key, long milliseconds) {
    return executeCommand(getCommandObjects().pexpire(key, milliseconds));
  }

  @Override
  public long pexpire(byte[] key, long milliseconds, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().pexpire(key, milliseconds, expiryOption));
  }

  @Override
  public long expireTime(byte[] key) {
    return executeCommand(getCommandObjects().expireTime(key));
  }

  @Override
  public long pexpireTime(byte[] key) {
    return executeCommand(getCommandObjects().pexpireTime(key));
  }

  @Override
  public long expireAt(byte[] key, long unixTime) {
    return executeCommand(getCommandObjects().expireAt(key, unixTime));
  }

  @Override
  public long expireAt(byte[] key, long unixTime, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().expireAt(key, unixTime, expiryOption));
  }

  @Override
  public long pexpireAt(byte[] key, long millisecondsTimestamp) {
    return executeCommand(getCommandObjects().pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pexpireAt(byte[] key, long millisecondsTimestamp, ExpiryOption expiryOption) {
    return executeCommand(getCommandObjects().pexpireAt(key, millisecondsTimestamp, expiryOption));
  }

  @Override
  public long ttl(String key) {
    return executeCommand(getCommandObjects().ttl(key));
  }

  @Override
  public long pttl(String key) {
    return executeCommand(getCommandObjects().pttl(key));
  }

  @Override
  public long touch(String key) {
    return executeCommand(getCommandObjects().touch(key));
  }

  @Override
  public long touch(String... keys) {
    return executeCommand(getCommandObjects().touch(keys));
  }

  @Override
  public long ttl(byte[] key) {
    return executeCommand(getCommandObjects().ttl(key));
  }

  @Override
  public long pttl(byte[] key) {
    return executeCommand(getCommandObjects().pttl(key));
  }

  @Override
  public long touch(byte[] key) {
    return executeCommand(getCommandObjects().touch(key));
  }

  @Override
  public long touch(byte[]... keys) {
    return executeCommand(getCommandObjects().touch(keys));
  }

  @Override
  public List<String> sort(String key) {
    return executeCommand(getCommandObjects().sort(key));
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParams) {
    return executeCommand(getCommandObjects().sort(key, sortingParams));
  }

  @Override
  public long sort(String key, String dstkey) {
    return executeCommand(getCommandObjects().sort(key, dstkey));
  }

  @Override
  public long sort(String key, SortingParams sortingParams, String dstkey) {
    return executeCommand(getCommandObjects().sort(key, sortingParams, dstkey));
  }

  @Override
  public List<String> sortReadonly(String key, SortingParams sortingParams) {
    return executeCommand(getCommandObjects().sortReadonly(key, sortingParams));
  }

  @Override
  public List<byte[]> sort(byte[] key) {
    return executeCommand(getCommandObjects().sort(key));
  }

  @Override
  public List<byte[]> sort(byte[] key, SortingParams sortingParams) {
    return executeCommand(getCommandObjects().sort(key, sortingParams));
  }

  @Override
  public long sort(byte[] key, byte[] dstkey) {
    return executeCommand(getCommandObjects().sort(key, dstkey));
  }

  @Override
  public List<byte[]> sortReadonly(byte[] key, SortingParams sortingParams) {
    return executeCommand(getCommandObjects().sortReadonly(key, sortingParams));
  }

  @Override
  public long sort(byte[] key, SortingParams sortingParams, byte[] dstkey) {
    return executeCommand(getCommandObjects().sort(key, sortingParams, dstkey));
  }

  @Override
  public long del(String key) {
    return executeCommand(getCommandObjects().del(key));
  }

  @Override
  public long del(String... keys) {
    return executeCommand(getCommandObjects().del(keys));
  }

  @Override
  public long unlink(String key) {
    return executeCommand(getCommandObjects().unlink(key));
  }

  @Override
  public long unlink(String... keys) {
    return executeCommand(getCommandObjects().unlink(keys));
  }

  @Override
  public long del(byte[] key) {
    return executeCommand(getCommandObjects().del(key));
  }

  @Override
  public long del(byte[]... keys) {
    return executeCommand(getCommandObjects().del(keys));
  }

  @Override
  public long unlink(byte[] key) {
    return executeCommand(getCommandObjects().unlink(key));
  }

  @Override
  public long unlink(byte[]... keys) {
    return executeCommand(getCommandObjects().unlink(keys));
  }

  @Override
  public Long memoryUsage(String key) {
    return executeCommand(getCommandObjects().memoryUsage(key));
  }

  @Override
  public Long memoryUsage(String key, int samples) {
    return executeCommand(getCommandObjects().memoryUsage(key, samples));
  }

  @Override
  public Long memoryUsage(byte[] key) {
    return executeCommand(getCommandObjects().memoryUsage(key));
  }

  @Override
  public Long memoryUsage(byte[] key, int samples) {
    return executeCommand(getCommandObjects().memoryUsage(key, samples));
  }

  @Override
  public boolean copy(String srcKey, String dstKey, boolean replace) {
    return executeCommand(getCommandObjects().copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(String oldkey, String newkey) {
    return executeCommand(getCommandObjects().rename(oldkey, newkey));
  }

  @Override
  public long renamenx(String oldkey, String newkey) {
    return executeCommand(getCommandObjects().renamenx(oldkey, newkey));
  }

  @Override
  public boolean copy(byte[] srcKey, byte[] dstKey, boolean replace) {
    return executeCommand(getCommandObjects().copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    return executeCommand(getCommandObjects().rename(oldkey, newkey));
  }

  @Override
  public long renamenx(byte[] oldkey, byte[] newkey) {
    return executeCommand(getCommandObjects().renamenx(oldkey, newkey));
  }

  public long dbSize() {
    return executeCommand(getCommandObjects().dbSize());
  }

  @Override
  public Set<String> keys(String pattern) {
    return executeCommand(getCommandObjects().keys(pattern));
  }

  @Override
  public ScanResult<String> scan(String cursor) {
    return executeCommand(getCommandObjects().scan(cursor));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params) {
    return executeCommand(getCommandObjects().scan(cursor, params));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params, String type) {
    return executeCommand(getCommandObjects().scan(cursor, params, type));
  }

  /**
   * @param batchCount COUNT for each batch execution
   * @param match pattern
   * @return scan iteration
   */
  public ScanIteration scanIteration(int batchCount, String match) {
    return new ScanIteration(getConnectionProvider(), batchCount, match);
  }

  /**
   * @param batchCount COUNT for each batch execution
   * @param match pattern
   * @param type key type
   * @return scan iteration
   */
  public ScanIteration scanIteration(int batchCount, String match, String type) {
    return new ScanIteration(getConnectionProvider(), batchCount, match, type);
  }

  @Override
  public Set<byte[]> keys(byte[] pattern) {
    return executeCommand(getCommandObjects().keys(pattern));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor) {
    return executeCommand(getCommandObjects().scan(cursor));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params) {
    return executeCommand(getCommandObjects().scan(cursor, params));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type) {
    return executeCommand(getCommandObjects().scan(cursor, params, type));
  }

  @Override
  public String randomKey() {
    return executeCommand(getCommandObjects().randomKey());
  }

  @Override
  public byte[] randomBinaryKey() {
    return executeCommand(getCommandObjects().randomBinaryKey());
  }

  @Override
  public String psetex(String key, long milliseconds, String value) {
    return executeCommand(getCommandObjects().psetex(key, milliseconds, value));
  }

  @Override
  public String psetex(byte[] key, long milliseconds, byte[] value) {
    return executeCommand(getCommandObjects().psetex(key, milliseconds, value));
  }

  @Override
  public long incr(String key) {
    return executeCommand(getCommandObjects().incr(key));
  }

  @Override
  public long incrBy(String key, long increment) {
    return executeCommand(getCommandObjects().incrBy(key, increment));
  }

  @Override
  public double incrByFloat(String key, double increment) {
    return executeCommand(getCommandObjects().incrByFloat(key, increment));
  }

  @Override
  public long decr(String key) {
    return executeCommand(getCommandObjects().decr(key));
  }

  @Override
  public long decrBy(String key, long decrement) {
    return executeCommand(getCommandObjects().decrBy(key, decrement));
  }

  @Override
  public long incr(byte[] key) {
    return executeCommand(getCommandObjects().incr(key));
  }

  @Override
  public long incrBy(byte[] key, long increment) {
    return executeCommand(getCommandObjects().incrBy(key, increment));
  }

  @Override
  public double incrByFloat(byte[] key, double increment) {
    return executeCommand(getCommandObjects().incrByFloat(key, increment));
  }

  @Override
  public long decr(byte[] key) {
    return executeCommand(getCommandObjects().decr(key));
  }

  @Override
  public long decrBy(byte[] key, long decrement) {
    return executeCommand(getCommandObjects().decrBy(key, decrement));
  }

  @Override
  public String set(String key, String value) {
    return executeCommand(getCommandObjects().set(key, value));
  }

  @Override
  public String set(String key, String value, SetParams params) {
    return executeCommand(getCommandObjects().set(key, value, params));
  }

  @Override
  public String get(String key) {
    return executeCommand(getCommandObjects().get(key));
  }

  @Override
  public String setGet(String key, String value) {
    return executeCommand(getCommandObjects().setGet(key, value));
  }

  @Override
  public String setGet(String key, String value, SetParams params) {
    return executeCommand(getCommandObjects().setGet(key, value, params));
  }

  @Override
  public String getDel(String key) {
    return executeCommand(getCommandObjects().getDel(key));
  }

  @Override
  public String getEx(String key, GetExParams params) {
    return executeCommand(getCommandObjects().getEx(key, params));
  }

  @Override
  public String set(byte[] key, byte[] value) {
    return executeCommand(getCommandObjects().set(key, value));
  }

  @Override
  public String set(byte[] key, byte[] value, SetParams params) {
    return executeCommand(getCommandObjects().set(key, value, params));
  }

  @Override
  public byte[] get(byte[] key) {
    return executeCommand(getCommandObjects().get(key));
  }

  @Override
  public byte[] setGet(byte[] key, byte[] value) {
    return executeCommand(getCommandObjects().setGet(key, value));
  }

  @Override
  public byte[] setGet(byte[] key, byte[] value, SetParams params) {
    return executeCommand(getCommandObjects().setGet(key, value, params));
  }

  @Override
  public byte[] getDel(byte[] key) {
    return executeCommand(getCommandObjects().getDel(key));
  }

  @Override
  public byte[] getEx(byte[] key, GetExParams params) {
    return executeCommand(getCommandObjects().getEx(key, params));
  }

  @Override
  public boolean setbit(String key, long offset, boolean value) {
    return executeCommand(getCommandObjects().setbit(key, offset, value));
  }

  @Override
  public boolean getbit(String key, long offset) {
    return executeCommand(getCommandObjects().getbit(key, offset));
  }

  @Override
  public long setrange(String key, long offset, String value) {
    return executeCommand(getCommandObjects().setrange(key, offset, value));
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    return executeCommand(getCommandObjects().getrange(key, startOffset, endOffset));
  }

  @Override
  public boolean setbit(byte[] key, long offset, boolean value) {
    return executeCommand(getCommandObjects().setbit(key, offset, value));
  }

  @Override
  public boolean getbit(byte[] key, long offset) {
    return executeCommand(getCommandObjects().getbit(key, offset));
  }

  @Override
  public long setnx(String key, String value) {
    return executeCommand(getCommandObjects().setnx(key, value));
  }

  @Override
  public String setex(String key, long seconds, String value) {
    return executeCommand(getCommandObjects().setex(key, seconds, value));
  }

  @Override
  public long setnx(byte[] key, byte[] value) {
    return executeCommand(getCommandObjects().setnx(key, value));
  }

  @Override
  public String setex(byte[] key, long seconds, byte[] value) {
    return executeCommand(getCommandObjects().setex(key, seconds, value));
  }

  @Override
  public long setrange(byte[] key, long offset, byte[] value) {
    return executeCommand(getCommandObjects().setrange(key, offset, value));
  }

  @Override
  public byte[] getrange(byte[] key, long startOffset, long endOffset) {
    return executeCommand(getCommandObjects().getrange(key, startOffset, endOffset));
  }

  @Override
  public List<String> mget(String... keys) {
    return executeCommand(getCommandObjects().mget(keys));
  }

  @Override
  public String mset(String... keysvalues) {
    return executeCommand(getCommandObjects().mset(keysvalues));
  }

  @Override
  public long msetnx(String... keysvalues) {
    return executeCommand(getCommandObjects().msetnx(keysvalues));
  }

  @Override
  public List<byte[]> mget(byte[]... keys) {
    return executeCommand(getCommandObjects().mget(keys));
  }

  @Override
  public String mset(byte[]... keysvalues) {
    return executeCommand(getCommandObjects().mset(keysvalues));
  }

  @Override
  public long msetnx(byte[]... keysvalues) {
    return executeCommand(getCommandObjects().msetnx(keysvalues));
  }

  @Override
  public long append(String key, String value) {
    return executeCommand(getCommandObjects().append(key, value));
  }

  @Override
  public String substr(String key, int start, int end) {
    return executeCommand(getCommandObjects().substr(key, start, end));
  }

  @Override
  public long strlen(String key) {
    return executeCommand(getCommandObjects().strlen(key));
  }

  @Override
  public long append(byte[] key, byte[] value) {
    return executeCommand(getCommandObjects().append(key, value));
  }

  @Override
  public byte[] substr(byte[] key, int start, int end) {
    return executeCommand(getCommandObjects().substr(key, start, end));
  }

  @Override
  public long strlen(byte[] key) {
    return executeCommand(getCommandObjects().strlen(key));
  }

  @Override
  public long bitcount(String key) {
    return executeCommand(getCommandObjects().bitcount(key));
  }

  @Override
  public long bitcount(String key, long start, long end) {
    return executeCommand(getCommandObjects().bitcount(key, start, end));
  }

  @Override
  public long bitcount(String key, long start, long end, BitCountOption option) {
    return executeCommand(getCommandObjects().bitcount(key, start, end, option));
  }

  @Override
  public long bitpos(String key, boolean value) {
    return executeCommand(getCommandObjects().bitpos(key, value));
  }

  @Override
  public long bitpos(String key, boolean value, BitPosParams params) {
    return executeCommand(getCommandObjects().bitpos(key, value, params));
  }

  @Override
  public long bitcount(byte[] key) {
    return executeCommand(getCommandObjects().bitcount(key));
  }

  @Override
  public long bitcount(byte[] key, long start, long end) {
    return executeCommand(getCommandObjects().bitcount(key, start, end));
  }

  @Override
  public long bitcount(byte[] key, long start, long end, BitCountOption option) {
    return executeCommand(getCommandObjects().bitcount(key, start, end, option));
  }

  @Override
  public long bitpos(byte[] key, boolean value) {
    return executeCommand(getCommandObjects().bitpos(key, value));
  }

  @Override
  public long bitpos(byte[] key, boolean value, BitPosParams params) {
    return executeCommand(getCommandObjects().bitpos(key, value, params));
  }

  @Override
  public List<Long> bitfield(String key, String... arguments) {
    return executeCommand(getCommandObjects().bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(String key, String... arguments) {
    return executeCommand(getCommandObjects().bitfieldReadonly(key, arguments));
  }

  @Override
  public List<Long> bitfield(byte[] key, byte[]... arguments) {
    return executeCommand(getCommandObjects().bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(byte[] key, byte[]... arguments) {
    return executeCommand(getCommandObjects().bitfieldReadonly(key, arguments));
  }

  @Override
  public long bitop(BitOP op, String destKey, String... srcKeys) {
    return executeCommand(getCommandObjects().bitop(op, destKey, srcKeys));
  }

  @Override
  public long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    return executeCommand(getCommandObjects().bitop(op, destKey, srcKeys));
  }

  @Override
  public LCSMatchResult lcs(String keyA, String keyB, LCSParams params) {
    return executeCommand(getCommandObjects().lcs(keyA, keyB, params));
  }

  @Override
  public LCSMatchResult lcs(byte[] keyA, byte[] keyB, LCSParams params) {
    return executeCommand(getCommandObjects().lcs(keyA, keyB, params));
  }

  // List commands
  @Override
  public long rpush(String key, String... string) {
    return executeCommand(getCommandObjects().rpush(key, string));
  }

  @Override
  public long lpush(String key, String... string) {
    return executeCommand(getCommandObjects().lpush(key, string));
  }

  @Override
  public long llen(String key) {
    return executeCommand(getCommandObjects().llen(key));
  }

  @Override
  public List<String> lrange(String key, long start, long stop) {
    return executeCommand(getCommandObjects().lrange(key, start, stop));
  }

  @Override
  public String ltrim(String key, long start, long stop) {
    return executeCommand(getCommandObjects().ltrim(key, start, stop));
  }

  @Override
  public String lindex(String key, long index) {
    return executeCommand(getCommandObjects().lindex(key, index));
  }

  @Override
  public long rpush(byte[] key, byte[]... args) {
    return executeCommand(getCommandObjects().rpush(key, args));
  }

  @Override
  public long lpush(byte[] key, byte[]... args) {
    return executeCommand(getCommandObjects().lpush(key, args));
  }

  @Override
  public long llen(byte[] key) {
    return executeCommand(getCommandObjects().llen(key));
  }

  @Override
  public List<byte[]> lrange(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().lrange(key, start, stop));
  }

  @Override
  public String ltrim(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().ltrim(key, start, stop));
  }

  @Override
  public byte[] lindex(byte[] key, long index) {
    return executeCommand(getCommandObjects().lindex(key, index));
  }

  @Override
  public String lset(String key, long index, String value) {
    return executeCommand(getCommandObjects().lset(key, index, value));
  }

  @Override
  public long lrem(String key, long count, String value) {
    return executeCommand(getCommandObjects().lrem(key, count, value));
  }

  @Override
  public String lpop(String key) {
    return executeCommand(getCommandObjects().lpop(key));
  }

  @Override
  public List<String> lpop(String key, int count) {
    return executeCommand(getCommandObjects().lpop(key, count));
  }

  @Override
  public String lset(byte[] key, long index, byte[] value) {
    return executeCommand(getCommandObjects().lset(key, index, value));
  }

  @Override
  public long lrem(byte[] key, long count, byte[] value) {
    return executeCommand(getCommandObjects().lrem(key, count, value));
  }

  @Override
  public byte[] lpop(byte[] key) {
    return executeCommand(getCommandObjects().lpop(key));
  }

  @Override
  public List<byte[]> lpop(byte[] key, int count) {
    return executeCommand(getCommandObjects().lpop(key, count));
  }

  @Override
  public Long lpos(String key, String element) {
    return executeCommand(getCommandObjects().lpos(key, element));
  }

  @Override
  public Long lpos(String key, String element, LPosParams params) {
    return executeCommand(getCommandObjects().lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(String key, String element, LPosParams params, long count) {
    return executeCommand(getCommandObjects().lpos(key, element, params, count));
  }

  @Override
  public Long lpos(byte[] key, byte[] element) {
    return executeCommand(getCommandObjects().lpos(key, element));
  }

  @Override
  public Long lpos(byte[] key, byte[] element, LPosParams params) {
    return executeCommand(getCommandObjects().lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(byte[] key, byte[] element, LPosParams params, long count) {
    return executeCommand(getCommandObjects().lpos(key, element, params, count));
  }

  @Override
  public String rpop(String key) {
    return executeCommand(getCommandObjects().rpop(key));
  }

  @Override
  public List<String> rpop(String key, int count) {
    return executeCommand(getCommandObjects().rpop(key, count));
  }

  @Override
  public byte[] rpop(byte[] key) {
    return executeCommand(getCommandObjects().rpop(key));
  }

  @Override
  public List<byte[]> rpop(byte[] key, int count) {
    return executeCommand(getCommandObjects().rpop(key, count));
  }

  @Override
  public long linsert(String key, ListPosition where, String pivot, String value) {
    return executeCommand(getCommandObjects().linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(String key, String... strings) {
    return executeCommand(getCommandObjects().lpushx(key, strings));
  }

  @Override
  public long rpushx(String key, String... strings) {
    return executeCommand(getCommandObjects().rpushx(key, strings));
  }

  @Override
  public long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
    return executeCommand(getCommandObjects().linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(byte[] key, byte[]... args) {
    return executeCommand(getCommandObjects().lpushx(key, args));
  }

  @Override
  public long rpushx(byte[] key, byte[]... args) {
    return executeCommand(getCommandObjects().rpushx(key, args));
  }

  @Override
  public List<String> blpop(int timeout, String key) {
    return executeCommand(getCommandObjects().blpop(timeout, key));
  }

  @Override
  public KeyValue<String, String> blpop(double timeout, String key) {
    return executeCommand(getCommandObjects().blpop(timeout, key));
  }

  @Override
  public List<String> brpop(int timeout, String key) {
    return executeCommand(getCommandObjects().brpop(timeout, key));
  }

  @Override
  public KeyValue<String, String> brpop(double timeout, String key) {
    return executeCommand(getCommandObjects().brpop(timeout, key));
  }

  @Override
  public List<String> blpop(int timeout, String... keys) {
    return executeCommand(getCommandObjects().blpop(timeout, keys));
  }

  @Override
  public KeyValue<String, String> blpop(double timeout, String... keys) {
    return executeCommand(getCommandObjects().blpop(timeout, keys));
  }

  @Override
  public List<String> brpop(int timeout, String... keys) {
    return executeCommand(getCommandObjects().brpop(timeout, keys));
  }

  @Override
  public KeyValue<String, String> brpop(double timeout, String... keys) {
    return executeCommand(getCommandObjects().brpop(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(int timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().blpop(timeout, keys));
  }

  @Override
  public KeyValue<byte[], byte[]> blpop(double timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().blpop(timeout, keys));
  }

  @Override
  public List<byte[]> brpop(int timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().brpop(timeout, keys));
  }

  @Override
  public KeyValue<byte[], byte[]> brpop(double timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().brpop(timeout, keys));
  }

  @Override
  public String rpoplpush(String srckey, String dstkey) {
    return executeCommand(getCommandObjects().rpoplpush(srckey, dstkey));
  }

  @Override
  public String brpoplpush(String source, String destination, int timeout) {
    return executeCommand(getCommandObjects().brpoplpush(source, destination, timeout));
  }

  @Override
  public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
    return executeCommand(getCommandObjects().rpoplpush(srckey, dstkey));
  }

  @Override
  public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
    return executeCommand(getCommandObjects().brpoplpush(source, destination, timeout));
  }

  @Override
  public String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return executeCommand(getCommandObjects().lmove(srcKey, dstKey, from, to));
  }

  @Override
  public String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to,
      double timeout) {
    return executeCommand(getCommandObjects().blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    return executeCommand(getCommandObjects().lmove(srcKey, dstKey, from, to));
  }

  @Override
  public byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to,
      double timeout) {
    return executeCommand(getCommandObjects().blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public KeyValue<String, List<String>> lmpop(ListDirection direction, String... keys) {
    return executeCommand(getCommandObjects().lmpop(direction, keys));
  }

  @Override
  public KeyValue<String, List<String>> lmpop(ListDirection direction, int count, String... keys) {
    return executeCommand(getCommandObjects().lmpop(direction, count, keys));
  }

  @Override
  public KeyValue<String, List<String>> blmpop(double timeout, ListDirection direction,
      String... keys) {
    return executeCommand(getCommandObjects().blmpop(timeout, direction, keys));
  }

  @Override
  public KeyValue<String, List<String>> blmpop(double timeout, ListDirection direction, int count,
      String... keys) {
    return executeCommand(getCommandObjects().blmpop(timeout, direction, count, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, byte[]... keys) {
    return executeCommand(getCommandObjects().lmpop(direction, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> lmpop(ListDirection direction, int count, byte[]... keys) {
    return executeCommand(getCommandObjects().lmpop(direction, count, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> blmpop(double timeout, ListDirection direction,
      byte[]... keys) {
    return executeCommand(getCommandObjects().blmpop(timeout, direction, keys));
  }

  @Override
  public KeyValue<byte[], List<byte[]>> blmpop(double timeout, ListDirection direction, int count,
      byte[]... keys) {
    return executeCommand(getCommandObjects().blmpop(timeout, direction, count, keys));
  }

  // Hash commands
  @Override
  public long hset(String key, String field, String value) {
    return executeCommand(getCommandObjects().hset(key, field, value));
  }

  @Override
  public long hset(String key, Map<String, String> hash) {
    return executeCommand(getCommandObjects().hset(key, hash));
  }

  @Override
  public long hsetex(String key, HSetExParams params, String field, String value) {
    return executeCommand(getCommandObjects().hsetex(key, params, field, value));
  }

  @Override
  public long hsetex(String key, HSetExParams params, Map<String, String> hash) {
    return executeCommand(getCommandObjects().hsetex(key, params, hash));
  }

  @Override
  public String hget(String key, String field) {
    return executeCommand(getCommandObjects().hget(key, field));
  }

  @Override
  public List<String> hgetex(String key, HGetExParams params, String... fields) {
    return executeCommand(getCommandObjects().hgetex(key, params, fields));
  }

  @Override
  public List<String> hgetdel(String key, String... fields) {
    return executeCommand(getCommandObjects().hgetdel(key, fields));
  }

  @Override
  public long hsetnx(String key, String field, String value) {
    return executeCommand(getCommandObjects().hsetnx(key, field, value));
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    return executeCommand(getCommandObjects().hmset(key, hash));
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    return executeCommand(getCommandObjects().hmget(key, fields));
  }

  @Override
  public long hset(byte[] key, byte[] field, byte[] value) {
    return executeCommand(getCommandObjects().hset(key, field, value));
  }

  @Override
  public long hset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(getCommandObjects().hset(key, hash));
  }

  @Override
  public long hsetex(byte[] key, HSetExParams params, byte[] field, byte[] value) {
    return executeCommand(getCommandObjects().hsetex(key, params, field, value));
  }

  @Override
  public long hsetex(byte[] key, HSetExParams params, Map<byte[], byte[]> hash) {
    return executeCommand(getCommandObjects().hsetex(key, params, hash));
  }

  @Override
  public byte[] hget(byte[] key, byte[] field) {
    return executeCommand(getCommandObjects().hget(key, field));
  }

  @Override
  public List<byte[]> hgetex(byte[] key, HGetExParams params, byte[]... fields) {
    return executeCommand(getCommandObjects().hgetex(key, params, fields));
  }

  @Override
  public List<byte[]> hgetdel(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hgetdel(key, fields));
  }

  @Override
  public long hsetnx(byte[] key, byte[] field, byte[] value) {
    return executeCommand(getCommandObjects().hsetnx(key, field, value));
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(getCommandObjects().hmset(key, hash));
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hmget(key, fields));
  }

  @Override
  public long hincrBy(String key, String field, long value) {
    return executeCommand(getCommandObjects().hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(String key, String field, double value) {
    return executeCommand(getCommandObjects().hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(String key, String field) {
    return executeCommand(getCommandObjects().hexists(key, field));
  }

  @Override
  public long hdel(String key, String... field) {
    return executeCommand(getCommandObjects().hdel(key, field));
  }

  @Override
  public long hlen(String key) {
    return executeCommand(getCommandObjects().hlen(key));
  }

  @Override
  public long hincrBy(byte[] key, byte[] field, long value) {
    return executeCommand(getCommandObjects().hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(byte[] key, byte[] field, double value) {
    return executeCommand(getCommandObjects().hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(byte[] key, byte[] field) {
    return executeCommand(getCommandObjects().hexists(key, field));
  }

  @Override
  public long hdel(byte[] key, byte[]... field) {
    return executeCommand(getCommandObjects().hdel(key, field));
  }

  @Override
  public long hlen(byte[] key) {
    return executeCommand(getCommandObjects().hlen(key));
  }

  @Override
  public Set<String> hkeys(String key) {
    return executeCommand(getCommandObjects().hkeys(key));
  }

  @Override
  public List<String> hvals(String key) {
    return executeCommand(getCommandObjects().hvals(key));
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    return executeCommand(getCommandObjects().hgetAll(key));
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return executeCommand(getCommandObjects().hkeys(key));
  }

  @Override
  public List<byte[]> hvals(byte[] key) {
    return executeCommand(getCommandObjects().hvals(key));
  }

  @Override
  public Map<byte[], byte[]> hgetAll(byte[] key) {
    return executeCommand(getCommandObjects().hgetAll(key));
  }

  @Override
  public String hrandfield(String key) {
    return executeCommand(getCommandObjects().hrandfield(key));
  }

  @Override
  public List<String> hrandfield(String key, long count) {
    return executeCommand(getCommandObjects().hrandfield(key, count));
  }

  @Override
  public List<Map.Entry<String, String>> hrandfieldWithValues(String key, long count) {
    return executeCommand(getCommandObjects().hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
    return executeCommand(getCommandObjects().hscan(key, cursor, params));
  }

  @Override
  public ScanResult<String> hscanNoValues(String key, String cursor, ScanParams params) {
    return executeCommand(getCommandObjects().hscanNoValues(key, cursor, params));
  }

  @Override
  public long hstrlen(String key, String field) {
    return executeCommand(getCommandObjects().hstrlen(key, field));
  }

  @Override
  public byte[] hrandfield(byte[] key) {
    return executeCommand(getCommandObjects().hrandfield(key));
  }

  @Override
  public List<byte[]> hrandfield(byte[] key, long count) {
    return executeCommand(getCommandObjects().hrandfield(key, count));
  }

  @Override
  public List<Map.Entry<byte[], byte[]>> hrandfieldWithValues(byte[] key, long count) {
    return executeCommand(getCommandObjects().hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(getCommandObjects().hscan(key, cursor, params));
  }

  @Override
  public ScanResult<byte[]> hscanNoValues(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(getCommandObjects().hscanNoValues(key, cursor, params));
  }

  @Override
  public long hstrlen(byte[] key, byte[] field) {
    return executeCommand(getCommandObjects().hstrlen(key, field));
  }

  @Override
  public List<Long> hexpire(String key, long seconds, String... fields) {
    return executeCommand(getCommandObjects().hexpire(key, seconds, fields));
  }

  @Override
  public List<Long> hexpire(String key, long seconds, ExpiryOption condition, String... fields) {
    return executeCommand(getCommandObjects().hexpire(key, seconds, condition, fields));
  }

  @Override
  public List<Long> hpexpire(String key, long milliseconds, String... fields) {
    return executeCommand(getCommandObjects().hpexpire(key, milliseconds, fields));
  }

  @Override
  public List<Long> hpexpire(String key, long milliseconds, ExpiryOption condition,
      String... fields) {
    return executeCommand(getCommandObjects().hpexpire(key, milliseconds, condition, fields));
  }

  @Override
  public List<Long> hexpireAt(String key, long unixTimeSeconds, String... fields) {
    return executeCommand(getCommandObjects().hexpireAt(key, unixTimeSeconds, fields));
  }

  @Override
  public List<Long> hexpireAt(String key, long unixTimeSeconds, ExpiryOption condition,
      String... fields) {
    return executeCommand(getCommandObjects().hexpireAt(key, unixTimeSeconds, condition, fields));
  }

  @Override
  public List<Long> hpexpireAt(String key, long unixTimeMillis, String... fields) {
    return executeCommand(getCommandObjects().hpexpireAt(key, unixTimeMillis, fields));
  }

  @Override
  public List<Long> hpexpireAt(String key, long unixTimeMillis, ExpiryOption condition,
      String... fields) {
    return executeCommand(getCommandObjects().hpexpireAt(key, unixTimeMillis, condition, fields));
  }

  @Override
  public List<Long> hexpire(byte[] key, long seconds, byte[]... fields) {
    return executeCommand(getCommandObjects().hexpire(key, seconds, fields));
  }

  @Override
  public List<Long> hexpire(byte[] key, long seconds, ExpiryOption condition, byte[]... fields) {
    return executeCommand(getCommandObjects().hexpire(key, seconds, condition, fields));
  }

  @Override
  public List<Long> hpexpire(byte[] key, long milliseconds, byte[]... fields) {
    return executeCommand(getCommandObjects().hpexpire(key, milliseconds, fields));
  }

  @Override
  public List<Long> hpexpire(byte[] key, long milliseconds, ExpiryOption condition,
      byte[]... fields) {
    return executeCommand(getCommandObjects().hpexpire(key, milliseconds, condition, fields));
  }

  @Override
  public List<Long> hexpireAt(byte[] key, long unixTimeSeconds, byte[]... fields) {
    return executeCommand(getCommandObjects().hexpireAt(key, unixTimeSeconds, fields));
  }

  @Override
  public List<Long> hexpireAt(byte[] key, long unixTimeSeconds, ExpiryOption condition,
      byte[]... fields) {
    return executeCommand(getCommandObjects().hexpireAt(key, unixTimeSeconds, condition, fields));
  }

  @Override
  public List<Long> hpexpireAt(byte[] key, long unixTimeMillis, byte[]... fields) {
    return executeCommand(getCommandObjects().hpexpireAt(key, unixTimeMillis, fields));
  }

  @Override
  public List<Long> hpexpireAt(byte[] key, long unixTimeMillis, ExpiryOption condition,
      byte[]... fields) {
    return executeCommand(getCommandObjects().hpexpireAt(key, unixTimeMillis, condition, fields));
  }

  @Override
  public List<Long> hexpireTime(String key, String... fields) {
    return executeCommand(getCommandObjects().hexpireTime(key, fields));
  }

  @Override
  public List<Long> hpexpireTime(String key, String... fields) {
    return executeCommand(getCommandObjects().hpexpireTime(key, fields));
  }

  @Override
  public List<Long> httl(String key, String... fields) {
    return executeCommand(getCommandObjects().httl(key, fields));
  }

  @Override
  public List<Long> hpttl(String key, String... fields) {
    return executeCommand(getCommandObjects().hpttl(key, fields));
  }

  @Override
  public List<Long> hexpireTime(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hexpireTime(key, fields));
  }

  @Override
  public List<Long> hpexpireTime(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hpexpireTime(key, fields));
  }

  @Override
  public List<Long> httl(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().httl(key, fields));
  }

  @Override
  public List<Long> hpttl(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hpttl(key, fields));
  }

  @Override
  public List<Long> hpersist(String key, String... fields) {
    return executeCommand(getCommandObjects().hpersist(key, fields));
  }

  @Override
  public List<Long> hpersist(byte[] key, byte[]... fields) {
    return executeCommand(getCommandObjects().hpersist(key, fields));
  }

  // Set commands
  @Override
  public long sadd(String key, String... members) {
    return executeCommand(getCommandObjects().sadd(key, members));
  }

  @Override
  public Set<String> smembers(String key) {
    return executeCommand(getCommandObjects().smembers(key));
  }

  @Override
  public long srem(String key, String... members) {
    return executeCommand(getCommandObjects().srem(key, members));
  }

  @Override
  public String spop(String key) {
    return executeCommand(getCommandObjects().spop(key));
  }

  @Override
  public Set<String> spop(String key, long count) {
    return executeCommand(getCommandObjects().spop(key, count));
  }

  @Override
  public long scard(String key) {
    return executeCommand(getCommandObjects().scard(key));
  }

  @Override
  public boolean sismember(String key, String member) {
    return executeCommand(getCommandObjects().sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(String key, String... members) {
    return executeCommand(getCommandObjects().smismember(key, members));
  }

  @Override
  public long sadd(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().sadd(key, members));
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    return executeCommand(getCommandObjects().smembers(key));
  }

  @Override
  public long srem(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().srem(key, members));
  }

  @Override
  public byte[] spop(byte[] key) {
    return executeCommand(getCommandObjects().spop(key));
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    return executeCommand(getCommandObjects().spop(key, count));
  }

  @Override
  public long scard(byte[] key) {
    return executeCommand(getCommandObjects().scard(key));
  }

  @Override
  public boolean sismember(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().smismember(key, members));
  }

  @Override
  public String srandmember(String key) {
    return executeCommand(getCommandObjects().srandmember(key));
  }

  @Override
  public List<String> srandmember(String key, int count) {
    return executeCommand(getCommandObjects().srandmember(key, count));
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
    return executeCommand(getCommandObjects().sscan(key, cursor, params));
  }

  @Override
  public byte[] srandmember(byte[] key) {
    return executeCommand(getCommandObjects().srandmember(key));
  }

  @Override
  public List<byte[]> srandmember(byte[] key, int count) {
    return executeCommand(getCommandObjects().srandmember(key, count));
  }

  @Override
  public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(getCommandObjects().sscan(key, cursor, params));
  }

  @Override
  public Set<String> sdiff(String... keys) {
    return executeCommand(getCommandObjects().sdiff(keys));
  }

  @Override
  public long sdiffstore(String dstkey, String... keys) {
    return executeCommand(getCommandObjects().sdiffstore(dstkey, keys));
  }

  @Override
  public Set<String> sinter(String... keys) {
    return executeCommand(getCommandObjects().sinter(keys));
  }

  @Override
  public long sinterstore(String dstkey, String... keys) {
    return executeCommand(getCommandObjects().sinterstore(dstkey, keys));
  }

  @Override
  public long sintercard(String... keys) {
    return executeCommand(getCommandObjects().sintercard(keys));
  }

  @Override
  public long sintercard(int limit, String... keys) {
    return executeCommand(getCommandObjects().sintercard(limit, keys));
  }

  @Override
  public Set<String> sunion(String... keys) {
    return executeCommand(getCommandObjects().sunion(keys));
  }

  @Override
  public long sunionstore(String dstkey, String... keys) {
    return executeCommand(getCommandObjects().sunionstore(dstkey, keys));
  }

  @Override
  public long smove(String srckey, String dstkey, String member) {
    return executeCommand(getCommandObjects().smove(srckey, dstkey, member));
  }

  @Override
  public Set<byte[]> sdiff(byte[]... keys) {
    return executeCommand(getCommandObjects().sdiff(keys));
  }

  @Override
  public long sdiffstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(getCommandObjects().sdiffstore(dstkey, keys));
  }

  @Override
  public Set<byte[]> sinter(byte[]... keys) {
    return executeCommand(getCommandObjects().sinter(keys));
  }

  @Override
  public long sinterstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(getCommandObjects().sinterstore(dstkey, keys));
  }

  @Override
  public long sintercard(byte[]... keys) {
    return executeCommand(getCommandObjects().sintercard(keys));
  }

  @Override
  public long sintercard(int limit, byte[]... keys) {
    return executeCommand(getCommandObjects().sintercard(limit, keys));
  }

  @Override
  public Set<byte[]> sunion(byte[]... keys) {
    return executeCommand(getCommandObjects().sunion(keys));
  }

  @Override
  public long sunionstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(getCommandObjects().sunionstore(dstkey, keys));
  }

  @Override
  public long smove(byte[] srckey, byte[] dstkey, byte[] member) {
    return executeCommand(getCommandObjects().smove(srckey, dstkey, member));
  }

  // Sorted Set commands
  @Override
  public long zadd(String key, double score, String member) {
    return executeCommand(getCommandObjects().zadd(key, score, member));
  }

  @Override
  public long zadd(String key, double score, String member, ZAddParams params) {
    return executeCommand(getCommandObjects().zadd(key, score, member, params));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers) {
    return executeCommand(getCommandObjects().zadd(key, scoreMembers));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return executeCommand(getCommandObjects().zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(String key, double score, String member, ZAddParams params) {
    return executeCommand(getCommandObjects().zaddIncr(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member) {
    return executeCommand(getCommandObjects().zadd(key, score, member));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(getCommandObjects().zadd(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    return executeCommand(getCommandObjects().zadd(key, scoreMembers));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    return executeCommand(getCommandObjects().zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(getCommandObjects().zaddIncr(key, score, member, params));
  }

  @Override
  public long zrem(String key, String... members) {
    return executeCommand(getCommandObjects().zrem(key, members));
  }

  @Override
  public double zincrby(String key, double increment, String member) {
    return executeCommand(getCommandObjects().zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
    return executeCommand(getCommandObjects().zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(String key, String member) {
    return executeCommand(getCommandObjects().zrank(key, member));
  }

  @Override
  public Long zrevrank(String key, String member) {
    return executeCommand(getCommandObjects().zrevrank(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrankWithScore(String key, String member) {
    return executeCommand(getCommandObjects().zrankWithScore(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrevrankWithScore(String key, String member) {
    return executeCommand(getCommandObjects().zrevrankWithScore(key, member));
  }

  @Override
  public long zrem(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().zrem(key, members));
  }

  @Override
  public double zincrby(byte[] key, double increment, byte[] member) {
    return executeCommand(getCommandObjects().zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
    return executeCommand(getCommandObjects().zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().zrank(key, member));
  }

  @Override
  public Long zrevrank(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().zrevrank(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrankWithScore(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().zrankWithScore(key, member));
  }

  @Override
  public KeyValue<Long, Double> zrevrankWithScore(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().zrevrankWithScore(key, member));
  }

  @Override
  public String zrandmember(String key) {
    return executeCommand(getCommandObjects().zrandmember(key));
  }

  @Override
  public List<String> zrandmember(String key, long count) {
    return executeCommand(getCommandObjects().zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(String key, long count) {
    return executeCommand(getCommandObjects().zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(String key) {
    return executeCommand(getCommandObjects().zcard(key));
  }

  @Override
  public Double zscore(String key, String member) {
    return executeCommand(getCommandObjects().zscore(key, member));
  }

  @Override
  public List<Double> zmscore(String key, String... members) {
    return executeCommand(getCommandObjects().zmscore(key, members));
  }

  @Override
  public byte[] zrandmember(byte[] key) {
    return executeCommand(getCommandObjects().zrandmember(key));
  }

  @Override
  public List<byte[]> zrandmember(byte[] key, long count) {
    return executeCommand(getCommandObjects().zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(byte[] key, long count) {
    return executeCommand(getCommandObjects().zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(byte[] key) {
    return executeCommand(getCommandObjects().zcard(key));
  }

  @Override
  public Double zscore(byte[] key, byte[] member) {
    return executeCommand(getCommandObjects().zscore(key, member));
  }

  @Override
  public List<Double> zmscore(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(String key) {
    return executeCommand(getCommandObjects().zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(String key, int count) {
    return executeCommand(getCommandObjects().zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(String key) {
    return executeCommand(getCommandObjects().zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(String key, int count) {
    return executeCommand(getCommandObjects().zpopmin(key, count));
  }

  @Override
  public long zcount(String key, double min, double max) {
    return executeCommand(getCommandObjects().zcount(key, min, max));
  }

  @Override
  public long zcount(String key, String min, String max) {
    return executeCommand(getCommandObjects().zcount(key, min, max));
  }

  @Override
  public Tuple zpopmax(byte[] key) {
    return executeCommand(getCommandObjects().zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(byte[] key, int count) {
    return executeCommand(getCommandObjects().zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(byte[] key) {
    return executeCommand(getCommandObjects().zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(byte[] key, int count) {
    return executeCommand(getCommandObjects().zpopmin(key, count));
  }

  @Override
  public long zcount(byte[] key, double min, double max) {
    return executeCommand(getCommandObjects().zcount(key, min, max));
  }

  @Override
  public long zcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zcount(key, min, max));
  }

  @Override
  public List<String> zrange(String key, long start, long stop) {
    return executeCommand(getCommandObjects().zrange(key, start, stop));
  }

  @Override
  public List<String> zrevrange(String key, long start, long stop) {
    return executeCommand(getCommandObjects().zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(String key, long start, long stop) {
    return executeCommand(getCommandObjects().zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(String key, long start, long stop) {
    return executeCommand(getCommandObjects().zrevrangeWithScores(key, start, stop));
  }

  @Override
  public List<String> zrange(String key, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrange(key, zRangeParams));
  }

  @Override
  public List<Tuple> zrangeWithScores(String key, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrangeWithScores(key, zRangeParams));
  }

  @Override
  public long zrangestore(String dest, String src, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrangestore(dest, src, zRangeParams));
  }

  @Override
  public List<String> zrangeByScore(String key, double min, double max) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max));
  }

  @Override
  public List<String> zrangeByScore(String key, String min, String max) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max));
  }

  @Override
  public List<String> zrevrangeByScore(String key, double max, double min) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, String max, String min) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    return executeCommand(getCommandObjects().zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    return executeCommand(getCommandObjects().zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    return executeCommand(getCommandObjects().zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    return executeCommand(getCommandObjects().zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<byte[]> zrange(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().zrange(key, start, stop));
  }

  @Override
  public List<byte[]> zrevrange(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().zrevrangeWithScores(key, start, stop));
  }

  @Override
  public List<byte[]> zrange(byte[] key, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrange(key, zRangeParams));
  }

  @Override
  public List<Tuple> zrangeWithScores(byte[] key, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrangeWithScores(key, zRangeParams));
  }

  @Override
  public long zrangestore(byte[] dest, byte[] src, ZRangeParams zRangeParams) {
    return executeCommand(getCommandObjects().zrangestore(dest, src, zRangeParams));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, double min, double max) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return executeCommand(getCommandObjects().zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    return executeCommand(getCommandObjects().zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    return executeCommand(getCommandObjects().zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset,
      int count) {
    return executeCommand(
      getCommandObjects().zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByRank(String key, long start, long stop) {
    return executeCommand(getCommandObjects().zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(String key, double min, double max) {
    return executeCommand(getCommandObjects().zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(String key, String min, String max) {
    return executeCommand(getCommandObjects().zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByRank(byte[] key, long start, long stop) {
    return executeCommand(getCommandObjects().zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(byte[] key, double min, double max) {
    return executeCommand(getCommandObjects().zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zremrangeByScore(key, min, max));
  }

  @Override
  public long zlexcount(String key, String min, String max) {
    return executeCommand(getCommandObjects().zlexcount(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(String key, String min, String max) {
    return executeCommand(getCommandObjects().zrangeByLex(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(String key, String min, String max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByLex(String key, String max, String min) {
    return executeCommand(getCommandObjects().zrevrangeByLex(key, max, min));
  }

  @Override
  public List<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(String key, String min, String max) {
    return executeCommand(getCommandObjects().zremrangeByLex(key, min, max));
  }

  @Override
  public long zlexcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zlexcount(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zrangeByLex(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(getCommandObjects().zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    return executeCommand(getCommandObjects().zrevrangeByLex(key, max, min));
  }

  @Override
  public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(getCommandObjects().zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(getCommandObjects().zremrangeByLex(key, min, max));
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
    return executeCommand(getCommandObjects().zscan(key, cursor, params));
  }

  @Override
  public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(getCommandObjects().zscan(key, cursor, params));
  }

  @Override
  public KeyValue<String, Tuple> bzpopmax(double timeout, String... keys) {
    return executeCommand(getCommandObjects().bzpopmax(timeout, keys));
  }

  @Override
  public KeyValue<String, Tuple> bzpopmin(double timeout, String... keys) {
    return executeCommand(getCommandObjects().bzpopmin(timeout, keys));
  }

  @Override
  public KeyValue<byte[], Tuple> bzpopmax(double timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().bzpopmax(timeout, keys));
  }

  @Override
  public KeyValue<byte[], Tuple> bzpopmin(double timeout, byte[]... keys) {
    return executeCommand(getCommandObjects().bzpopmin(timeout, keys));
  }

  @Override
  public List<String> zdiff(String... keys) {
    return executeCommand(getCommandObjects().zdiff(keys));
  }

  @Override
  public List<Tuple> zdiffWithScores(String... keys) {
    return executeCommand(getCommandObjects().zdiffWithScores(keys));
  }

  @Override
  public long zdiffstore(String dstkey, String... keys) {
    return executeCommand(getCommandObjects().zdiffstore(dstkey, keys));
  }

  @Override
  public List<byte[]> zdiff(byte[]... keys) {
    return executeCommand(getCommandObjects().zdiff(keys));
  }

  @Override
  public List<Tuple> zdiffWithScores(byte[]... keys) {
    return executeCommand(getCommandObjects().zdiffWithScores(keys));
  }

  @Override
  public long zdiffstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(getCommandObjects().zdiffstore(dstkey, keys));
  }

  @Override
  public long zinterstore(String dstkey, String... sets) {
    return executeCommand(getCommandObjects().zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(getCommandObjects().zinterstore(dstkey, params, sets));
  }

  @Override
  public List<String> zinter(ZParams params, String... keys) {
    return executeCommand(getCommandObjects().zinter(params, keys));
  }

  @Override
  public List<Tuple> zinterWithScores(ZParams params, String... keys) {
    return executeCommand(getCommandObjects().zinterWithScores(params, keys));
  }

  @Override
  public long zinterstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(getCommandObjects().zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(getCommandObjects().zinterstore(dstkey, params, sets));
  }

  @Override
  public long zintercard(byte[]... keys) {
    return executeCommand(getCommandObjects().zintercard(keys));
  }

  @Override
  public long zintercard(long limit, byte[]... keys) {
    return executeCommand(getCommandObjects().zintercard(limit, keys));
  }

  @Override
  public long zintercard(String... keys) {
    return executeCommand(getCommandObjects().zintercard(keys));
  }

  @Override
  public long zintercard(long limit, String... keys) {
    return executeCommand(getCommandObjects().zintercard(limit, keys));
  }

  @Override
  public List<byte[]> zinter(ZParams params, byte[]... keys) {
    return executeCommand(getCommandObjects().zinter(params, keys));
  }

  @Override
  public List<Tuple> zinterWithScores(ZParams params, byte[]... keys) {
    return executeCommand(getCommandObjects().zinterWithScores(params, keys));
  }

  @Override
  public List<String> zunion(ZParams params, String... keys) {
    return executeCommand(getCommandObjects().zunion(params, keys));
  }

  @Override
  public List<Tuple> zunionWithScores(ZParams params, String... keys) {
    return executeCommand(getCommandObjects().zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(String dstkey, String... sets) {
    return executeCommand(getCommandObjects().zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(getCommandObjects().zunionstore(dstkey, params, sets));
  }

  @Override
  public List<byte[]> zunion(ZParams params, byte[]... keys) {
    return executeCommand(getCommandObjects().zunion(params, keys));
  }

  @Override
  public List<Tuple> zunionWithScores(ZParams params, byte[]... keys) {
    return executeCommand(getCommandObjects().zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(getCommandObjects().zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(getCommandObjects().zunionstore(dstkey, params, sets));
  }

  @Override
  public KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, String... keys) {
    return executeCommand(getCommandObjects().zmpop(option, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> zmpop(SortedSetOption option, int count, String... keys) {
    return executeCommand(getCommandObjects().zmpop(option, count, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> bzmpop(double timeout, SortedSetOption option,
      String... keys) {
    return executeCommand(getCommandObjects().bzmpop(timeout, option, keys));
  }

  @Override
  public KeyValue<String, List<Tuple>> bzmpop(double timeout, SortedSetOption option, int count,
      String... keys) {
    return executeCommand(getCommandObjects().bzmpop(timeout, option, count, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, byte[]... keys) {
    return executeCommand(getCommandObjects().zmpop(option, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, int count, byte[]... keys) {
    return executeCommand(getCommandObjects().zmpop(option, count, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option,
      byte[]... keys) {
    return executeCommand(getCommandObjects().bzmpop(timeout, option, keys));
  }

  @Override
  public KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option, int count,
      byte[]... keys) {
    return executeCommand(getCommandObjects().bzmpop(timeout, option, count, keys));
  }

  // Geo commands
  @Override
  public long geoadd(String key, double longitude, double latitude, String member) {
    return executeCommand(getCommandObjects().geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(getCommandObjects().geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(String key, GeoAddParams params,
      Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(getCommandObjects().geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(String key, String member1, String member2) {
    return executeCommand(getCommandObjects().geodist(key, member1, member2));
  }

  @Override
  public Double geodist(String key, String member1, String member2, GeoUnit unit) {
    return executeCommand(getCommandObjects().geodist(key, member1, member2, unit));
  }

  @Override
  public List<String> geohash(String key, String... members) {
    return executeCommand(getCommandObjects().geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(String key, String... members) {
    return executeCommand(getCommandObjects().geopos(key, members));
  }

  @Override
  public long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    return executeCommand(getCommandObjects().geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(getCommandObjects().geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(byte[] key, GeoAddParams params,
      Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(getCommandObjects().geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2) {
    return executeCommand(getCommandObjects().geodist(key, member1, member2));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    return executeCommand(getCommandObjects().geodist(key, member1, member2, unit));
  }

  @Override
  public List<byte[]> geohash(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
    return executeCommand(getCommandObjects().geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    return executeCommand(
      getCommandObjects().georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(getCommandObjects().georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(getCommandObjects().georadiusStore(key, longitude, latitude, radius, unit,
      param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(
      getCommandObjects().georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, coord, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, String member, double width, double height,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, member, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoCoordinate coord, double width,
      double height, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, coord, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(String key, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearch(key, params));
  }

  @Override
  public long geosearchStore(String dest, String src, String member, double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, member, radius, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoCoordinate coord, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, coord, radius, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, String member, double width, double height,
      GeoUnit unit) {
    return executeCommand(
      getCommandObjects().geosearchStore(dest, src, member, width, height, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoCoordinate coord, double width,
      double height, GeoUnit unit) {
    return executeCommand(
      getCommandObjects().geosearchStore(dest, src, coord, width, height, unit));
  }

  @Override
  public long geosearchStore(String dest, String src, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, params));
  }

  @Override
  public long geosearchStoreStoreDist(String dest, String src, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearchStoreStoreDist(dest, src, params));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    return executeCommand(
      getCommandObjects().georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(getCommandObjects().georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(
      getCommandObjects().georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(getCommandObjects().georadiusStore(key, longitude, latitude, radius, unit,
      param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(
      getCommandObjects().georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoCoordinate coord, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, coord, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, byte[] member, double width, double height,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, member, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoCoordinate coord, double width,
      double height, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearch(key, coord, width, height, unit));
  }

  @Override
  public List<GeoRadiusResponse> geosearch(byte[] key, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearch(key, params));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, member, radius, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double radius,
      GeoUnit unit) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, coord, radius, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, byte[] member, double width, double height,
      GeoUnit unit) {
    return executeCommand(
      getCommandObjects().geosearchStore(dest, src, member, width, height, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double width,
      double height, GeoUnit unit) {
    return executeCommand(
      getCommandObjects().geosearchStore(dest, src, coord, width, height, unit));
  }

  @Override
  public long geosearchStore(byte[] dest, byte[] src, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearchStore(dest, src, params));
  }

  @Override
  public long geosearchStoreStoreDist(byte[] dest, byte[] src, GeoSearchParam params) {
    return executeCommand(getCommandObjects().geosearchStoreStoreDist(dest, src, params));
  }

  // Hyper Log Log commands
  @Override
  public long pfadd(String key, String... elements) {
    return executeCommand(getCommandObjects().pfadd(key, elements));
  }

  @Override
  public String pfmerge(String destkey, String... sourcekeys) {
    return executeCommand(getCommandObjects().pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(String key) {
    return executeCommand(getCommandObjects().pfcount(key));
  }

  @Override
  public long pfcount(String... keys) {
    return executeCommand(getCommandObjects().pfcount(keys));
  }

  @Override
  public long pfadd(byte[] key, byte[]... elements) {
    return executeCommand(getCommandObjects().pfadd(key, elements));
  }

  @Override
  public String pfmerge(byte[] destkey, byte[]... sourcekeys) {
    return executeCommand(getCommandObjects().pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(byte[] key) {
    return executeCommand(getCommandObjects().pfcount(key));
  }

  @Override
  public long pfcount(byte[]... keys) {
    return executeCommand(getCommandObjects().pfcount(keys));
  }

  // Stream commands
  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return executeCommand(getCommandObjects().xadd(key, id, hash));
  }

  @Override
  public StreamEntryID xadd(String key, XAddParams params, Map<String, String> hash) {
    return executeCommand(getCommandObjects().xadd(key, params, hash));
  }

  @Override
  public long xlen(String key) {
    return executeCommand(getCommandObjects().xlen(key));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return executeCommand(getCommandObjects().xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return executeCommand(getCommandObjects().xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start,
      int count) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start, count));
  }

  @Override
  public List<StreamEntry> xrange(String key, String start, String end) {
    return executeCommand(getCommandObjects().xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, String start, String end, int count) {
    return executeCommand(getCommandObjects().xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, String end, String start) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, String end, String start, int count) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start, count));
  }

  @Override
  public long xack(String key, String group, StreamEntryID... ids) {
    return executeCommand(getCommandObjects().xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(String key, String groupName, StreamEntryID id, boolean makeStream) {
    return executeCommand(getCommandObjects().xgroupCreate(key, groupName, id, makeStream));
  }

  @Override
  public String xgroupSetID(String key, String groupName, StreamEntryID id) {
    return executeCommand(getCommandObjects().xgroupSetID(key, groupName, id));
  }

  @Override
  public long xgroupDestroy(String key, String groupName) {
    return executeCommand(getCommandObjects().xgroupDestroy(key, groupName));
  }

  @Override
  public boolean xgroupCreateConsumer(String key, String groupName, String consumerName) {
    return executeCommand(getCommandObjects().xgroupCreateConsumer(key, groupName, consumerName));
  }

  @Override
  public long xgroupDelConsumer(String key, String groupName, String consumerName) {
    return executeCommand(getCommandObjects().xgroupDelConsumer(key, groupName, consumerName));
  }

  @Override
  public StreamPendingSummary xpending(String key, String groupName) {
    return executeCommand(getCommandObjects().xpending(key, groupName));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupName, XPendingParams params) {
    return executeCommand(getCommandObjects().xpending(key, groupName, params));
  }

  @Override
  public long xdel(String key, StreamEntryID... ids) {
    return executeCommand(getCommandObjects().xdel(key, ids));
  }

  @Override
  public long xtrim(String key, long maxLen, boolean approximate) {
    return executeCommand(getCommandObjects().xtrim(key, maxLen, approximate));
  }

  @Override
  public long xtrim(String key, XTrimParams params) {
    return executeCommand(getCommandObjects().xtrim(key, params));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumerName, long minIdleTime,
      XClaimParams params, StreamEntryID... ids) {
    return executeCommand(
      getCommandObjects().xclaim(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumerName,
      long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(
      getCommandObjects().xclaimJustId(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group,
      String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(
      getCommandObjects().xautoclaim(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group,
      String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(
      getCommandObjects().xautoclaimJustId(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public StreamInfo xinfoStream(String key) {
    return executeCommand(getCommandObjects().xinfoStream(key));
  }

  @Override
  public StreamFullInfo xinfoStreamFull(String key) {
    return executeCommand(getCommandObjects().xinfoStreamFull(key));
  }

  @Override
  public StreamFullInfo xinfoStreamFull(String key, int count) {
    return executeCommand(getCommandObjects().xinfoStreamFull(key, count));
  }

  @Override
  public List<StreamGroupInfo> xinfoGroups(String key) {
    return executeCommand(getCommandObjects().xinfoGroups(key));
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    return executeCommand(getCommandObjects().xinfoConsumers(key, group));
  }

  @Override
  public List<StreamConsumerInfo> xinfoConsumers2(String key, String group) {
    return executeCommand(getCommandObjects().xinfoConsumers2(key, group));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams) {
    return executeCommand(getCommandObjects().xread(xReadParams, streams));
  }

  @Override
  public Map<String, List<StreamEntry>> xreadAsMap(XReadParams xReadParams,
      Map<String, StreamEntryID> streams) {
    return executeCommand(getCommandObjects().xreadAsMap(xReadParams, streams));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupName, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return executeCommand(
      getCommandObjects().xreadGroup(groupName, consumer, xReadGroupParams, streams));
  }

  @Override
  public Map<String, List<StreamEntry>> xreadGroupAsMap(String groupName, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return executeCommand(
      getCommandObjects().xreadGroupAsMap(groupName, consumer, xReadGroupParams, streams));
  }

  @Override
  public byte[] xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash) {
    return executeCommand(getCommandObjects().xadd(key, params, hash));
  }

  @Override
  public long xlen(byte[] key) {
    return executeCommand(getCommandObjects().xlen(key));
  }

  @Override
  public List<Object> xrange(byte[] key, byte[] start, byte[] end) {
    return executeCommand(getCommandObjects().xrange(key, start, end));
  }

  @Override
  public List<Object> xrange(byte[] key, byte[] start, byte[] end, int count) {
    return executeCommand(getCommandObjects().xrange(key, start, end, count));
  }

  @Override
  public List<Object> xrevrange(byte[] key, byte[] end, byte[] start) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start));
  }

  @Override
  public List<Object> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    return executeCommand(getCommandObjects().xrevrange(key, end, start, count));
  }

  @Override
  public long xack(byte[] key, byte[] group, byte[]... ids) {
    return executeCommand(getCommandObjects().xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(byte[] key, byte[] groupName, byte[] id, boolean makeStream) {
    return executeCommand(getCommandObjects().xgroupCreate(key, groupName, id, makeStream));
  }

  @Override
  public String xgroupSetID(byte[] key, byte[] groupName, byte[] id) {
    return executeCommand(getCommandObjects().xgroupSetID(key, groupName, id));
  }

  @Override
  public long xgroupDestroy(byte[] key, byte[] groupName) {
    return executeCommand(getCommandObjects().xgroupDestroy(key, groupName));
  }

  @Override
  public boolean xgroupCreateConsumer(byte[] key, byte[] groupName, byte[] consumerName) {
    return executeCommand(getCommandObjects().xgroupCreateConsumer(key, groupName, consumerName));
  }

  @Override
  public long xgroupDelConsumer(byte[] key, byte[] groupName, byte[] consumerName) {
    return executeCommand(getCommandObjects().xgroupDelConsumer(key, groupName, consumerName));
  }

  @Override
  public long xdel(byte[] key, byte[]... ids) {
    return executeCommand(getCommandObjects().xdel(key, ids));
  }

  @Override
  public long xtrim(byte[] key, long maxLen, boolean approximateLength) {
    return executeCommand(getCommandObjects().xtrim(key, maxLen, approximateLength));
  }

  @Override
  public long xtrim(byte[] key, XTrimParams params) {
    return executeCommand(getCommandObjects().xtrim(key, params));
  }

  @Override
  public Object xpending(byte[] key, byte[] groupName) {
    return executeCommand(getCommandObjects().xpending(key, groupName));
  }

  @Override
  public List<Object> xpending(byte[] key, byte[] groupName, XPendingParams params) {
    return executeCommand(getCommandObjects().xpending(key, groupName, params));
  }

  @Override
  public List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumerName, long minIdleTime,
      XClaimParams params, byte[]... ids) {
    return executeCommand(
      getCommandObjects().xclaim(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumerName, long minIdleTime,
      XClaimParams params, byte[]... ids) {
    return executeCommand(
      getCommandObjects().xclaimJustId(key, group, consumerName, minIdleTime, params, ids));
  }

  @Override
  public List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(
      getCommandObjects().xautoclaim(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(getCommandObjects().xautoclaimJustId(key, groupName, consumerName,
      minIdleTime, start, params));
  }

  @Override
  public Object xinfoStream(byte[] key) {
    return executeCommand(getCommandObjects().xinfoStream(key));
  }

  @Override
  public Object xinfoStreamFull(byte[] key) {
    return executeCommand(getCommandObjects().xinfoStreamFull(key));
  }

  @Override
  public Object xinfoStreamFull(byte[] key, int count) {
    return executeCommand(getCommandObjects().xinfoStreamFull(key, count));
  }

  @Override
  public List<Object> xinfoGroups(byte[] key) {
    return executeCommand(getCommandObjects().xinfoGroups(key));
  }

  @Override
  public List<Object> xinfoConsumers(byte[] key, byte[] group) {
    return executeCommand(getCommandObjects().xinfoConsumers(key, group));
  }

  @Override
  public List<Map.Entry<byte[], List<StreamEntryBinary>>> xreadBinary(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams) {
    return executeCommand(getCommandObjects().xreadBinary(xReadParams, streams));
  }

  @Override
  public Map<byte[], List<StreamEntryBinary>> xreadBinaryAsMap(XReadParams xReadParams,
      Map<byte[], StreamEntryID> streams) {
    return executeCommand(getCommandObjects().xreadBinaryAsMap(xReadParams, streams));
  }

  @Override
  public List<Map.Entry<byte[], List<StreamEntryBinary>>> xreadGroupBinary(byte[] groupName,
      byte[] consumer, XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams) {
    return executeCommand(
      getCommandObjects().xreadGroupBinary(groupName, consumer, xReadGroupParams, streams));
  }

  @Override
  public Map<byte[], List<StreamEntryBinary>> xreadGroupBinaryAsMap(byte[] groupName,
      byte[] consumer, XReadGroupParams xReadGroupParams, Map<byte[], StreamEntryID> streams) {
    return executeCommand(
      getCommandObjects().xreadGroupBinaryAsMap(groupName, consumer, xReadGroupParams, streams));
  }

  // Scripting commands
  @Override
  public Object eval(String script) {
    return executeCommand(getCommandObjects().eval(script));
  }

  @Override
  public Object eval(String script, int keyCount, String... params) {
    return executeCommand(getCommandObjects().eval(script, keyCount, params));
  }

  @Override
  public Object eval(String script, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().eval(script, keys, args));
  }

  @Override
  public Object evalReadonly(String script, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().evalReadonly(script, keys, args));
  }

  @Override
  public Object evalsha(String sha1) {
    return executeCommand(getCommandObjects().evalsha(sha1));
  }

  @Override
  public Object evalsha(String sha1, int keyCount, String... params) {
    return executeCommand(getCommandObjects().evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(String sha1, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().evalsha(sha1, keys, args));
  }

  @Override
  public Object evalshaReadonly(String sha1, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().evalshaReadonly(sha1, keys, args));
  }

  @Override
  public Object eval(byte[] script) {
    return executeCommand(getCommandObjects().eval(script));
  }

  @Override
  public Object eval(byte[] script, int keyCount, byte[]... params) {
    return executeCommand(getCommandObjects().eval(script, keyCount, params));
  }

  @Override
  public Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().eval(script, keys, args));
  }

  @Override
  public Object evalReadonly(byte[] script, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().evalReadonly(script, keys, args));
  }

  @Override
  public Object evalsha(byte[] sha1) {
    return executeCommand(getCommandObjects().evalsha(sha1));
  }

  @Override
  public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
    return executeCommand(getCommandObjects().evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().evalsha(sha1, keys, args));
  }

  @Override
  public Object evalshaReadonly(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().evalshaReadonly(sha1, keys, args));
  }

  @Override
  public Object fcall(String name, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().fcall(name, keys, args));
  }

  @Override
  public Object fcallReadonly(String name, List<String> keys, List<String> args) {
    return executeCommand(getCommandObjects().fcallReadonly(name, keys, args));
  }

  @Override
  public String functionDelete(String libraryName) {
    return checkAndBroadcastCommand(getCommandObjects().functionDelete(libraryName));
  }

  @Override
  public String functionFlush() {
    return checkAndBroadcastCommand(getCommandObjects().functionFlush());
  }

  @Override
  public String functionFlush(FlushMode mode) {
    return checkAndBroadcastCommand(getCommandObjects().functionFlush(mode));
  }

  @Override
  public String functionKill() {
    return checkAndBroadcastCommand(getCommandObjects().functionKill());
  }

  @Override
  public List<LibraryInfo> functionList() {
    return executeCommand(getCommandObjects().functionList());
  }

  @Override
  public List<LibraryInfo> functionList(String libraryNamePattern) {
    return executeCommand(getCommandObjects().functionList(libraryNamePattern));
  }

  @Override
  public List<LibraryInfo> functionListWithCode() {
    return executeCommand(getCommandObjects().functionListWithCode());
  }

  @Override
  public List<LibraryInfo> functionListWithCode(String libraryNamePattern) {
    return executeCommand(getCommandObjects().functionListWithCode(libraryNamePattern));
  }

  @Override
  public String functionLoad(String functionCode) {
    return checkAndBroadcastCommand(getCommandObjects().functionLoad(functionCode));
  }

  @Override
  public String functionLoadReplace(String functionCode) {
    return checkAndBroadcastCommand(getCommandObjects().functionLoadReplace(functionCode));
  }

  @Override
  public FunctionStats functionStats() {
    return executeCommand(getCommandObjects().functionStats());
  }

  @Override
  public Object fcall(byte[] name, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().fcall(name, keys, args));
  }

  @Override
  public Object fcallReadonly(byte[] name, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(getCommandObjects().fcallReadonly(name, keys, args));
  }

  @Override
  public String functionDelete(byte[] libraryName) {
    return checkAndBroadcastCommand(getCommandObjects().functionDelete(libraryName));
  }

  @Override
  public byte[] functionDump() {
    return executeCommand(getCommandObjects().functionDump());
  }

  @Override
  public List<Object> functionListBinary() {
    return executeCommand(getCommandObjects().functionListBinary());
  }

  @Override
  public List<Object> functionList(final byte[] libraryNamePattern) {
    return executeCommand(getCommandObjects().functionList(libraryNamePattern));
  }

  @Override
  public List<Object> functionListWithCodeBinary() {
    return executeCommand(getCommandObjects().functionListWithCodeBinary());
  }

  @Override
  public List<Object> functionListWithCode(final byte[] libraryNamePattern) {
    return executeCommand(getCommandObjects().functionListWithCode(libraryNamePattern));
  }

  @Override
  public String functionLoad(byte[] functionCode) {
    return checkAndBroadcastCommand(getCommandObjects().functionLoad(functionCode));
  }

  @Override
  public String functionLoadReplace(byte[] functionCode) {
    return checkAndBroadcastCommand(getCommandObjects().functionLoadReplace(functionCode));
  }

  @Override
  public String functionRestore(byte[] serializedValue) {
    return checkAndBroadcastCommand(getCommandObjects().functionRestore(serializedValue));
  }

  @Override
  public String functionRestore(byte[] serializedValue, FunctionRestorePolicy policy) {
    return checkAndBroadcastCommand(getCommandObjects().functionRestore(serializedValue, policy));
  }

  @Override
  public Object functionStatsBinary() {
    return executeCommand(getCommandObjects().functionStatsBinary());
  }

  // Other key commands
  @Override
  public Long objectRefcount(String key) {
    return executeCommand(getCommandObjects().objectRefcount(key));
  }

  @Override
  public String objectEncoding(String key) {
    return executeCommand(getCommandObjects().objectEncoding(key));
  }

  @Override
  public Long objectIdletime(String key) {
    return executeCommand(getCommandObjects().objectIdletime(key));
  }

  @Override
  public Long objectFreq(String key) {
    return executeCommand(getCommandObjects().objectFreq(key));
  }

  @Override
  public Long objectRefcount(byte[] key) {
    return executeCommand(getCommandObjects().objectRefcount(key));
  }

  @Override
  public byte[] objectEncoding(byte[] key) {
    return executeCommand(getCommandObjects().objectEncoding(key));
  }

  @Override
  public Long objectIdletime(byte[] key) {
    return executeCommand(getCommandObjects().objectIdletime(key));
  }

  @Override
  public Long objectFreq(byte[] key) {
    return executeCommand(getCommandObjects().objectFreq(key));
  }

  @Override
  public String migrate(String host, int port, String key, int timeout) {
    return executeCommand(getCommandObjects().migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, String... keys) {
    return executeCommand(getCommandObjects().migrate(host, port, timeout, params, keys));
  }

  @Override
  public String migrate(String host, int port, byte[] key, int timeout) {
    return executeCommand(getCommandObjects().migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys) {
    return executeCommand(getCommandObjects().migrate(host, port, timeout, params, keys));
  }

  // Sample key commands
  @Override
  public long waitReplicas(String sampleKey, int replicas, long timeout) {
    return executeCommand(getCommandObjects().waitReplicas(sampleKey, replicas, timeout));
  }

  @Override
  public long waitReplicas(byte[] sampleKey, int replicas, long timeout) {
    return executeCommand(getCommandObjects().waitReplicas(sampleKey, replicas, timeout));
  }

  @Override
  public KeyValue<Long, Long> waitAOF(String sampleKey, long numLocal, long numReplicas,
      long timeout) {
    return executeCommand(getCommandObjects().waitAOF(sampleKey, numLocal, numReplicas, timeout));
  }

  @Override
  public KeyValue<Long, Long> waitAOF(byte[] sampleKey, long numLocal, long numReplicas,
      long timeout) {
    return executeCommand(getCommandObjects().waitAOF(sampleKey, numLocal, numReplicas, timeout));
  }

  @Override
  public Object eval(String script, String sampleKey) {
    return executeCommand(getCommandObjects().eval(script, sampleKey));
  }

  @Override
  public Object evalsha(String sha1, String sampleKey) {
    return executeCommand(getCommandObjects().evalsha(sha1, sampleKey));
  }

  @Override
  public Object eval(byte[] script, byte[] sampleKey) {
    return executeCommand(getCommandObjects().eval(script, sampleKey));
  }

  @Override
  public Object evalsha(byte[] sha1, byte[] sampleKey) {
    return executeCommand(getCommandObjects().evalsha(sha1, sampleKey));
  }

  public List<Boolean> scriptExists(List<String> sha1s) {
    return checkAndBroadcastCommand(getCommandObjects().scriptExists(sha1s));
  }

  @Override
  public Boolean scriptExists(String sha1, String sampleKey) {
    return scriptExists(sampleKey, new String[] { sha1 }).get(0);
  }

  @Override
  public Boolean scriptExists(byte[] sha1, byte[] sampleKey) {
    return scriptExists(sampleKey, new byte[][] { sha1 }).get(0);
  }

  @Override
  public List<Boolean> scriptExists(String sampleKey, String... sha1s) {
    return executeCommand(getCommandObjects().scriptExists(sampleKey, sha1s));
  }

  @Override
  public List<Boolean> scriptExists(byte[] sampleKey, byte[]... sha1s) {
    return executeCommand(getCommandObjects().scriptExists(sampleKey, sha1s));
  }

  public String scriptLoad(String script) {
    return checkAndBroadcastCommand(getCommandObjects().scriptLoad(script));
  }

  @Override
  public String scriptLoad(String script, String sampleKey) {
    return executeCommand(getCommandObjects().scriptLoad(script, sampleKey));
  }

  public String scriptFlush() {
    return checkAndBroadcastCommand(getCommandObjects().scriptFlush());
  }

  @Override
  public String scriptFlush(String sampleKey) {
    return executeCommand(getCommandObjects().scriptFlush(sampleKey));
  }

  @Override
  public String scriptFlush(String sampleKey, FlushMode flushMode) {
    return executeCommand(getCommandObjects().scriptFlush(sampleKey, flushMode));
  }

  public String scriptKill() {
    return checkAndBroadcastCommand(getCommandObjects().scriptKill());
  }

  @Override
  public String scriptKill(String sampleKey) {
    return executeCommand(getCommandObjects().scriptKill(sampleKey));
  }

  @Override
  public byte[] scriptLoad(byte[] script, byte[] sampleKey) {
    return executeCommand(getCommandObjects().scriptLoad(script, sampleKey));
  }

  @Override
  public String scriptFlush(byte[] sampleKey) {
    return executeCommand(getCommandObjects().scriptFlush(sampleKey));
  }

  @Override
  public String scriptFlush(byte[] sampleKey, FlushMode flushMode) {
    return executeCommand(getCommandObjects().scriptFlush(sampleKey, flushMode));
  }

  @Override
  public String scriptKill(byte[] sampleKey) {
    return executeCommand(getCommandObjects().scriptKill(sampleKey));
  }

  public String slowlogReset() {
    return checkAndBroadcastCommand(getCommandObjects().slowlogReset());
  }

  // Random node commands
  public long publish(String channel, String message) {
    return executeCommand(getCommandObjects().publish(channel, message));
  }

  public long publish(byte[] channel, byte[] message) {
    return executeCommand(getCommandObjects().publish(channel, message));
  }

  // RediSearch commands
  public long hsetObject(String key, String field, Object value) {
    return executeCommand(getCommandObjects().hsetObject(key, field, value));
  }

  public long hsetObject(String key, Map<String, Object> hash) {
    return executeCommand(getCommandObjects().hsetObject(key, hash));
  }

  @Override
  public String ftCreate(String indexName, IndexOptions indexOptions, Schema schema) {
    return checkAndBroadcastCommand(getCommandObjects().ftCreate(indexName, indexOptions, schema));
  }

  @Override
  public String ftCreate(String indexName, FTCreateParams createParams,
      Iterable<SchemaField> schemaFields) {
    return checkAndBroadcastCommand(
      getCommandObjects().ftCreate(indexName, createParams, schemaFields));
  }

  @Override
  public String ftAlter(String indexName, Schema schema) {
    return checkAndBroadcastCommand(getCommandObjects().ftAlter(indexName, schema));
  }

  @Override
  public String ftAlter(String indexName, Iterable<SchemaField> schemaFields) {
    return checkAndBroadcastCommand(getCommandObjects().ftAlter(indexName, schemaFields));
  }

  @Override
  public String ftAliasAdd(String aliasName, String indexName) {
    return checkAndBroadcastCommand(getCommandObjects().ftAliasAdd(aliasName, indexName));
  }

  @Override
  public String ftAliasUpdate(String aliasName, String indexName) {
    return checkAndBroadcastCommand(getCommandObjects().ftAliasUpdate(aliasName, indexName));
  }

  @Override
  public String ftAliasDel(String aliasName) {
    return checkAndBroadcastCommand(getCommandObjects().ftAliasDel(aliasName));
  }

  @Override
  public String ftDropIndex(String indexName) {
    return checkAndBroadcastCommand(getCommandObjects().ftDropIndex(indexName));
  }

  @Override
  public String ftDropIndexDD(String indexName) {
    return checkAndBroadcastCommand(getCommandObjects().ftDropIndexDD(indexName));
  }

  @Override
  public SearchResult ftSearch(String indexName, String query) {
    return executeCommand(getCommandObjects().ftSearch(indexName, query));
  }

  @Override
  public SearchResult ftSearch(String indexName, String query, FTSearchParams params) {
    return executeCommand(getCommandObjects().ftSearch(indexName, query, params));
  }

  @Override
  public SearchResult ftSearch(String indexName, Query query) {
    return executeCommand(getCommandObjects().ftSearch(indexName, query));
  }

  /**
   * {@link FTSearchParams#limit(int, int)} will be ignored.
   * @param batchSize batch size
   * @param indexName index name
   * @param query query
   * @param params limit will be ignored
   * @return search iteration
   */
  public FtSearchIteration ftSearchIteration(int batchSize, String indexName, String query,
      FTSearchParams params) {
    return new FtSearchIteration(getConnectionProvider(), getCommandObjects().getProtocol(),
        batchSize, indexName, query, params);
  }

  /**
   * {@link Query#limit(java.lang.Integer, java.lang.Integer)} will be ignored.
   * @param batchSize batch size
   * @param indexName index name
   * @param query limit will be ignored
   * @return search iteration
   */
  public FtSearchIteration ftSearchIteration(int batchSize, String indexName, Query query) {
    return new FtSearchIteration(getConnectionProvider(), getCommandObjects().getProtocol(),
        batchSize, indexName, query);
  }

  @Override
  public String ftExplain(String indexName, Query query) {
    return executeCommand(getCommandObjects().ftExplain(indexName, query));
  }

  @Override
  public List<String> ftExplainCLI(String indexName, Query query) {
    return executeCommand(getCommandObjects().ftExplainCLI(indexName, query));
  }

  @Override
  public AggregationResult ftAggregate(String indexName, AggregationBuilder aggr) {
    return executeCommand(getCommandObjects().ftAggregate(indexName, aggr));
  }

  /**
   * {@link AggregationBuilder#cursor(int, long) CURSOR} must be set.
   * @param indexName index name
   * @param aggr cursor must be set
   * @return aggregate iteration
   */
  public FtAggregateIteration ftAggregateIteration(String indexName, AggregationBuilder aggr) {
    return new FtAggregateIteration(getConnectionProvider(), indexName, aggr);
  }

  @Override
  public AggregationResult ftCursorRead(String indexName, long cursorId, int count) {
    return executeCommand(getCommandObjects().ftCursorRead(indexName, cursorId, count));
  }

  @Override
  public String ftCursorDel(String indexName, long cursorId) {
    return executeCommand(getCommandObjects().ftCursorDel(indexName, cursorId));
  }

  @Override
  public Map.Entry<AggregationResult, ProfilingInfo> ftProfileAggregate(String indexName,
      FTProfileParams profileParams, AggregationBuilder aggr) {
    return executeCommand(getCommandObjects().ftProfileAggregate(indexName, profileParams, aggr));
  }

  @Override
  public Map.Entry<SearchResult, ProfilingInfo> ftProfileSearch(String indexName,
      FTProfileParams profileParams, Query query) {
    return executeCommand(getCommandObjects().ftProfileSearch(indexName, profileParams, query));
  }

  @Override
  public Map.Entry<SearchResult, ProfilingInfo> ftProfileSearch(String indexName,
      FTProfileParams profileParams, String query, FTSearchParams searchParams) {
    return executeCommand(
      getCommandObjects().ftProfileSearch(indexName, profileParams, query, searchParams));
  }

  @Override
  public String ftSynUpdate(String indexName, String synonymGroupId, String... terms) {
    return executeCommand(getCommandObjects().ftSynUpdate(indexName, synonymGroupId, terms));
  }

  @Override
  public Map<String, List<String>> ftSynDump(String indexName) {
    return executeCommand(getCommandObjects().ftSynDump(indexName));
  }

  @Override
  public long ftDictAdd(String dictionary, String... terms) {
    return executeCommand(getCommandObjects().ftDictAdd(dictionary, terms));
  }

  @Override
  public long ftDictDel(String dictionary, String... terms) {
    return executeCommand(getCommandObjects().ftDictDel(dictionary, terms));
  }

  @Override
  public Set<String> ftDictDump(String dictionary) {
    return executeCommand(getCommandObjects().ftDictDump(dictionary));
  }

  @Override
  public long ftDictAddBySampleKey(String indexName, String dictionary, String... terms) {
    return executeCommand(getCommandObjects().ftDictAddBySampleKey(indexName, dictionary, terms));
  }

  @Override
  public long ftDictDelBySampleKey(String indexName, String dictionary, String... terms) {
    return executeCommand(getCommandObjects().ftDictDelBySampleKey(indexName, dictionary, terms));
  }

  @Override
  public Set<String> ftDictDumpBySampleKey(String indexName, String dictionary) {
    return executeCommand(getCommandObjects().ftDictDumpBySampleKey(indexName, dictionary));
  }

  @Override
  public Map<String, Map<String, Double>> ftSpellCheck(String index, String query) {
    return executeCommand(getCommandObjects().ftSpellCheck(index, query));
  }

  @Override
  public Map<String, Map<String, Double>> ftSpellCheck(String index, String query,
      FTSpellCheckParams spellCheckParams) {
    return executeCommand(getCommandObjects().ftSpellCheck(index, query, spellCheckParams));
  }

  @Override
  public Map<String, Object> ftInfo(String indexName) {
    return executeCommand(getCommandObjects().ftInfo(indexName));
  }

  @Override
  public Set<String> ftTagVals(String indexName, String fieldName) {
    return executeCommand(getCommandObjects().ftTagVals(indexName, fieldName));
  }

  @Override
  public long ftSugAdd(String key, String string, double score) {
    return executeCommand(getCommandObjects().ftSugAdd(key, string, score));
  }

  @Override
  public long ftSugAddIncr(String key, String string, double score) {
    return executeCommand(getCommandObjects().ftSugAddIncr(key, string, score));
  }

  @Override
  public List<String> ftSugGet(String key, String prefix) {
    return executeCommand(getCommandObjects().ftSugGet(key, prefix));
  }

  @Override
  public List<String> ftSugGet(String key, String prefix, boolean fuzzy, int max) {
    return executeCommand(getCommandObjects().ftSugGet(key, prefix, fuzzy, max));
  }

  @Override
  public List<Tuple> ftSugGetWithScores(String key, String prefix) {
    return executeCommand(getCommandObjects().ftSugGetWithScores(key, prefix));
  }

  @Override
  public List<Tuple> ftSugGetWithScores(String key, String prefix, boolean fuzzy, int max) {
    return executeCommand(getCommandObjects().ftSugGetWithScores(key, prefix, fuzzy, max));
  }

  @Override
  public boolean ftSugDel(String key, String string) {
    return executeCommand(getCommandObjects().ftSugDel(key, string));
  }

  @Override
  public long ftSugLen(String key) {
    return executeCommand(getCommandObjects().ftSugLen(key));
  }

  @Override
  public Set<String> ftList() {
    return executeCommand(getCommandObjects().ftList());
  }

  // RedisJSON commands
  @Override
  public String jsonSet(String key, Path2 path, Object object) {
    return executeCommand(getCommandObjects().jsonSet(key, path, object));
  }

  @Override
  public String jsonSetWithEscape(String key, Path2 path, Object object) {
    return executeCommand(getCommandObjects().jsonSetWithEscape(key, path, object));
  }

  @Override
  public String jsonSet(String key, Path2 path, Object pojo, JsonSetParams params) {
    return executeCommand(getCommandObjects().jsonSet(key, path, pojo, params));
  }

  @Override
  public String jsonSetWithEscape(String key, Path2 path, Object pojo, JsonSetParams params) {
    return executeCommand(getCommandObjects().jsonSetWithEscape(key, path, pojo, params));
  }

  @Override
  public String jsonMerge(String key, Path2 path, Object object) {
    return executeCommand(getCommandObjects().jsonMerge(key, path, object));
  }

  @Override
  public Object jsonGet(String key) {
    return executeCommand(getCommandObjects().jsonGet(key));
  }

  @Override
  public Object jsonGet(String key, Path2... paths) {
    return executeCommand(getCommandObjects().jsonGet(key, paths));
  }

  @Override
  public List<JSONArray> jsonMGet(Path2 path, String... keys) {
    return executeCommand(getCommandObjects().jsonMGet(path, keys));
  }

  @Override
  public long jsonDel(String key) {
    return executeCommand(getCommandObjects().jsonDel(key));
  }

  @Override
  public long jsonDel(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonDel(key, path));
  }

  @Override
  public long jsonClear(String key) {
    return executeCommand(getCommandObjects().jsonClear(key));
  }

  @Override
  public long jsonClear(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonClear(key, path));
  }

  @Override
  public List<Boolean> jsonToggle(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonToggle(key, path));
  }

  @Override
  public List<Class<?>> jsonType(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonType(key, path));
  }

  @Override
  public List<Long> jsonStrAppend(String key, Path2 path, Object string) {
    return executeCommand(getCommandObjects().jsonStrAppend(key, path, string));
  }

  @Override
  public List<Long> jsonStrLen(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonStrLen(key, path));
  }

  @Override
  public Object jsonNumIncrBy(String key, Path2 path, double value) {
    return executeCommand(getCommandObjects().jsonNumIncrBy(key, path, value));
  }

  @Override
  public List<Long> jsonArrAppend(String key, Path2 path, Object... objects) {
    return executeCommand(getCommandObjects().jsonArrAppend(key, path, objects));
  }

  @Override
  public List<Long> jsonArrAppendWithEscape(String key, Path2 path, Object... objects) {
    return executeCommand(getCommandObjects().jsonArrAppendWithEscape(key, path, objects));
  }

  @Override
  public List<Long> jsonArrIndex(String key, Path2 path, Object scalar) {
    return executeCommand(getCommandObjects().jsonArrIndex(key, path, scalar));
  }

  @Override
  public List<Long> jsonArrIndexWithEscape(String key, Path2 path, Object scalar) {
    return executeCommand(getCommandObjects().jsonArrIndexWithEscape(key, path, scalar));
  }

  @Override
  public List<Long> jsonArrInsert(String key, Path2 path, int index, Object... objects) {
    return executeCommand(getCommandObjects().jsonArrInsert(key, path, index, objects));
  }

  @Override
  public List<Long> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects) {
    return executeCommand(getCommandObjects().jsonArrInsertWithEscape(key, path, index, objects));
  }

  @Override
  public List<Object> jsonArrPop(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonArrPop(key, path));
  }

  @Override
  public List<Object> jsonArrPop(String key, Path2 path, int index) {
    return executeCommand(getCommandObjects().jsonArrPop(key, path, index));
  }

  @Override
  public List<Long> jsonArrLen(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonArrLen(key, path));
  }

  @Override
  public List<Long> jsonArrTrim(String key, Path2 path, int start, int stop) {
    return executeCommand(getCommandObjects().jsonArrTrim(key, path, start, stop));
  }

  @Override
  public List<Long> jsonObjLen(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonObjLen(key, path));
  }

  @Override
  public List<List<String>> jsonObjKeys(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonObjKeys(key, path));
  }

  @Override
  public List<Long> jsonDebugMemory(String key, Path2 path) {
    return executeCommand(getCommandObjects().jsonDebugMemory(key, path));
  }

  // RedisTimeSeries commands
  @Override
  public String tsCreate(String key) {
    return executeCommand(getCommandObjects().tsCreate(key));
  }

  @Override
  public String tsCreate(String key, TSCreateParams createParams) {
    return executeCommand(getCommandObjects().tsCreate(key, createParams));
  }

  @Override
  public long tsDel(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(getCommandObjects().tsDel(key, fromTimestamp, toTimestamp));
  }

  @Override
  public String tsAlter(String key, TSAlterParams alterParams) {
    return executeCommand(getCommandObjects().tsAlter(key, alterParams));
  }

  @Override
  public long tsAdd(String key, double value) {
    return executeCommand(getCommandObjects().tsAdd(key, value));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value) {
    return executeCommand(getCommandObjects().tsAdd(key, timestamp, value));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value, TSCreateParams createParams) {
    return executeCommand(getCommandObjects().tsAdd(key, timestamp, value, createParams));
  }

  @Override
  public long tsAdd(String key, long timestamp, double value, TSAddParams addParams) {
    return executeCommand(getCommandObjects().tsAdd(key, timestamp, value, addParams));
  }

  @Override
  public List<Long> tsMAdd(Map.Entry<String, TSElement>... entries) {
    return executeCommand(getCommandObjects().tsMAdd(entries));
  }

  @Override
  public long tsIncrBy(String key, double value) {
    return executeCommand(getCommandObjects().tsIncrBy(key, value));
  }

  @Override
  public long tsIncrBy(String key, double value, long timestamp) {
    return executeCommand(getCommandObjects().tsIncrBy(key, value, timestamp));
  }

  @Override
  public long tsIncrBy(String key, double addend, TSIncrByParams incrByParams) {
    return executeCommand(getCommandObjects().tsIncrBy(key, addend, incrByParams));
  }

  @Override
  public long tsDecrBy(String key, double value) {
    return executeCommand(getCommandObjects().tsDecrBy(key, value));
  }

  @Override
  public long tsDecrBy(String key, double value, long timestamp) {
    return executeCommand(getCommandObjects().tsDecrBy(key, value, timestamp));
  }

  @Override
  public long tsDecrBy(String key, double subtrahend, TSDecrByParams decrByParams) {
    return executeCommand(getCommandObjects().tsDecrBy(key, subtrahend, decrByParams));
  }

  @Override
  public List<TSElement> tsRange(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(getCommandObjects().tsRange(key, fromTimestamp, toTimestamp));
  }

  @Override
  public List<TSElement> tsRange(String key, TSRangeParams rangeParams) {
    return executeCommand(getCommandObjects().tsRange(key, rangeParams));
  }

  @Override
  public List<TSElement> tsRevRange(String key, long fromTimestamp, long toTimestamp) {
    return executeCommand(getCommandObjects().tsRevRange(key, fromTimestamp, toTimestamp));
  }

  @Override
  public List<TSElement> tsRevRange(String key, TSRangeParams rangeParams) {
    return executeCommand(getCommandObjects().tsRevRange(key, rangeParams));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRange(long fromTimestamp, long toTimestamp,
      String... filters) {
    return executeCommand(getCommandObjects().tsMRange(fromTimestamp, toTimestamp, filters));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRange(TSMRangeParams multiRangeParams) {
    return executeCommand(getCommandObjects().tsMRange(multiRangeParams));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRevRange(long fromTimestamp, long toTimestamp,
      String... filters) {
    return executeCommand(getCommandObjects().tsMRevRange(fromTimestamp, toTimestamp, filters));
  }

  @Override
  public Map<String, TSMRangeElements> tsMRevRange(TSMRangeParams multiRangeParams) {
    return executeCommand(getCommandObjects().tsMRevRange(multiRangeParams));
  }

  @Override
  public TSElement tsGet(String key) {
    return executeCommand(getCommandObjects().tsGet(key));
  }

  @Override
  public TSElement tsGet(String key, TSGetParams getParams) {
    return executeCommand(getCommandObjects().tsGet(key, getParams));
  }

  @Override
  public Map<String, TSMGetElement> tsMGet(TSMGetParams multiGetParams, String... filters) {
    return executeCommand(getCommandObjects().tsMGet(multiGetParams, filters));
  }

  @Override
  public String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType,
      long timeBucket) {
    return executeCommand(
      getCommandObjects().tsCreateRule(sourceKey, destKey, aggregationType, timeBucket));
  }

  @Override
  public String tsCreateRule(String sourceKey, String destKey, AggregationType aggregationType,
      long bucketDuration, long alignTimestamp) {
    return executeCommand(getCommandObjects().tsCreateRule(sourceKey, destKey, aggregationType,
      bucketDuration, alignTimestamp));
  }

  @Override
  public String tsDeleteRule(String sourceKey, String destKey) {
    return executeCommand(getCommandObjects().tsDeleteRule(sourceKey, destKey));
  }

  @Override
  public List<String> tsQueryIndex(String... filters) {
    return executeCommand(getCommandObjects().tsQueryIndex(filters));
  }

  @Override
  public TSInfo tsInfo(String key) {
    return executeCommand(getCommandObjects().tsInfo(key));
  }

  @Override
  public TSInfo tsInfoDebug(String key) {
    return executeCommand(getCommandObjects().tsInfoDebug(key));
  }

  // RedisBloom commands
  @Override
  public String bfReserve(String key, double errorRate, long capacity) {
    return executeCommand(getCommandObjects().bfReserve(key, errorRate, capacity));
  }

  @Override
  public String bfReserve(String key, double errorRate, long capacity,
      BFReserveParams reserveParams) {
    return executeCommand(getCommandObjects().bfReserve(key, errorRate, capacity, reserveParams));
  }

  @Override
  public boolean bfAdd(String key, String item) {
    return executeCommand(getCommandObjects().bfAdd(key, item));
  }

  @Override
  public List<Boolean> bfMAdd(String key, String... items) {
    return executeCommand(getCommandObjects().bfMAdd(key, items));
  }

  @Override
  public List<Boolean> bfInsert(String key, String... items) {
    return executeCommand(getCommandObjects().bfInsert(key, items));
  }

  @Override
  public List<Boolean> bfInsert(String key, BFInsertParams insertParams, String... items) {
    return executeCommand(getCommandObjects().bfInsert(key, insertParams, items));
  }

  @Override
  public boolean bfExists(String key, String item) {
    return executeCommand(getCommandObjects().bfExists(key, item));
  }

  @Override
  public List<Boolean> bfMExists(String key, String... items) {
    return executeCommand(getCommandObjects().bfMExists(key, items));
  }

  @Override
  public Map.Entry<Long, byte[]> bfScanDump(String key, long iterator) {
    return executeCommand(getCommandObjects().bfScanDump(key, iterator));
  }

  @Override
  public String bfLoadChunk(String key, long iterator, byte[] data) {
    return executeCommand(getCommandObjects().bfLoadChunk(key, iterator, data));
  }

  @Override
  public long bfCard(String key) {
    return executeCommand(getCommandObjects().bfCard(key));
  }

  @Override
  public Map<String, Object> bfInfo(String key) {
    return executeCommand(getCommandObjects().bfInfo(key));
  }

  @Override
  public String cfReserve(String key, long capacity) {
    return executeCommand(getCommandObjects().cfReserve(key, capacity));
  }

  @Override
  public String cfReserve(String key, long capacity, CFReserveParams reserveParams) {
    return executeCommand(getCommandObjects().cfReserve(key, capacity, reserveParams));
  }

  @Override
  public boolean cfAdd(String key, String item) {
    return executeCommand(getCommandObjects().cfAdd(key, item));
  }

  @Override
  public boolean cfAddNx(String key, String item) {
    return executeCommand(getCommandObjects().cfAddNx(key, item));
  }

  @Override
  public List<Boolean> cfInsert(String key, String... items) {
    return executeCommand(getCommandObjects().cfInsert(key, items));
  }

  @Override
  public List<Boolean> cfInsert(String key, CFInsertParams insertParams, String... items) {
    return executeCommand(getCommandObjects().cfInsert(key, insertParams, items));
  }

  @Override
  public List<Boolean> cfInsertNx(String key, String... items) {
    return executeCommand(getCommandObjects().cfInsertNx(key, items));
  }

  @Override
  public List<Boolean> cfInsertNx(String key, CFInsertParams insertParams, String... items) {
    return executeCommand(getCommandObjects().cfInsertNx(key, insertParams, items));
  }

  @Override
  public boolean cfExists(String key, String item) {
    return executeCommand(getCommandObjects().cfExists(key, item));
  }

  @Override
  public List<Boolean> cfMExists(String key, String... items) {
    return executeCommand(getCommandObjects().cfMExists(key, items));
  }

  @Override
  public boolean cfDel(String key, String item) {
    return executeCommand(getCommandObjects().cfDel(key, item));
  }

  @Override
  public long cfCount(String key, String item) {
    return executeCommand(getCommandObjects().cfCount(key, item));
  }

  @Override
  public Map.Entry<Long, byte[]> cfScanDump(String key, long iterator) {
    return executeCommand(getCommandObjects().cfScanDump(key, iterator));
  }

  @Override
  public String cfLoadChunk(String key, long iterator, byte[] data) {
    return executeCommand(getCommandObjects().cfLoadChunk(key, iterator, data));
  }

  @Override
  public Map<String, Object> cfInfo(String key) {
    return executeCommand(getCommandObjects().cfInfo(key));
  }

  @Override
  public String cmsInitByDim(String key, long width, long depth) {
    return executeCommand(getCommandObjects().cmsInitByDim(key, width, depth));
  }

  @Override
  public String cmsInitByProb(String key, double error, double probability) {
    return executeCommand(getCommandObjects().cmsInitByProb(key, error, probability));
  }

  @Override
  public List<Long> cmsIncrBy(String key, Map<String, Long> itemIncrements) {
    return executeCommand(getCommandObjects().cmsIncrBy(key, itemIncrements));
  }

  @Override
  public List<Long> cmsQuery(String key, String... items) {
    return executeCommand(getCommandObjects().cmsQuery(key, items));
  }

  @Override
  public String cmsMerge(String destKey, String... keys) {
    return executeCommand(getCommandObjects().cmsMerge(destKey, keys));
  }

  @Override
  public String cmsMerge(String destKey, Map<String, Long> keysAndWeights) {
    return executeCommand(getCommandObjects().cmsMerge(destKey, keysAndWeights));
  }

  @Override
  public Map<String, Object> cmsInfo(String key) {
    return executeCommand(getCommandObjects().cmsInfo(key));
  }

  @Override
  public String topkReserve(String key, long topk) {
    return executeCommand(getCommandObjects().topkReserve(key, topk));
  }

  @Override
  public String topkReserve(String key, long topk, long width, long depth, double decay) {
    return executeCommand(getCommandObjects().topkReserve(key, topk, width, depth, decay));
  }

  @Override
  public List<String> topkAdd(String key, String... items) {
    return executeCommand(getCommandObjects().topkAdd(key, items));
  }

  @Override
  public List<String> topkIncrBy(String key, Map<String, Long> itemIncrements) {
    return executeCommand(getCommandObjects().topkIncrBy(key, itemIncrements));
  }

  @Override
  public List<Boolean> topkQuery(String key, String... items) {
    return executeCommand(getCommandObjects().topkQuery(key, items));
  }

  @Override
  public List<String> topkList(String key) {
    return executeCommand(getCommandObjects().topkList(key));
  }

  @Override
  public Map<String, Long> topkListWithCount(String key) {
    return executeCommand(getCommandObjects().topkListWithCount(key));
  }

  @Override
  public Map<String, Object> topkInfo(String key) {
    return executeCommand(getCommandObjects().topkInfo(key));
  }

  @Override
  public String tdigestCreate(String key) {
    return executeCommand(getCommandObjects().tdigestCreate(key));
  }

  @Override
  public String tdigestCreate(String key, int compression) {
    return executeCommand(getCommandObjects().tdigestCreate(key, compression));
  }

  @Override
  public String tdigestReset(String key) {
    return executeCommand(getCommandObjects().tdigestReset(key));
  }

  @Override
  public String tdigestMerge(String destinationKey, String... sourceKeys) {
    return executeCommand(getCommandObjects().tdigestMerge(destinationKey, sourceKeys));
  }

  @Override
  public String tdigestMerge(TDigestMergeParams mergeParams, String destinationKey,
      String... sourceKeys) {
    return executeCommand(
      getCommandObjects().tdigestMerge(mergeParams, destinationKey, sourceKeys));
  }

  @Override
  public Map<String, Object> tdigestInfo(String key) {
    return executeCommand(getCommandObjects().tdigestInfo(key));
  }

  @Override
  public String tdigestAdd(String key, double... values) {
    return executeCommand(getCommandObjects().tdigestAdd(key, values));
  }

  @Override
  public List<Double> tdigestCDF(String key, double... values) {
    return executeCommand(getCommandObjects().tdigestCDF(key, values));
  }

  @Override
  public List<Double> tdigestQuantile(String key, double... quantiles) {
    return executeCommand(getCommandObjects().tdigestQuantile(key, quantiles));
  }

  @Override
  public double tdigestMin(String key) {
    return executeCommand(getCommandObjects().tdigestMin(key));
  }

  @Override
  public double tdigestMax(String key) {
    return executeCommand(getCommandObjects().tdigestMax(key));
  }

  @Override
  public double tdigestTrimmedMean(String key, double lowCutQuantile, double highCutQuantile) {
    return executeCommand(
      getCommandObjects().tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile));
  }

  @Override
  public List<Long> tdigestRank(String key, double... values) {
    return executeCommand(getCommandObjects().tdigestRank(key, values));
  }

  @Override
  public List<Long> tdigestRevRank(String key, double... values) {
    return executeCommand(getCommandObjects().tdigestRevRank(key, values));
  }

  @Override
  public List<Double> tdigestByRank(String key, long... ranks) {
    return executeCommand(getCommandObjects().tdigestByRank(key, ranks));
  }

  @Override
  public List<Double> tdigestByRevRank(String key, long... ranks) {
    return executeCommand(getCommandObjects().tdigestByRevRank(key, ranks));
  }
}
