package redis.clients.jedis;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.args.*;
import redis.clients.jedis.commands.AllKeyBinaryCommands;
import redis.clients.jedis.commands.AllKeyCommands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.JedisClusterConnectionProvider;
import redis.clients.jedis.providers.JedisConnectionProvider;
import redis.clients.jedis.providers.SimpleJedisConnectionProvider;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.stream.*;
import redis.clients.jedis.util.IOUtils;

public class Jedis implements AllKeyCommands, AllKeyBinaryCommands, AutoCloseable {

  protected final JedisCommandExecutor executor;
  private final RedisCommandObjects commandObjects;

  public Jedis() {
    this(new HostAndPort(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT));
  }

  public Jedis(HostAndPort hostAndPort) {
    this(new SimpleJedisConnectionProvider(hostAndPort));
  }

  public Jedis(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new SimpleJedisConnectionProvider(hostAndPort, clientConfig));
  }

  public Jedis(JedisSocketFactory socketFactory) {
    this(new JedisConnection(socketFactory));
  }

  public Jedis(JedisConnection connection) {
    this(new SimpleJedisConnectionProvider(connection));
  }

  public Jedis(JedisConnectionProvider provider) {
    this.executor = new SimpleJedisExecutor(provider);
    this.commandObjects = (provider instanceof JedisClusterConnectionProvider)
        ? new RedisClusterCommandObjects() : new RedisCommandObjects();
  }

  public Jedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig, int maxAttempts) {
    this(new JedisClusterConnectionProvider(jedisClusterNodes, clientConfig), maxAttempts,
        Duration.ofMillis(maxAttempts * clientConfig.getSocketTimeoutMillis()));
  }

  public Jedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig, int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new JedisClusterConnectionProvider(jedisClusterNodes, clientConfig), maxAttempts, maxTotalRetriesDuration);
  }

  public Jedis(Set<HostAndPort> jedisClusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<JedisConnection> poolConfig, int maxAttempts, Duration maxTotalRetriesDuration) {
    this(new JedisClusterConnectionProvider(jedisClusterNodes, clientConfig, poolConfig), maxAttempts, maxTotalRetriesDuration);
  }

  public Jedis(JedisClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    if (provider instanceof JedisClusterConnectionProvider) {
      this.executor = new RetryableClusterCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration);
      this.commandObjects = new RedisClusterCommandObjects();
    } else {
      this.executor = new RetryableCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration);
      this.commandObjects = new RedisCommandObjects();
    }
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.executor);
  }

  protected final <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  // Key commands
  @Override
  public boolean exists(String key) {
    return executeCommand(commandObjects.exists(key));
  }

  @Override
  public long exists(String... keys) {
    return executeCommand(commandObjects.exists(keys));
  }

  @Override
  public long persist(String key) {
    return executeCommand(commandObjects.persist(key));
  }

  @Override
  public String type(String key) {
    return executeCommand(commandObjects.type(key));
  }

  @Override
  public boolean exists(byte[] key) {
    return executeCommand(commandObjects.exists(key));
  }

  @Override
  public long exists(byte[]... keys) {
    return executeCommand(commandObjects.exists(keys));
  }

  @Override
  public long persist(byte[] key) {
    return executeCommand(commandObjects.persist(key));
  }

  @Override
  public String type(byte[] key) {
    return executeCommand(commandObjects.type(key));
  }

  @Override
  public byte[] dump(String key) {
    return executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public byte[] dump(byte[] key) {
    return executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public long expire(String key, long seconds) {
    return executeCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public long pexpire(String key, long milliseconds) {
    return executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long expireAt(String key, long unixTime) {
    return executeCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long expire(byte[] key, long seconds) {
    return executeCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public long pexpire(byte[] key, long milliseconds) {
    return executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long expireAt(byte[] key, long unixTime) {
    return executeCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public long pexpireAt(byte[] key, long millisecondsTimestamp) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long ttl(String key) {
    return executeCommand(commandObjects.ttl(key));
  }

  @Override
  public long pttl(String key) {
    return executeCommand(commandObjects.pttl(key));
  }

  @Override
  public long touch(String key) {
    return executeCommand(commandObjects.touch(key));
  }

  @Override
  public long touch(String... keys) {
    return executeCommand(commandObjects.touch(keys));
  }

  @Override
  public long ttl(byte[] key) {
    return executeCommand(commandObjects.ttl(key));
  }

  @Override
  public long pttl(byte[] key) {
    return executeCommand(commandObjects.pttl(key));
  }

  @Override
  public long touch(byte[] key) {
    return executeCommand(commandObjects.touch(key));
  }

  @Override
  public long touch(byte[]... keys) {
    return executeCommand(commandObjects.touch(keys));
  }

  @Override
  public List<String> sort(String key) {
    return executeCommand(commandObjects.sort(key));
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParameters) {
    return executeCommand(commandObjects.sort(key, sortingParameters));
  }

  @Override
  public long sort(String key, String dstkey) {
    return executeCommand(commandObjects.sort(key, dstkey));
  }

  @Override
  public long sort(String key, SortingParams sortingParameters, String dstkey) {
    return executeCommand(commandObjects.sort(key, sortingParameters, dstkey));
  }

  @Override
  public List<byte[]> sort(byte[] key) {
    return executeCommand(commandObjects.sort(key));
  }

  @Override
  public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
    return executeCommand(commandObjects.sort(key, sortingParameters));
  }

  @Override
  public long sort(byte[] key, byte[] dstkey) {
    return executeCommand(commandObjects.sort(key, dstkey));
  }

  @Override
  public long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
    return executeCommand(commandObjects.sort(key, sortingParameters, dstkey));
  }

  @Override
  public long del(String key) {
    return executeCommand(commandObjects.del(key));
  }

  @Override
  public long del(String... keys) {
    return executeCommand(commandObjects.del(keys));
  }

  @Override
  public long unlink(String key) {
    return executeCommand(commandObjects.unlink(key));
  }

  @Override
  public long unlink(String... keys) {
    return executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public long del(byte[] key) {
    return executeCommand(commandObjects.del(key));
  }

  @Override
  public long del(byte[]... keys) {
    return executeCommand(commandObjects.del(keys));
  }

  @Override
  public long unlink(byte[] key) {
    return executeCommand(commandObjects.unlink(key));
  }

  @Override
  public long unlink(byte[]... keys) {
    return executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public Long memoryUsage(String key) {
    return executeCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Long memoryUsage(String key, int samples) {
    return executeCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public Long memoryUsage(byte[] key) {
    return executeCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Long memoryUsage(byte[] key, int samples) {
    return executeCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public boolean copy(String srcKey, String dstKey, boolean replace) {
    return executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(String oldkey, String newkey) {
    return executeCommand(commandObjects.rename(oldkey, newkey));
  }

  @Override
  public long renamenx(String oldkey, String newkey) {
    return executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  @Override
  public boolean copy(byte[] srcKey, byte[] dstKey, boolean replace) {
    return executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public String rename(byte[] oldkey, byte[] newkey) {
    return executeCommand(commandObjects.rename(oldkey, newkey));
  }

  @Override
  public long renamenx(byte[] oldkey, byte[] newkey) {
    return executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  @Override
  public Set<String> keys(String pattern) {
    return executeCommand(commandObjects.keys(pattern));
  }

  @Override
  public ScanResult<String> scan(String cursor) {
    return executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params) {
    return executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<String> scan(String cursor, ScanParams params, String type) {
    return executeCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public Set<byte[]> keys(byte[] pattern) {
    return executeCommand(commandObjects.keys(pattern));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor) {
    return executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type) {
    return executeCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public String randomKey() {
    return executeCommand(commandObjects.randomKey());
  }

  @Override
  public byte[] randomBinaryKey() {
    return executeCommand(commandObjects.randomBinaryKey());
  }
  // Key commands

  // String commands
  @Override
  public String set(String key, String value) {
    return executeCommand(commandObjects.set(key, value));
  }

  @Override
  public String set(String key, String value, SetParams params) {
    return executeCommand(commandObjects.set(key, value, params));
  }

  @Override
  public String get(String key) {
    return executeCommand(commandObjects.get(key));
  }

  @Override
  public String getDel(String key) {
    return executeCommand(commandObjects.getDel(key));
  }

  @Override
  public String getEx(String key, GetExParams params) {
    return executeCommand(commandObjects.getEx(key, params));
  }

  @Override
  public String set(byte[] key, byte[] value) {
    return executeCommand(commandObjects.set(key, value));
  }

  @Override
  public String set(byte[] key, byte[] value, SetParams params) {
    return executeCommand(commandObjects.set(key, value, params));
  }

  @Override
  public byte[] get(byte[] key) {
    return executeCommand(commandObjects.get(key));
  }

  @Override
  public byte[] getDel(byte[] key) {
    return executeCommand(commandObjects.getDel(key));
  }

  @Override
  public byte[] getEx(byte[] key, GetExParams params) {
    return executeCommand(commandObjects.getEx(key, params));
  }

  @Override
  public boolean setbit(String key, long offset, boolean value) {
    return executeCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public boolean getbit(String key, long offset) {
    return executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(String key, long offset, String value) {
    return executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    return executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public boolean setbit(byte[] key, long offset, boolean value) {
    return executeCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public boolean getbit(byte[] key, long offset) {
    return executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(byte[] key, long offset, byte[] value) {
    return executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public byte[] getrange(byte[] key, long startOffset, long endOffset) {
    return executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public String getSet(String key, String value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  @Override
  public long setnx(String key, String value) {
    return executeCommand(commandObjects.setnx(key, value));
  }

  @Override
  public String setex(String key, long seconds, String value) {
    return executeCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public String psetex(String key, long milliseconds, String value) {
    return executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public byte[] getSet(byte[] key, byte[] value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  @Override
  public long setnx(byte[] key, byte[] value) {
    return executeCommand(commandObjects.setnx(key, value));
  }

  @Override
  public String setex(byte[] key, long seconds, byte[] value) {
    return executeCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public String psetex(byte[] key, long milliseconds, byte[] value) {
    return executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public long incr(String key) {
    return executeCommand(commandObjects.incr(key));
  }

  @Override
  public long incrBy(String key, long increment) {
    return executeCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public double incrByFloat(String key, double increment) {
    return executeCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public long decr(String key) {
    return executeCommand(commandObjects.decr(key));
  }

  @Override
  public long decrBy(String key, long decrement) {
    return executeCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public long incr(byte[] key) {
    return executeCommand(commandObjects.incr(key));
  }

  @Override
  public long incrBy(byte[] key, long increment) {
    return executeCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public double incrByFloat(byte[] key, double increment) {
    return executeCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public long decr(byte[] key) {
    return executeCommand(commandObjects.decr(key));
  }

  @Override
  public long decrBy(byte[] key, long decrement) {
    return executeCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public List<String> mget(String... keys) {
    return executeCommand(commandObjects.mget(keys));
  }

  @Override
  public String mset(String... keysvalues) {
    return executeCommand(commandObjects.mset(keysvalues));
  }

  @Override
  public long msetnx(String... keysvalues) {
    return executeCommand(commandObjects.msetnx(keysvalues));
  }

  @Override
  public List<byte[]> mget(byte[]... keys) {
    return executeCommand(commandObjects.mget(keys));
  }

  @Override
  public String mset(byte[]... keysvalues) {
    return executeCommand(commandObjects.mset(keysvalues));
  }

  @Override
  public long msetnx(byte[]... keysvalues) {
    return executeCommand(commandObjects.msetnx(keysvalues));
  }

  @Override
  public long append(String key, String value) {
    return executeCommand(commandObjects.append(key, value));
  }

  @Override
  public String substr(String key, int start, int end) {
    return executeCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public long strlen(String key) {
    return executeCommand(commandObjects.strlen(key));
  }

  @Override
  public long append(byte[] key, byte[] value) {
    return executeCommand(commandObjects.append(key, value));
  }

  @Override
  public byte[] substr(byte[] key, int start, int end) {
    return executeCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public long strlen(byte[] key) {
    return executeCommand(commandObjects.strlen(key));
  }

  @Override
  public long bitcount(String key) {
    return executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(String key, long start, long end) {
    return executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitpos(String key, boolean value) {
    return executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(String key, boolean value, BitPosParams params) {
    return executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public long bitcount(byte[] key) {
    return executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(byte[] key, long start, long end) {
    return executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitpos(byte[] key, boolean value) {
    return executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(byte[] key, boolean value, BitPosParams params) {
    return executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public List<Long> bitfield(String key, String... arguments) {
    return executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(String key, String... arguments) {
    return executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public List<Long> bitfield(byte[] key, byte[]... arguments) {
    return executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(byte[] key, byte[]... arguments) {
    return executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public long bitop(BitOP op, String destKey, String... srcKeys) {
    return executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    return executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public LCSMatchResult strAlgoLCSKeys(final String keyA, final String keyB, final StrAlgoLCSParams params) {
    return executeCommand(commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }

  @Override
  public LCSMatchResult strAlgoLCSKeys(byte[] keyA, byte[] keyB, StrAlgoLCSParams params) {
    return executeCommand(commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }
  // String commands

  // List commands
  @Override
  public long rpush(String key, String... string) {
    return executeCommand(commandObjects.rpush(key, string));
  }

  @Override
  public long lpush(String key, String... string) {
    return executeCommand(commandObjects.lpush(key, string));
  }

  @Override
  public long llen(String key) {
    return executeCommand(commandObjects.llen(key));
  }

  @Override
  public List<String> lrange(String key, long start, long stop) {
    return executeCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public String ltrim(String key, long start, long stop) {
    return executeCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public String lindex(String key, long index) {
    return executeCommand(commandObjects.lindex(key, index));
  }

  @Override
  public long rpush(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.rpush(key, args));
  }

  @Override
  public long lpush(byte[] key, byte[]... args) {
    return executeCommand(commandObjects.lpush(key, args));
  }

  @Override
  public long llen(byte[] key) {
    return executeCommand(commandObjects.llen(key));
  }

  @Override
  public List<byte[]> lrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public String ltrim(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public byte[] lindex(byte[] key, long index) {
    return executeCommand(commandObjects.lindex(key, index));
  }

  @Override
  public String lset(String key, long index, String value) {
    return executeCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public long lrem(String key, long count, String value) {
    return executeCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public String lpop(String key) {
    return executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<String> lpop(String key, int count) {
    return executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public String lset(byte[] key, long index, byte[] value) {
    return executeCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public long lrem(byte[] key, long count, byte[] value) {
    return executeCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public byte[] lpop(byte[] key) {
    return executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<byte[]> lpop(byte[] key, int count) {
    return executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public Long lpos(String key, String element) {
    return executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(String key, String element, LPosParams params) {
    return executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(String key, String element, LPosParams params, long count) {
    return executeCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public Long lpos(byte[] key, byte[] element) {
    return executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(byte[] key, byte[] element, LPosParams params) {
    return executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(byte[] key, byte[] element, LPosParams params, long count) {
    return executeCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public String rpop(String key) {
    return executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<String> rpop(String key, int count) {
    return executeCommand(commandObjects.rpop(key, count));
  }

  @Override
  public byte[] rpop(byte[] key) {
    return executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<byte[]> rpop(byte[] key, int count) {
    return executeCommand(commandObjects.rpop(key, count));
  }

  @Override
  public long linsert(String key, ListPosition where, String pivot, String value) {
    return executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(String key, String... string) {
    return executeCommand(commandObjects.lpushx(key, string));
  }

  @Override
  public long rpushx(String key, String... string) {
    return executeCommand(commandObjects.rpushx(key, string));
  }

  @Override
  public long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
    return executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(byte[] key, byte[]... arg) {
    return executeCommand(commandObjects.lpushx(key, arg));
  }

  @Override
  public long rpushx(byte[] key, byte[]... arg) {
    return executeCommand(commandObjects.rpushx(key, arg));
  }

  @Override
  public List<String> blpop(int timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public KeyedListElement blpop(double timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public List<String> brpop(int timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public KeyedListElement brpop(double timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public List<String> blpop(int timeout, String... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public KeyedListElement blpop(double timeout, String... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<String> brpop(int timeout, String... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public KeyedListElement brpop(double timeout, String... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(int timeout, byte[]... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<byte[]> brpop(int timeout, byte[]... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public List<byte[]> brpop(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public String rpoplpush(String srckey, String dstkey) {
    return executeCommand(commandObjects.rpoplpush(srckey, dstkey));
  }

  @Override
  public String brpoplpush(String source, String destination, int timeout) {
    return executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
    return executeCommand(commandObjects.rpoplpush(srckey, dstkey));
  }

  @Override
  public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
    return executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    return executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout) {
    return executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }
  // List commands

  // Hash commands
  @Override
  public long hset(String key, String field, String value) {
    return executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hset(key, hash));
  }

  @Override
  public String hget(String key, String field) {
    return executeCommand(commandObjects.hget(key, field));
  }

  @Override
  public long hsetnx(String key, String field, String value) {
    return executeCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    return executeCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public long hset(byte[] key, byte[] field, byte[] value) {
    return executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(commandObjects.hset(key, hash));
  }

  @Override
  public byte[] hget(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hget(key, field));
  }

  @Override
  public long hsetnx(byte[] key, byte[] field, byte[] value) {
    return executeCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    return executeCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    return executeCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public long hincrBy(String key, String field, long value) {
    return executeCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(String key, String field, double value) {
    return executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(String key, String field) {
    return executeCommand(commandObjects.hexists(key, field));
  }

  @Override
  public long hdel(String key, String... field) {
    return executeCommand(commandObjects.hdel(key, field));
  }

  @Override
  public long hlen(String key) {
    return executeCommand(commandObjects.hlen(key));
  }

  @Override
  public long hincrBy(byte[] key, byte[] field, long value) {
    return executeCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(byte[] key, byte[] field, double value) {
    return executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hexists(key, field));
  }

  @Override
  public long hdel(byte[] key, byte[]... field) {
    return executeCommand(commandObjects.hdel(key, field));
  }

  @Override
  public long hlen(byte[] key) {
    return executeCommand(commandObjects.hlen(key));
  }

  @Override
  public Set<String> hkeys(String key) {
    return executeCommand(commandObjects.hkeys(key));
  }

  @Override
  public List<String> hvals(String key) {
    return executeCommand(commandObjects.hvals(key));
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    return executeCommand(commandObjects.hgetAll(key));
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    return executeCommand(commandObjects.hkeys(key));
  }

  @Override
  public List<byte[]> hvals(byte[] key) {
    return executeCommand(commandObjects.hvals(key));
  }

  @Override
  public Map<byte[], byte[]> hgetAll(byte[] key) {
    return executeCommand(commandObjects.hgetAll(key));
  }

  @Override
  public String hrandfield(String key) {
    return executeCommand(commandObjects.hrandfield(key));
  }

  @Override
  public List<String> hrandfield(String key, long count) {
    return executeCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public Map<String, String> hrandfieldWithValues(String key, long count) {
    return executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public long hstrlen(String key, String field) {
    return executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public byte[] hrandfield(byte[] key) {
    return executeCommand(commandObjects.hrandfield(key));
  }

  @Override
  public List<byte[]> hrandfield(byte[] key, long count) {
    return executeCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public Map<byte[], byte[]> hrandfieldWithValues(byte[] key, long count) {
    return executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public long hstrlen(byte[] key, byte[] field) {
    return executeCommand(commandObjects.hstrlen(key, field));
  }
  // Hash commands

  // Set commands
  @Override
  public long sadd(String key, String... member) {
    return executeCommand(commandObjects.sadd(key, member));
  }

  @Override
  public Set<String> smembers(String key) {
    return executeCommand(commandObjects.smembers(key));
  }

  @Override
  public long srem(String key, String... member) {
    return executeCommand(commandObjects.srem(key, member));
  }

  @Override
  public String spop(String key) {
    return executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<String> spop(String key, long count) {
    return executeCommand(commandObjects.spop(key, count));
  }

  @Override
  public long scard(String key) {
    return executeCommand(commandObjects.scard(key));
  }

  @Override
  public boolean sismember(String key, String member) {
    return executeCommand(commandObjects.sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(String key, String... members) {
    return executeCommand(commandObjects.smismember(key, members));
  }

  @Override
  public long sadd(byte[] key, byte[]... member) {
    return executeCommand(commandObjects.sadd(key, member));
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    return executeCommand(commandObjects.smembers(key));
  }

  @Override
  public long srem(byte[] key, byte[]... member) {
    return executeCommand(commandObjects.srem(key, member));
  }

  @Override
  public byte[] spop(byte[] key) {
    return executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    return executeCommand(commandObjects.spop(key, count));
  }

  @Override
  public long scard(byte[] key) {
    return executeCommand(commandObjects.scard(key));
  }

  @Override
  public boolean sismember(byte[] key, byte[] member) {
    return executeCommand(commandObjects.sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.smismember(key, members));
  }

  @Override
  public String srandmember(String key) {
    return executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<String> srandmember(String key, int count) {
    return executeCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public byte[] srandmember(byte[] key) {
    return executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<byte[]> srandmember(byte[] key, int count) {
    return executeCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public Set<String> sdiff(String... keys) {
    return executeCommand(commandObjects.sdiff(keys));
  }

  @Override
  public long sdiffstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sdiffstore(dstkey, keys));
  }

  @Override
  public Set<String> sinter(String... keys) {
    return executeCommand(commandObjects.sinter(keys));
  }

  @Override
  public long sinterstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  @Override
  public Set<String> sunion(String... keys) {
    return executeCommand(commandObjects.sunion(keys));
  }

  @Override
  public long sunionstore(String dstkey, String... keys) {
    return executeCommand(commandObjects.sunionstore(dstkey, keys));
  }

  @Override
  public long smove(String srckey, String dstkey, String member) {
    return executeCommand(commandObjects.smove(srckey, dstkey, member));
  }

  @Override
  public Set<byte[]> sdiff(byte[]... keys) {
    return executeCommand(commandObjects.sdiff(keys));
  }

  @Override
  public long sdiffstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sdiffstore(dstkey, keys));
  }

  @Override
  public Set<byte[]> sinter(byte[]... keys) {
    return executeCommand(commandObjects.sinter(keys));
  }

  @Override
  public long sinterstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  @Override
  public Set<byte[]> sunion(byte[]... keys) {
    return executeCommand(commandObjects.sunion(keys));
  }

  @Override
  public long sunionstore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.sunionstore(dstkey, keys));
  }

  @Override
  public long smove(byte[] srckey, byte[] dstkey, byte[] member) {
    return executeCommand(commandObjects.smove(srckey, dstkey, member));
  }
  // Set commands

  // Sorted Set commands
  @Override
  public long zadd(String key, double score, String member) {
    return executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers) {
    return executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member) {
    return executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    return executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(byte[] key, double score, byte[] member, ZAddParams params) {
    return executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public long zrem(String key, String... members) {
    return executeCommand(commandObjects.zrem(key, members));
  }

  @Override
  public double zincrby(String key, double increment, String member) {
    return executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
    return executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(String key, String member) {
    return executeCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Long zrevrank(String key, String member) {
    return executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public long zrem(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.zrem(key, members));
  }

  @Override
  public double zincrby(byte[] key, double increment, byte[] member) {
    return executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
    return executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Long zrevrank(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public String zrandmember(String key) {
    return executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public Set<String> zrandmember(String key, long count) {
    return executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public Set<Tuple> zrandmemberWithScores(String key, long count) {
    return executeCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(String key) {
    return executeCommand(commandObjects.zcard(key));
  }

  @Override
  public Double zscore(String key, String member) {
    return executeCommand(commandObjects.zscore(key, member));
  }

  @Override
  public List<Double> zmscore(String key, String... members) {
    return executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public byte[] zrandmember(byte[] key) {
    return executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public Set<byte[]> zrandmember(byte[] key, long count) {
    return executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public Set<Tuple> zrandmemberWithScores(byte[] key, long count) {
    return executeCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(byte[] key) {
    return executeCommand(commandObjects.zcard(key));
  }

  @Override
  public Double zscore(byte[] key, byte[] member) {
    return executeCommand(commandObjects.zscore(key, member));
  }

  @Override
  public List<Double> zmscore(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(String key) {
    return executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public Set<Tuple> zpopmax(String key, int count) {
    return executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(String key) {
    return executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public Set<Tuple> zpopmin(String key, int count) {
    return executeCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public long zcount(String key, double min, double max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(String key, String min, String max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Tuple zpopmax(byte[] key) {
    return executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public Set<Tuple> zpopmax(byte[] key, int count) {
    return executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(byte[] key) {
    return executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public Set<Tuple> zpopmin(byte[] key, int count) {
    return executeCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public long zcount(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Set<String> zrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public Set<String> zrevrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public Set<Tuple> zrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Set<byte[]> zrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public Set<byte[]> zrevrange(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public Set<Tuple> zrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByRank(String key, long start, long stop) {
    return executeCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByRank(byte[] key, long start, long stop) {
    return executeCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(byte[] key, double min, double max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zlexcount(String key, String min, String max) {
    return executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public Set<String> zrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public long zlexcount(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
    return executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
    return executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public KeyedZSetElement bzpopmax(double timeout, String... keys) {
    return executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public KeyedZSetElement bzpopmin(double timeout, String... keys) {
    return executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public List<byte[]> bzpopmax(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public List<byte[]> bzpopmin(double timeout, byte[]... keys) {
    return executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public Set<String> zdiff(String... keys) {
    return executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public Set<Tuple> zdiffWithScores(String... keys) {
    return executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  public long zdiffStore(String dstkey, String... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Override
  public Set<byte[]> zdiff(byte[]... keys) {
    return executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public Set<Tuple> zdiffWithScores(byte[]... keys) {
    return executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  public long zdiffStore(byte[] dstkey, byte[]... keys) {
    return executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Override
  public long zinterstore(String dstkey, String... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public Set<String> zinter(ZParams params, String... keys) {
    return executeCommand(commandObjects.zinter(params, keys));
  }

  @Override
  public Set<Tuple> zinterWithScores(ZParams params, String... keys) {
    return executeCommand(commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public long zinterstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, sets));
  }

  @Override
  public long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public Set<byte[]> zinter(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zinter(params, keys));
  }

  @Override
  public Set<Tuple> zinterWithScores(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public Set<String> zunion(ZParams params, String... keys) {
    return executeCommand(commandObjects.zunion(params, keys));
  }

  @Override
  public Set<Tuple> zunionWithScores(ZParams params, String... keys) {
    return executeCommand(commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(String dstkey, String... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(String dstkey, ZParams params, String... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }

  @Override
  public Set<byte[]> zunion(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zunion(params, keys));
  }

  @Override
  public Set<Tuple> zunionWithScores(ZParams params, byte[]... keys) {
    return executeCommand(commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public long zunionstore(byte[] dstkey, byte[]... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, sets));
  }

  @Override
  public long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    return executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }
  // Sorted Set commands

  // Geo commands
  @Override
  public long geoadd(String key, double longitude, double latitude, String member) {
    return executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(String key, String member1, String member2) {
    return executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(String key, String member1, String member2, GeoUnit unit) {
    return executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<String> geohash(String key, String... members) {
    return executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(String key, String... members) {
    return executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    return executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(byte[] key, GeoAddParams params, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2) {
    return executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    return executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<byte[]> geohash(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
    return executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long georadiusStore(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public long georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }
  // Geo commands

  // Hyper Log Log commands
  @Override
  public long pfadd(String key, String... elements) {
    return executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public String pfmerge(String destkey, String... sourcekeys) {
    return executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(String key) {
    return executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public long pfcount(String... keys) {
    return executeCommand(commandObjects.pfcount(keys));
  }

  @Override
  public long pfadd(byte[] key, byte[]... elements) {
    return executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public String pfmerge(byte[] destkey, byte[]... sourcekeys) {
    return executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(byte[] key) {
    return executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public long pfcount(byte[]... keys) {
    return executeCommand(commandObjects.pfcount(keys));
  }
  // Hyper Log Log commands

  // Stream commands
  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return executeCommand(commandObjects.xadd(key, id, hash));
  }

  @Override
  public StreamEntryID xadd(String key, Map<String, String> hash, XAddParams params) {
    return executeCommand(commandObjects.xadd(key, hash, params));
  }

  @Override
  public long xlen(String key) {
    return executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(String key, String group, StreamEntryID... ids) {
    return executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
    return executeCommand(commandObjects.xgroupCreate(key, groupname, id, makeStream));
  }

  @Override
  public String xgroupSetID(String key, String groupname, StreamEntryID id) {
    return executeCommand(commandObjects.xgroupSetID(key, groupname, id));
  }

  @Override
  public long xgroupDestroy(String key, String groupname) {
    return executeCommand(commandObjects.xgroupDestroy(key, groupname));
  }

  @Override
  public long xgroupDelConsumer(String key, String groupname, String consumername) {
    return executeCommand(commandObjects.xgroupDelConsumer(key, groupname, consumername));
  }

  @Override
  public StreamPendingSummary xpending(String key, String groupname) {
    return executeCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername) {
    return executeCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, XPendingParams params) {
    return executeCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public long xdel(String key, StreamEntryID... ids) {
    return executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(String key, long maxLen, boolean approximate) {
    return executeCommand(commandObjects.xtrim(key, maxLen, approximate));
  }

  @Override
  public long xtrim(String key, XTrimParams params) {
    return executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaim(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public StreamInfo xinfoStream(String key) {
    return executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public List<StreamGroupInfo> xinfoGroup(String key) {
    return executeCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    return executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xread(xReadParams, streams));
  }

  public List<Entry<String, List<StreamEntry>>> xreadGroup(final String groupname,
      final String consumer, final int count, final long block, final boolean noAck,
      final Entry<String, StreamEntryID>... streams) {
    if (block > Integer.MAX_VALUE) throw new IllegalArgumentException();
    XReadGroupParams params = XReadGroupParams.xReadGroupParams();
    if (count > 0) params.count(count);
    if (block > 0) params.block((int) block);
    if (noAck) params.noAck();
    Map<String, StreamEntryID> streamMap = new java.util.LinkedHashMap<>(streams.length);
    for (Entry<String, StreamEntryID> stream : streams) {
      streamMap.put(stream.getKey(), stream.getValue());
    }
    return xreadGroup(groupname, consumer, params, streamMap);
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return executeCommand(commandObjects.xreadGroup(groupname, consumer, xReadGroupParams, streams));
  }

  @Override
  public byte[] xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    return executeCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public long xlen(byte[] key) {
    return executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<byte[]> xrange(byte[] key, byte[] start, byte[] end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<byte[]> xrange(byte[] key, byte[] start, byte[] end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(byte[] key, byte[] group, byte[]... ids) {
    return executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(byte[] key, byte[] groupname, byte[] id, boolean makeStream) {
    return executeCommand(commandObjects.xgroupCreate(key, groupname, id, makeStream));
  }

  @Override
  public String xgroupSetID(byte[] key, byte[] groupname, byte[] id) {
    return executeCommand(commandObjects.xgroupSetID(key, groupname, id));
  }

  @Override
  public long xgroupDestroy(byte[] key, byte[] groupname) {
    return executeCommand(commandObjects.xgroupDestroy(key, groupname));
  }

  @Override
  public long xgroupDelConsumer(byte[] key, byte[] groupname, byte[] consumerName) {
    return executeCommand(commandObjects.xgroupDelConsumer(key, groupname, consumerName));
  }

  @Override
  public long xdel(byte[] key, byte[]... ids) {
    return executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(byte[] key, long maxLen, boolean approximateLength) {
    return executeCommand(commandObjects.xtrim(key, maxLen, approximateLength));
  }

  @Override
  public long xtrim(byte[] key, XTrimParams params) {
    return executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public Object xpending(byte[] key, byte[] groupname) {
    return executeCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public List<Object> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername) {
    return executeCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public List<Object> xpending(byte[] key, byte[] groupname, XPendingParams params) {
    return executeCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids) {
    return executeCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaim(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName, long minIdleTime, byte[] start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public Object xinfoStream(byte[] key) {
    return executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public List<Object> xinfoGroup(byte[] key) {
    return executeCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public List<Object> xinfoConsumers(byte[] key, byte[] group) {
    return executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public List<byte[]> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams) {
    return executeCommand(commandObjects.xreadGroup(groupname, consumer, xReadGroupParams, streams));
  }
  // Stream commands

  // Scripting commands
  @Override
  public Object eval(String script) {
    return executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object eval(String script, int keyCount, String... params) {
    return executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(String script, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Object evalsha(String sha1) {
    return executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(String sha1, int keyCount, String... params) {
    return executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(String sha1, List<String> keys, List<String> args) {
    return executeCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Object eval(byte[] script) {
    return executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object eval(byte[] script, int keyCount, byte[]... params) {
    return executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Object evalsha(byte[] sha1) {
    return executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
    return executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    return executeCommand(commandObjects.evalsha(sha1, keys, args));
  }
  // Scripting commands

  public LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params) {
    return executeCommand(commandObjects.strAlgoLCSStrings(strA, strB, params));
  }

}
