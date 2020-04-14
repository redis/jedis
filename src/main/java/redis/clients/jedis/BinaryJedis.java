package redis.clients.jedis;

import static redis.clients.jedis.Protocol.toByteArray;

import java.io.Closeable;
import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.commands.AdvancedBinaryJedisCommands;
import redis.clients.jedis.commands.BasicCommands;
import redis.clients.jedis.commands.BinaryJedisCommands;
import redis.clients.jedis.commands.BinaryScriptingCommands;
import redis.clients.jedis.commands.MultiKeyBinaryCommands;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisURIHelper;

public class BinaryJedis implements BasicCommands, BinaryJedisCommands, MultiKeyBinaryCommands,
    AdvancedBinaryJedisCommands, BinaryScriptingCommands, Closeable {
  protected Client client = null;
  protected Transaction transaction = null;
  protected Pipeline pipeline = null;
  private final byte[][] dummyArray = new byte[0][];

  public BinaryJedis() {
    client = new Client();
  }

  public BinaryJedis(final String host) {
    URI uri = URI.create(host);
    if (JedisURIHelper.isValid(uri)) {
      initializeClientFromURI(uri);
    } else {
      client = new Client(host);
    }
  }

  public BinaryJedis(final HostAndPort hp) {
    this(hp.getHost(), hp.getPort());
  }

  public BinaryJedis(final String host, final int port) {
    client = new Client(host, port);
  }

  public BinaryJedis(final String host, final int port, final boolean ssl) {
    client = new Client(host, port, ssl);
  }

  public BinaryJedis(final String host, final int port, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    client = new Client(host, port, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public BinaryJedis(final String host, final int port, final int timeout) {
    this(host, port, timeout, timeout);
  }

  public BinaryJedis(final String host, final int port, final int timeout, final boolean ssl) {
    this(host, port, timeout, timeout, ssl);
  }

  public BinaryJedis(final String host, final int port, final int timeout, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, timeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public BinaryJedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout) {
    client = new Client(host, port);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
  }

  public BinaryJedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl) {
    client = new Client(host, port, ssl);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
  }

  public BinaryJedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    client = new Client(host, port, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
  }

  public BinaryJedis(final JedisShardInfo shardInfo) {
    client = new Client(shardInfo.getHost(), shardInfo.getPort(), shardInfo.getSsl(),
        shardInfo.getSslSocketFactory(), shardInfo.getSslParameters(),
        shardInfo.getHostnameVerifier());
    client.setConnectionTimeout(shardInfo.getConnectionTimeout());
    client.setSoTimeout(shardInfo.getSoTimeout());
    client.setUser(shardInfo.getUser());
    client.setPassword(shardInfo.getPassword());
    client.setDb(shardInfo.getDb());
  }

  public BinaryJedis(URI uri) {
    initializeClientFromURI(uri);
  }

  public BinaryJedis(URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    initializeClientFromURI(uri, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public BinaryJedis(final URI uri, final int timeout) {
    this(uri, timeout, timeout);
  }

  public BinaryJedis(final URI uri, final int timeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, timeout, timeout, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public BinaryJedis(final URI uri, final int connectionTimeout, final int soTimeout) {
    initializeClientFromURI(uri);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
  }

  public BinaryJedis(final URI uri, final int connectionTimeout, final int soTimeout,
      final SSLSocketFactory sslSocketFactory,final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    initializeClientFromURI(uri, sslSocketFactory, sslParameters, hostnameVerifier);
    client.setConnectionTimeout(connectionTimeout);
    client.setSoTimeout(soTimeout);
  }

  public BinaryJedis(final JedisSocketFactory jedisSocketFactory) {
    client = new Client(jedisSocketFactory);
  }

  private void initializeClientFromURI(URI uri) {
    initializeClientFromURI(uri, null, null, null);
  }

  private void initializeClientFromURI(URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI. %s", uri.toString()));
    }

    client = new Client(uri.getHost(), uri.getPort(), JedisURIHelper.isRedisSSLScheme(uri),
      sslSocketFactory, sslParameters, hostnameVerifier);

    String password = JedisURIHelper.getPassword(uri);
    if (password != null) {
      String user = JedisURIHelper.getUser(uri);
      if (user == null) {
        client.auth(password);
      } else {
        client.auth(user, password);
      }
      client.getStatusCodeReply();
    }

    int dbIndex = JedisURIHelper.getDBIndex(uri);
    if (dbIndex > 0) {
      client.select(dbIndex);
      client.getStatusCodeReply();
      client.setDb(dbIndex);
    }
  }

  @Override
  public String ping() {
    checkIsInMultiOrPipeline();
    client.ping();
    return client.getStatusCodeReply();
  }

  /**
   * Works same as <tt>ping()</tt> but returns argument message instead of <tt>PONG</tt>.
   * @param message
   * @return message
   */
  public byte[] ping(final byte[] message) {
    checkIsInMultiOrPipeline();
    client.ping(message);
    return client.getBinaryBulkReply();
  }

  /**
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
   * GB).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return Status code reply
   */
  @Override
  public String set(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.set(key, value);
    return client.getStatusCodeReply();
  }

  /**
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
   * GB).
   * @param key
   * @param value
   * @param params
   * @return Status code reply
   */
  @Override
  public String set(final byte[] key, final byte[] value, final SetParams params) {
    checkIsInMultiOrPipeline();
    client.set(key, value, params);
    return client.getStatusCodeReply();
  }

  /**
   * Get the value of the specified key. If the key does not exist the special value 'nil' is
   * returned. If the value stored at key is not a string an error is returned because GET can only
   * handle string values.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return Bulk reply
   */
  @Override
  public byte[] get(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.get(key);
    return client.getBinaryBulkReply();
  }

  /**
   * Ask the server to silently close the connection.
   */
  @Override
  public String quit() {
    checkIsInMultiOrPipeline();
    client.quit();
    String quitReturn = client.getStatusCodeReply();
    client.disconnect();
    return quitReturn;
  }

  /**
   * Test if the specified keys exist. The command returns the number of keys exist.
   * Time complexity: O(N)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys exist,
   *         0 if none of the specified keys exist.
   */
  @Override
  public Long exists(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.exists(keys);
    return client.getIntegerReply();
  }

  /**
   * Test if the specified key exists. The command returns true if the key exists, otherwise false is
   * returned. Note that even keys set with an empty string as value will return true. Time
   * complexity: O(1)
   * @param key
   * @return Boolean reply, true if the key exists, otherwise false
   */
  @Override
  public Boolean exists(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.exists(key);
    return client.getIntegerReply() == 1;
  }

  /**
   * Remove the specified keys. If a given key does not exist no operation is performed for this
   * key. The command returns the number of keys removed. Time complexity: O(1)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys were removed
   *         0 if none of the specified key existed
   */
  @Override
  public Long del(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.del(keys);
    return client.getIntegerReply();
  }

  @Override
  public Long del(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.del(key);
    return client.getIntegerReply();
  }

  /**
   * This command is very similar to DEL: it removes the specified keys. Just like DEL a key is
   * ignored if it does not exist. However the command performs the actual memory reclaiming in a
   * different thread, so it is not blocking, while DEL is. This is where the command name comes
   * from: the command just unlinks the keys from the keyspace. The actual removal will happen later
   * asynchronously.
   * <p>
   * Time complexity: O(1) for each key removed regardless of its size. Then the command does O(N)
   * work in a different thread in order to reclaim memory, where N is the number of allocations the
   * deleted objects where composed of.
   * @param keys
   * @return Integer reply: The number of keys that were unlinked
   */
  @Override
  public Long unlink(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.unlink(keys);
    return client.getIntegerReply();
  }

  @Override
  public Long unlink(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.unlink(key);
    return client.getIntegerReply();
  }

  /**
   * Return the type of the value stored at key in form of a string. The type can be one of "none",
   * "string", "list", "set". "none" is returned if the key does not exist. Time complexity: O(1)
   * @param key
   * @return Status code reply, specifically: "none" if the key does not exist "string" if the key
   *         contains a String value "list" if the key contains a List value "set" if the key
   *         contains a Set value "zset" if the key contains a Sorted Set value "hash" if the key
   *         contains a Hash value
   */
  @Override
  public String type(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.type(key);
    return client.getStatusCodeReply();
  }

  /**
   * Delete all the keys of the currently selected DB. This command never fails.
   * @return Status code reply
   */
  @Override
  public String flushDB() {
    checkIsInMultiOrPipeline();
    client.flushDB();
    return client.getStatusCodeReply();
  }

  /**
   * Returns all the keys matching the glob-style pattern as space separated strings. For example if
   * you have in the database the keys "foo" and "foobar" the command "KEYS foo*" will return
   * "foo foobar".
   * <p>
   * Note that while the time complexity for this operation is O(n) the constant times are pretty
   * low. For example Redis running on an entry level laptop can scan a 1 million keys database in
   * 40 milliseconds. <b>Still it's better to consider this one of the slow commands that may ruin
   * the DB performance if not used with care.</b>
   * <p>
   * In other words this command is intended only for debugging and special operations like creating
   * a script to change the DB schema. Don't use it in your normal code. Use Redis Sets in order to
   * group together a subset of objects.
   * <p>
   * Glob style patterns examples:
   * <ul>
   * <li>h?llo will match hello hallo hhllo
   * <li>h*llo will match hllo heeeello
   * <li>h[ae]llo will match hello and hallo, but not hillo
   * </ul>
   * <p>
   * Use \ to escape special chars if you want to match them verbatim.
   * <p>
   * Time complexity: O(n) (with n being the number of keys in the DB, and assuming keys and pattern
   * of limited length)
   * @param pattern
   * @return Multi bulk reply
   */
  @Override
  public Set<byte[]> keys(final byte[] pattern) {
    checkIsInMultiOrPipeline();
    client.keys(pattern);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Return a randomly selected key from the currently selected DB.
   * <p>
   * Time complexity: O(1)
   * @return Single line reply, specifically the randomly selected key or an empty string is the
   *         database is empty
   */
  @Override
  public byte[] randomBinaryKey() {
    checkIsInMultiOrPipeline();
    client.randomKey();
    return client.getBinaryBulkReply();
  }

  /**
   * Atomically renames the key oldkey to newkey. If the source and destination name are the same an
   * error is returned. If newkey already exists it is overwritten.
   * <p>
   * Time complexity: O(1)
   * @param oldkey
   * @param newkey
   * @return Status code repy
   */
  @Override
  public String rename(final byte[] oldkey, final byte[] newkey) {
    checkIsInMultiOrPipeline();
    client.rename(oldkey, newkey);
    return client.getStatusCodeReply();
  }

  /**
   * Rename oldkey into newkey but fails if the destination key newkey already exists.
   * <p>
   * Time complexity: O(1)
   * @param oldkey
   * @param newkey
   * @return Integer reply, specifically: 1 if the key was renamed 0 if the target key already exist
   */
  @Override
  public Long renamenx(final byte[] oldkey, final byte[] newkey) {
    checkIsInMultiOrPipeline();
    client.renamenx(oldkey, newkey);
    return client.getIntegerReply();
  }

  /**
   * Return the number of keys in the currently selected database.
   * @return Integer reply
   */
  @Override
  public Long dbSize() {
    checkIsInMultiOrPipeline();
    client.dbSize();
    return client.getIntegerReply();
  }

  /**
   * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
   * the server. A key with an associated timeout is said to be volatile in Redis terminology.
   * <p>
   * Volatile keys are stored on disk like the other keys, the timeout is persistent too like all the
   * other aspects of the dataset. Saving a dataset containing expires and stopping the server does
   * not stop the flow of time as Redis stores on disk the time when the key will no longer be
   * available as Unix time, and not the remaining seconds.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link #persist(byte[]) PERSIST} command.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/expire">Expire Command</a>
   * @param key
   * @param seconds
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
   *         the key already has an associated timeout (this may happen only in Redis versions &lt;
   *         2.1.3, Redis &gt;= 2.1.3 will happily update the timeout), or the key does not exist.
   */
  @Override
  public Long expire(final byte[] key, final int seconds) {
    checkIsInMultiOrPipeline();
    client.expire(key, seconds);
    return client.getIntegerReply();
  }

  /**
   * EXPIREAT works exactly like {@link #expire(byte[], int) EXPIRE} but instead to get the number of
   * seconds representing the Time To Live of the key as a second argument (that is a relative way
   * of specifying the TTL), it takes an absolute one in the form of a UNIX timestamp (Number of
   * seconds elapsed since 1 Gen 1970).
   * <p>
   * EXPIREAT was introduced in order to implement the Append Only File persistence mode so that
   * EXPIRE commands are automatically translated into EXPIREAT commands for the append only file.
   * Of course EXPIREAT can also used by programmers that need a way to simply specify that a given
   * key should expire at a given time in the future.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link #persist(byte[]) PERSIST} command.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/expire">Expire Command</a>
   * @param key
   * @param unixTime
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
   *         the key already has an associated timeout (this may happen only in Redis versions &lt;
   *         2.1.3, Redis &gt;= 2.1.3 will happily update the timeout), or the key does not exist.
   */
  @Override
  public Long expireAt(final byte[] key, final long unixTime) {
    checkIsInMultiOrPipeline();
    client.expireAt(key, unixTime);
    return client.getIntegerReply();
  }

  /**
   * The TTL command returns the remaining time to live in seconds of a key that has an
   * {@link #expire(byte[], int) EXPIRE} set. This introspection capability allows a Redis client to
   * check how many seconds a given key will continue to be part of the dataset.
   * @param key
   * @return Integer reply, returns the remaining time to live in seconds of a key that has an
   *         EXPIRE. If the Key does not exists or does not have an associated expire, -1 is
   *         returned.
   */
  @Override
  public Long ttl(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.ttl(key);
    return client.getIntegerReply();
  }

  /**
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @param keys
   * @return Integer reply: The number of keys that were touched.
   */
  @Override
  public Long touch(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.touch(keys);
    return client.getIntegerReply();
  }

  @Override
  public Long touch(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.touch(key);
    return client.getIntegerReply();
  }

  /**
   * Select the DB with having the specified zero-based numeric index. For default every new client
   * connection is automatically selected to DB 0.
   * @param index
   * @return Status code reply
   */
  @Override
  public String select(final int index) {
    checkIsInMultiOrPipeline();
    client.select(index);
    String statusCodeReply = client.getStatusCodeReply();
    client.setDb(index);

    return statusCodeReply;
  }

  @Override
  public String swapDB(final int index1, final int index2) {
    checkIsInMultiOrPipeline();
    client.swapDB(index1, index2);
    return client.getStatusCodeReply();
  }

  /**
   * Move the specified key from the currently selected DB to the specified destination DB. Note
   * that this command returns 1 only if the key was successfully moved, and 0 if the target key was
   * already there or if the source key was not found at all, so it is possible to use MOVE as a
   * locking primitive.
   * @param key
   * @param dbIndex
   * @return Integer reply, specifically: 1 if the key was moved 0 if the key was not moved because
   *         already present on the target DB or was not found in the current DB.
   */
  @Override
  public Long move(final byte[] key, final int dbIndex) {
    checkIsInMultiOrPipeline();
    client.move(key, dbIndex);
    return client.getIntegerReply();
  }

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one. This
   * command never fails.
   * @return Status code reply
   */
  @Override
  public String flushAll() {
    checkIsInMultiOrPipeline();
    client.flushAll();
    return client.getStatusCodeReply();
  }

  /**
   * GETSET is an atomic set this value and return the old value command. Set key to the string
   * value and return the old value stored at key. The string can't be longer than 1073741824 bytes
   * (1 GB).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return Bulk reply
   */
  @Override
  public byte[] getSet(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.getSet(key, value);
    return client.getBinaryBulkReply();
  }

  /**
   * Get the values of all the specified keys. If one or more keys don't exist or is not of type
   * String, a 'nil' value is returned instead of the value of the specified key, but the operation
   * never fails.
   * <p>
   * Time complexity: O(1) for every key
   * @param keys
   * @return Multi bulk reply
   */
  @Override
  public List<byte[]> mget(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.mget(keys);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * SETNX works exactly like {@link #set(byte[], byte[]) SET} with the only difference that if the
   * key already exists no operation is performed. SETNX actually means "SET if Not eXists".
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
   */
  @Override
  public Long setnx(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.setnx(key, value);
    return client.getIntegerReply();
  }

  /**
   * The command is exactly equivalent to the following group of commands:
   * {@link #set(byte[], byte[]) SET} + {@link #expire(byte[], int) EXPIRE}. The operation is
   * atomic.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param seconds
   * @param value
   * @return Status code reply
   */
  @Override
  public String setex(final byte[] key, final int seconds, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.setex(key, seconds, value);
    return client.getStatusCodeReply();
  }

  /**
   * Set the the respective keys to the respective values. MSET will replace old values with new
   * values, while {@link #msetnx(byte[]...) MSETNX} will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
   * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
   * are modified, another client talking to Redis can either see the changes to both A and B at
   * once, or no modification at all.
   * @see #msetnx(byte[]...)
   * @param keysvalues
   * @return Status code reply Basically +OK as MSET can't fail
   */
  @Override
  public String mset(final byte[]... keysvalues) {
    checkIsInMultiOrPipeline();
    client.mset(keysvalues);
    return client.getStatusCodeReply();
  }

  /**
   * Set the the respective keys to the respective values. {@link #mset(byte[]...) MSET} will
   * replace old values with new values, while MSETNX will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
   * Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
   * are modified, another client talking to Redis can either see the changes to both A and B at
   * once, or no modification at all.
   * @see #mset(byte[]...)
   * @param keysvalues
   * @return Integer reply, specifically: 1 if the all the keys were set 0 if no key was set (at
   *         least one key already existed)
   */
  @Override
  public Long msetnx(final byte[]... keysvalues) {
    checkIsInMultiOrPipeline();
    client.msetnx(keysvalues);
    return client.getIntegerReply();
  }

  /**
   * DECRBY work just like {@link #decr(byte[]) INCR} but instead to decrement by 1 the decrement is
   * integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incr(byte[])
   * @see #decr(byte[])
   * @see #incrBy(byte[], long)
   * @param key
   * @param decrement
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public Long decrBy(final byte[] key, final long decrement) {
    checkIsInMultiOrPipeline();
    client.decrBy(key, decrement);
    return client.getIntegerReply();
  }

  /**
   * Decrement the number stored at key by one. If the key does not exist or contains a value of a
   * wrong type, set the key to the value of "0" before to perform the decrement operation.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incr(byte[])
   * @see #incrBy(byte[], long)
   * @see #decrBy(byte[], long)
   * @param key
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public Long decr(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.decr(key);
    return client.getIntegerReply();
  }

  /**
   * INCRBY work just like {@link #incr(byte[]) INCR} but instead to increment by 1 the increment is
   * integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incr(byte[])
   * @see #decr(byte[])
   * @see #decrBy(byte[], long)
   * @param key
   * @param increment
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public Long incrBy(final byte[] key, final long increment) {
    checkIsInMultiOrPipeline();
    client.incrBy(key, increment);
    return client.getIntegerReply();
  }

  /**
   * INCRBYFLOAT work just like {@link #incrBy(byte[], long)} INCRBY} but increments by floats
   * instead of integers.
   * <p>
   * INCRBYFLOAT commands are limited to double precision floating point values.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "double" types.
   * Simply the string stored at the key is parsed as a base double precision floating point value,
   * incremented, and then converted back as a string. There is no DECRYBYFLOAT but providing a
   * negative value will work as expected.
   * <p>
   * Time complexity: O(1)
   * @see #incr(byte[])
   * @see #decr(byte[])
   * @see #decrBy(byte[], long)
   * @param key the key to increment
   * @param increment the value to increment by
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public Double incrByFloat(final byte[] key, final double increment) {
    checkIsInMultiOrPipeline();
    client.incrByFloat(key, increment);
    String dval = client.getBulkReply();
    return (dval != null ? new Double(dval) : null);
  }

  /**
   * Increment the number stored at key by one. If the key does not exist or contains a value of a
   * wrong type, set the key to the value of "0" before to perform the increment operation.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incrBy(byte[], long)
   * @see #decr(byte[])
   * @see #decrBy(byte[], long)
   * @param key
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public Long incr(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.incr(key);
    return client.getIntegerReply();
  }

  /**
   * If the key already exists and is a string, this command appends the provided value at the end
   * of the string. If the key does not exist it is created and set as an empty string, so APPEND
   * will be very similar to SET in this special case.
   * <p>
   * Time complexity: O(1). The amortized time complexity is O(1) assuming the appended value is
   * small and the already present value is of any size, since the dynamic string library used by
   * Redis will double the free space available on every reallocation.
   * @param key
   * @param value
   * @return Integer reply, specifically the total length of the string after the append operation.
   */
  @Override
  public Long append(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.append(key, value);
    return client.getIntegerReply();
  }

  /**
   * Return a subset of the string from offset start to offset end (both offsets are inclusive).
   * Negative offsets can be used in order to provide an offset starting from the end of the string.
   * So -1 means the last char, -2 the penultimate and so forth.
   * <p>
   * The function handles out of range requests without raising an error, but just limiting the
   * resulting range to the actual length of the string.
   * <p>
   * Time complexity: O(start+n) (with start being the start index and n the total length of the
   * requested range). Note that the lookup part of this command is O(1) so for small strings this
   * is actually an O(1) command.
   * @param key
   * @param start
   * @param end
   * @return Bulk reply
   */
  @Override
  public byte[] substr(final byte[] key, final int start, final int end) {
    checkIsInMultiOrPipeline();
    client.substr(key, start, end);
    return client.getBinaryBulkReply();
  }

  /**
   * Set the specified hash field to the specified value.
   * <p>
   * If key does not exist, a new key holding a hash is created.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @param value
   * @return If the field already exists, and the HSET just produced an update of the value, 0 is
   *         returned, otherwise if a new field is created 1 is returned.
   */
  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.hset(key, field, value);
    return client.getIntegerReply();
  }

  @Override
  public Long hset(final byte[] key, final Map<byte[], byte[]> hash) {
    checkIsInMultiOrPipeline();
    client.hset(key, hash);
    return client.getIntegerReply();
  }

  /**
   * If key holds a hash, retrieve the value associated to the specified field.
   * <p>
   * If the field is not found or the key does not exist, a special 'nil' value is returned.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @return Bulk reply
   */
  @Override
  public byte[] hget(final byte[] key, final byte[] field) {
    checkIsInMultiOrPipeline();
    client.hget(key, field);
    return client.getBinaryBulkReply();
  }

  /**
   * Set the specified hash field to the specified value if the field not exists. <b>Time
   * complexity:</b> O(1)
   * @param key
   * @param field
   * @param value
   * @return If the field already exists, 0 is returned, otherwise if a new field is created 1 is
   *         returned.
   */
  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.hsetnx(key, field, value);
    return client.getIntegerReply();
  }

  /**
   * Set the respective fields to the respective values. HMSET replaces old values with new values.
   * <p>
   * If key does not exist, a new key holding a hash is created.
   * <p>
   * <b>Time complexity:</b> O(N) (with N being the number of fields)
   * @param key
   * @param hash
   * @return Always OK because HMSET can't fail
   */
  @Override
  public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    checkIsInMultiOrPipeline();
    client.hmset(key, hash);
    return client.getStatusCodeReply();
  }

  /**
   * Retrieve the values associated to the specified fields.
   * <p>
   * If some of the specified fields do not exist, nil values are returned. Non existing keys are
   * considered like empty hashes.
   * <p>
   * <b>Time complexity:</b> O(N) (with N being the number of fields)
   * @param key
   * @param fields
   * @return Multi Bulk Reply specifically a list of all the values associated with the specified
   *         fields, in the same order of the request.
   */
  @Override
  public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
    checkIsInMultiOrPipeline();
    client.hmget(key, fields);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Increment the number stored at field in the hash at key by value. If key does not exist, a new
   * key holding a hash is created. If field does not exist or holds a string, the value is set to 0
   * before applying the operation. Since the value argument is signed you can use this command to
   * perform both increments and decrements.
   * <p>
   * The range of values supported by HINCRBY is limited to 64 bit signed integers.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @param value
   * @return Integer reply The new value at field after the increment operation.
   */
  @Override
  public Long hincrBy(final byte[] key, final byte[] field, final long value) {
    checkIsInMultiOrPipeline();
    client.hincrBy(key, field, value);
    return client.getIntegerReply();
  }

  /**
   * Increment the number stored at field in the hash at key by a double precision floating point
   * value. If key does not exist, a new key holding a hash is created. If field does not exist or
   * holds a string, the value is set to 0 before applying the operation. Since the value argument
   * is signed you can use this command to perform both increments and decrements.
   * <p>
   * The range of values supported by HINCRBYFLOAT is limited to double precision floating point
   * values.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @param value
   * @return Double precision floating point reply The new value at field after the increment
   *         operation.
   */
  @Override
  public Double hincrByFloat(final byte[] key, final byte[] field, final double value) {
    checkIsInMultiOrPipeline();
    client.hincrByFloat(key, field, value);
    final String dval = client.getBulkReply();
    return (dval != null ? new Double(dval) : null);
  }

  /**
   * Test for existence of a specified field in a hash. <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @return Return true if the hash stored at key contains the specified field. Return false if the key is
   *         not found or the field is not present.
   */
  @Override
  public Boolean hexists(final byte[] key, final byte[] field) {
    checkIsInMultiOrPipeline();
    client.hexists(key, field);
    return client.getIntegerReply() == 1;
  }

  /**
   * Remove the specified field from an hash stored at key.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param fields
   * @return If the field was present in the hash it is deleted and 1 is returned, otherwise 0 is
   *         returned and no operation is performed.
   */
  @Override
  public Long hdel(final byte[] key, final byte[]... fields) {
    checkIsInMultiOrPipeline();
    client.hdel(key, fields);
    return client.getIntegerReply();
  }

  /**
   * Return the number of items in a hash.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @return The number of entries (fields) contained in the hash stored at key. If the specified
   *         key does not exist, 0 is returned assuming an empty hash.
   */
  @Override
  public Long hlen(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.hlen(key);
    return client.getIntegerReply();
  }

  /**
   * Return all the fields in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields names contained into a hash.
   */
  @Override
  public Set<byte[]> hkeys(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.hkeys(key);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Return all the values in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields values contained into a hash.
   */
  @Override
  public List<byte[]> hvals(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.hvals(key);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Return all the fields and associated values in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields and values contained into a hash.
   */
  @Override
  public Map<byte[], byte[]> hgetAll(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.hgetAll(key);
    final List<byte[]> flatHash = client.getBinaryMultiBulkReply();
    final Map<byte[], byte[]> hash = new JedisByteHashMap();
    final Iterator<byte[]> iterator = flatHash.iterator();
    while (iterator.hasNext()) {
      hash.put(iterator.next(), iterator.next());
    }

    return hash;
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @see BinaryJedis#rpush(byte[], byte[]...)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public Long rpush(final byte[] key, final byte[]... strings) {
    checkIsInMultiOrPipeline();
    client.rpush(key, strings);
    return client.getIntegerReply();
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @see BinaryJedis#rpush(byte[], byte[]...)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public Long lpush(final byte[] key, final byte[]... strings) {
    checkIsInMultiOrPipeline();
    client.lpush(key, strings);
    return client.getIntegerReply();
  }

  /**
   * Return the length of the list stored at the specified key. If the key does not exist zero is
   * returned (the same behaviour as for empty lists). If the value stored at key is not a list an
   * error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return The length of the list.
   */
  @Override
  public Long llen(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.llen(key);
    return client.getIntegerReply();
  }

  /**
   * Return the specified elements of the list stored at the specified key. Start and end are
   * zero-based indexes. 0 is the first element of the list (the list head), 1 the next element and
   * so on.
   * <p>
   * For example LRANGE foobar 0 2 will return the first three elements of the list.
   * <p>
   * start and end can also be negative numbers indicating offsets from the end of the list. For
   * example -1 is the last element of the list, -2 the penultimate element and so on.
   * <p>
   * <b>Consistency with range functions in various programming languages</b>
   * <p>
   * Note that if you have a list of numbers from 0 to 100, LRANGE 0 10 will return 11 elements,
   * that is, rightmost item is included. This may or may not be consistent with behavior of
   * range-related functions in your programming language of choice (think Ruby's Range.new,
   * Array#slice or Python's range() function).
   * <p>
   * LRANGE behavior is consistent with one of Tcl.
   * <p>
   * <b>Out-of-range indexes</b>
   * <p>
   * Indexes out of range will not produce an error: if start is over the end of the list, or start
   * &gt; end, an empty list is returned. If end is over the end of the list Redis will threat it
   * just like the last element of the list.
   * <p>
   * Time complexity: O(start+n) (with n being the length of the range and start being the start
   * offset)
   * @param key
   * @param start
   * @param stop
   * @return Multi bulk reply, specifically a list of elements in the specified range.
   */
  @Override
  public List<byte[]> lrange(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.lrange(key, start, stop);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Trim an existing list so that it will contain only the specified range of elements specified.
   * Start and end are zero-based indexes. 0 is the first element of the list (the list head), 1 the
   * next element and so on.
   * <p>
   * For example LTRIM foobar 0 2 will modify the list stored at foobar key so that only the first
   * three elements of the list will remain.
   * <p>
   * start and end can also be negative numbers indicating offsets from the end of the list. For
   * example -1 is the last element of the list, -2 the penultimate element and so on.
   * <p>
   * Indexes out of range will not produce an error: if start is over the end of the list, or start
   * &gt; end, an empty list is left as value. If end over the end of the list Redis will threat it
   * just like the last element of the list.
   * <p>
   * Hint: the obvious use of LTRIM is together with LPUSH/RPUSH. For example:
   * <p>
   * {@code lpush("mylist", "someelement"); ltrim("mylist", 0, 99); * }
   * <p>
   * The above two commands will push elements in the list taking care that the list will not grow
   * without limits. This is very useful when using Redis to store logs for example. It is important
   * to note that when used in this way LTRIM is an O(1) operation because in the average case just
   * one element is removed from the tail of the list.
   * <p>
   * Time complexity: O(n) (with n being len of list - len of range)
   * @param key
   * @param start
   * @param stop
   * @return Status code reply
   */
  @Override
  public String ltrim(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.ltrim(key, start, stop);
    return client.getStatusCodeReply();
  }

  /**
   * Return the specified element of the list stored at the specified key. 0 is the first element, 1
   * the second and so on. Negative indexes are supported, for example -1 is the last element, -2
   * the penultimate and so on.
   * <p>
   * If the value stored at key is not of list type an error is returned. If the index is out of
   * range a 'nil' reply is returned.
   * <p>
   * Note that even if the average time complexity is O(n) asking for the first or the last element
   * of the list is O(1).
   * <p>
   * Time complexity: O(n) (with n being the length of the list)
   * @param key
   * @param index
   * @return Bulk reply, specifically the requested element
   */
  @Override
  public byte[] lindex(final byte[] key, final long index) {
    checkIsInMultiOrPipeline();
    client.lindex(key, index);
    return client.getBinaryBulkReply();
  }

  /**
   * Set a new value as the element at index position of the List at key.
   * <p>
   * Out of range indexes will generate an error.
   * <p>
   * Similarly to other list commands accepting indexes, the index can be negative to access
   * elements starting from the end of the list. So -1 is the last element, -2 is the penultimate,
   * and so forth.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(N) (with N being the length of the list), setting the first or last elements of the list is
   * O(1).
   * @see #lindex(byte[], long)
   * @param key
   * @param index
   * @param value
   * @return Status code reply
   */
  @Override
  public String lset(final byte[] key, final long index, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.lset(key, index, value);
    return client.getStatusCodeReply();
  }

  /**
   * Remove the first count occurrences of the value element from the list. If count is zero all the
   * elements are removed. If count is negative elements are removed from tail to head, instead to
   * go from head to tail that is the normal behaviour. So for example LREM with count -2 and hello
   * as value to remove against the list (a,b,c,hello,x,hello,hello) will leave the list
   * (a,b,c,hello,x). The number of removed elements is returned as an integer, see below for more
   * information about the returned value. Note that non existing keys are considered like empty
   * lists by LREM, so LREM against non existing keys will always return 0.
   * <p>
   * Time complexity: O(N) (with N being the length of the list)
   * @param key
   * @param count
   * @param value
   * @return Integer Reply, specifically: The number of removed elements if the operation succeeded
   */
  @Override
  public Long lrem(final byte[] key, final long count, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.lrem(key, count, value);
    return client.getIntegerReply();
  }

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned.
   * @see #rpop(byte[])
   * @param key
   * @return Bulk reply
   */
  @Override
  public byte[] lpop(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.lpop(key);
    return client.getBinaryBulkReply();
  }

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned.
   * @see #lpop(byte[])
   * @param key
   * @return Bulk reply
   */
  @Override
  public byte[] rpop(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.rpop(key);
    return client.getBinaryBulkReply();
  }

  /**
   * Atomically return and remove the last (tail) element of the srckey list, and push the element
   * as the first (head) element of the dstkey list. For example if the source list contains the
   * elements "a","b","c" and the destination list contains the elements "foo","bar" after an
   * RPOPLPUSH command the content of the two lists will be "a","b" and "c","foo","bar".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned. If
   * the srckey and dstkey are the same the operation is equivalent to removing the last element
   * from the list and pushing it as first element of the list, so it's a "list rotation" command.
   * <p>
   * Time complexity: O(1)
   * @param srckey
   * @param dstkey
   * @return Bulk reply
   */
  @Override
  public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
    checkIsInMultiOrPipeline();
    client.rpoplpush(srckey, dstkey);
    return client.getBinaryBulkReply();
  }

  /**
   * Add the specified member to the set value stored at key. If member is already a member of the
   * set no operation is performed. If key does not exist a new set with the specified member as
   * sole member is created. If the key exists but does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
   *         already a member of the set
   */
  @Override
  public Long sadd(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    client.sadd(key, members);
    return client.getIntegerReply();
  }

  /**
   * Return all the members (elements) of the set value stored at key. This is just syntax glue for
   * {@link #sinter(byte[]...)} SINTER}.
   * <p>
   * Time complexity O(N)
   * @param key the key of the set
   * @return Multi bulk reply
   */
  @Override
  public Set<byte[]> smembers(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.smembers(key);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Remove the specified member from the set value stored at key. If member was not a member of the
   * set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key the key of the set
   * @param member the set member to remove
   * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
   *         not a member of the set
   */
  @Override
  public Long srem(final byte[] key, final byte[]... member) {
    checkIsInMultiOrPipeline();
    client.srem(key, member);
    return client.getIntegerReply();
  }

  /**
   * Remove a random element from a Set returning it as return value. If the Set is empty or the key
   * does not exist, a nil object is returned.
   * <p>
   * The {@link #srandmember(byte[])} command does a similar work but the returned element is not
   * removed from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return Bulk reply
   */
  @Override
  public byte[] spop(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.spop(key);
    return client.getBinaryBulkReply();
  }

  @Override
  public Set<byte[]> spop(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    client.spop(key, count);
    List<byte[]> members = client.getBinaryMultiBulkReply();
    if (members == null) return null;
    return SetFromList.of(members);
  }

  /**
   * Move the specified member from the set at srckey to the set at dstkey. This operation is
   * atomic, in every given moment the element will appear to be in the source or destination set
   * for accessing clients.
   * <p>
   * If the source set does not exist or does not contain the specified element no operation is
   * performed and zero is returned, otherwise the element is removed from the source set and added
   * to the destination set. On success one is returned, even if the element was already present in
   * the destination set.
   * <p>
   * An error is raised if the source or destination keys contain a non Set value.
   * <p>
   * Time complexity O(1)
   * @param srckey
   * @param dstkey
   * @param member
   * @return Integer reply, specifically: 1 if the element was moved 0 if the element was not found
   *         on the first set and no operation was performed
   */
  @Override
  public Long smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.smove(srckey, dstkey, member);
    return client.getIntegerReply();
  }

  /**
   * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
   * for empty sets.
   * @param key
   * @return Integer reply, specifically: the cardinality (number of elements) of the set as an
   *         integer.
   */
  @Override
  public Long scard(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.scard(key);
    return client.getIntegerReply();
  }

  /**
   * Return true if member is a member of the set stored at key, otherwise false is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param member
   * @return Boolean reply, specifically: true if the element is a member of the set false if the element
   *         is not a member of the set OR if the key does not exist
   */
  @Override
  public Boolean sismember(final byte[] key, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.sismember(key, member);
    return client.getIntegerReply() == 1;
  }

  /**
   * Return the members of a set resulting from the intersection of all the sets hold at the
   * specified keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the
   * client as a multi-bulk reply (see the protocol specification for more information). If just a
   * single key is specified, then this command produces the same result as
   * {@link #smembers(byte[]) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
   * <p>
   * Non existing keys are considered like empty sets, so if one of the keys is missing an empty set
   * is returned (since the intersection with an empty set always is an empty set).
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param keys
   * @return Multi bulk reply, specifically the list of common elements.
   */
  @Override
  public Set<byte[]> sinter(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sinter(keys);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * This commanad works exactly like {@link #sinter(byte[]...) SINTER} but instead of being returned
   * the resulting set is stored as dstkey.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public Long sinterstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sinterstore(dstkey, keys);
    return client.getIntegerReply();
  }

  /**
   * Return the members of a set resulting from the union of all the sets hold at the specified
   * keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the client as a
   * multi-bulk reply (see the protocol specification for more information). If just a single key is
   * specified, then this command produces the same result as {@link #smembers(byte[]) SMEMBERS}.
   * <p>
   * Non existing keys are considered like empty sets.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys
   * @return Multi bulk reply, specifically the list of common elements.
   */
  @Override
  public Set<byte[]> sunion(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sunion(keys);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * This command works exactly like {@link #sunion(byte[]...) SUNION} but instead of being returned
   * the resulting set is stored as dstkey. Any existing value in dstkey will be over-written.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public Long sunionstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sunionstore(dstkey, keys);
    return client.getIntegerReply();
  }

  /**
   * Return the difference between the Set stored at key1 and all the Sets key2, ..., keyN
   * <p>
   * <b>Example:</b>
   * 
   * <pre>
   * key1 = [x, a, b, c]
   * key2 = [c]
   * key3 = [a, d]
   * SDIFF key1,key2,key3 =&gt; [x, b]
   * </pre>
   * 
   * Non existing keys are considered like empty sets.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(N) with N being the total number of elements of all the sets
   * @param keys
   * @return Return the members of a set resulting from the difference between the first set
   *         provided and all the successive sets.
   */
  @Override
  public Set<byte[]> sdiff(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sdiff(keys);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * This command works exactly like {@link #sdiff(byte[]...) SDIFF} but instead of being returned
   * the resulting set is stored in dstkey.
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public Long sdiffstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.sdiffstore(dstkey, keys);
    return client.getIntegerReply();
  }

  /**
   * Return a random element from a Set, without removing the element. If the Set is empty or the
   * key does not exist, a nil object is returned.
   * <p>
   * The SPOP command does a similar work but the returned element is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return Bulk reply
   */
  @Override
  public byte[] srandmember(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.srandmember(key);
    return client.getBinaryBulkReply();
  }

  @Override
  public List<byte[]> srandmember(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    client.srandmember(key, count);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Add the specified member having the specified score to the sorted set stored at key. If member
   * is already a member of the sorted set the score is updated, and the element reinserted in the
   * right position to ensure sorting. If key does not exist a new sorted set with the specified
   * member as sole member is created. If the key exists but does not hold a sorted set value an
   * error is returned.
   * <p>
   * The score value can be the string representation of a double precision floating point number.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param score
   * @param member
   * @return Integer reply, specifically: 1 if the new element was added 0 if the element was
   *         already a member of the sorted set and the score was updated
   */
  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.zadd(key, score, member);
    return client.getIntegerReply();
  }

  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    client.zadd(key, score, member, params);
    return client.getIntegerReply();
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    checkIsInMultiOrPipeline();
    client.zadd(key, scoreMembers);
    return client.getIntegerReply();
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    client.zadd(key, scoreMembers, params);
    return client.getIntegerReply();
  }

  @Override
  public Set<byte[]> zrange(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.zrange(key, start, stop);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Remove the specified member from the sorted set value stored at key. If member was not a member
   * of the set no operation is performed. If key does not not hold a set value an error is
   * returned.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
   *         not a member of the set
   */
  @Override
  public Long zrem(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    client.zrem(key, members);
    return client.getIntegerReply();
  }

  /**
   * If member already exists in the sorted set adds the increment to its score and updates the
   * position of the element in the sorted set accordingly. If member does not already exist in the
   * sorted set it is added with increment as score (that is, like if the previous score was
   * virtually zero). If key does not exist a new sorted set with the specified member as sole
   * member is created. If the key exists but does not hold a sorted set value an error is returned.
   * <p>
   * The score value can be the string representation of a double precision floating point number.
   * It's possible to provide a negative value to perform a decrement.
   * <p>
   * For an introduction to sorted sets check the Introduction to Redis data types page.
   * <p>
   * Time complexity O(log(N)) with N being the number of elements in the sorted set
   * @param key
   * @param increment
   * @param member
   * @return The new score
   */
  @Override
  public Double zincrby(final byte[] key, final double increment, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.zincrby(key, increment, member);
    return BuilderFactory.DOUBLE.build(client.getOne());
  }

  @Override
  public Double zincrby(final byte[] key, final double increment, final byte[] member, final ZIncrByParams params) {
    checkIsInMultiOrPipeline();
    client.zincrby(key, increment, member, params);
    return BuilderFactory.DOUBLE.build(client.getOne());
  }

  /**
   * Return the rank (or index) or member in the sorted set at key, with scores being ordered from
   * low to high.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))
   * @see #zrevrank(byte[], byte[])
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  @Override
  public Long zrank(final byte[] key, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.zrank(key, member);
    return client.getIntegerReply();
  }

  /**
   * Return the rank (or index) or member in the sorted set at key, with scores being ordered from
   * high to low.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))
   * @see #zrank(byte[], byte[])
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  @Override
  public Long zrevrank(final byte[] key, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.zrevrank(key, member);
    return client.getIntegerReply();
  }

  @Override
  public Set<byte[]> zrevrange(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.zrevrange(key, start, stop);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<Tuple> zrangeWithScores(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.zrangeWithScores(key, start, stop);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.zrevrangeWithScores(key, start, stop);
    return getTupledSet();
  }

  /**
   * Return the sorted set cardinality (number of elements). If the key does not exist 0 is
   * returned, like for empty sorted sets.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return the cardinality (number of elements) of the set as an integer.
   */
  @Override
  public Long zcard(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.zcard(key);
    return client.getIntegerReply();
  }

  /**
   * Return the score of the specified element of the sorted set at key. If the specified element
   * does not exist in the sorted set, or the key does not exist at all, a special 'nil' value is
   * returned.
   * <p>
   * <b>Time complexity:</b> O(1)
   * @param key
   * @param member
   * @return the score
   */
  @Override
  public Double zscore(final byte[] key, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.zscore(key, member);
    final String score = client.getBulkReply();
    return (score != null ? new Double(score) : null);
  }

  @Override
  public Tuple zpopmax(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.zpopmax(key);
    return BuilderFactory.TUPLE.build(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<Tuple> zpopmax(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    client.zpopmax(key, count);
    return getTupledSet();
  }

  @Override
  public Tuple zpopmin(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.zpopmin(key);
    return BuilderFactory.TUPLE.build(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<Tuple> zpopmin(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    client.zpopmin(key, count);
    return getTupledSet();
  }

  public Transaction multi() {
    client.multi();
    client.getOne(); // expected OK
    transaction = new Transaction(client);
    return transaction;
  }

  protected void checkIsInMultiOrPipeline() {
    if (client.isInMulti()) {
      throw new JedisDataException(
          "Cannot use Jedis when in Multi. Please use Transaction or reset jedis state.");
    } else if (pipeline != null && pipeline.hasPipelinedResponse()) {
      throw new JedisDataException(
          "Cannot use Jedis when in Pipeline. Please use Pipeline or reset jedis state .");
    }
  }

  public void connect() {
    client.connect();
  }

  public void disconnect() {
    client.disconnect();
  }

  public void resetState() {
    if (client.isConnected()) {
      if (transaction != null) {
        transaction.close();
      }

      if (pipeline != null) {
        pipeline.close();
      }

      client.resetState();
    }

    transaction = null;
    pipeline = null;
  }

  @Override
  public String watch(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.watch(keys);
    return client.getStatusCodeReply();
  }

  @Override
  public String unwatch() {
    checkIsInMultiOrPipeline();
    client.unwatch();
    return client.getStatusCodeReply();
  }

  @Override
  public void close() {
    client.close();
  }

  /**
   * Sort a Set or a List.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set value at key. By default sorting is
   * numeric with elements being compared as double precision floating point numbers. This is the
   * simplest form of SORT.
   * @see #sort(byte[], byte[])
   * @see #sort(byte[], SortingParams)
   * @see #sort(byte[], SortingParams, byte[])
   * @param key
   * @return Assuming the Set/List at key contains a list of numbers, the return value will be the
   *         list of numbers ordered from the smallest to the biggest number.
   */
  @Override
  public List<byte[]> sort(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.sort(key);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Sort a Set or a List accordingly to the specified parameters.
   * <p>
   * <b>examples:</b>
   * <p>
   * Given are the following sets and key/values:
   * 
   * <pre>
   * x = [1, 2, 3]
   * y = [a, b, c]
   * 
   * k1 = z
   * k2 = y
   * k3 = x
   * 
   * w1 = 9
   * w2 = 8
   * w3 = 7
   * </pre>
   * 
   * Sort Order:
   * 
   * <pre>
   * sort(x) or sort(x, sp.asc())
   * -&gt; [1, 2, 3]
   * 
   * sort(x, sp.desc())
   * -&gt; [3, 2, 1]
   * 
   * sort(y)
   * -&gt; [c, a, b]
   * 
   * sort(y, sp.alpha())
   * -&gt; [a, b, c]
   * 
   * sort(y, sp.alpha().desc())
   * -&gt; [c, a, b]
   * </pre>
   * 
   * Limit (e.g. for Pagination):
   * 
   * <pre>
   * sort(x, sp.limit(0, 2))
   * -&gt; [1, 2]
   * 
   * sort(y, sp.alpha().desc().limit(1, 2))
   * -&gt; [b, a]
   * </pre>
   * 
   * Sorting by external keys:
   * 
   * <pre>
   * sort(x, sb.by(w*))
   * -&gt; [3, 2, 1]
   * 
   * sort(x, sb.by(w*).desc())
   * -&gt; [1, 2, 3]
   * </pre>
   * 
   * Getting external keys:
   * 
   * <pre>
   * sort(x, sp.by(w*).get(k*))
   * -&gt; [x, y, z]
   * 
   * sort(x, sp.by(w*).get(#).get(k*))
   * -&gt; [3, x, 2, y, 1, z]
   * </pre>
   * @see #sort(byte[])
   * @see #sort(byte[], SortingParams, byte[])
   * @param key
   * @param sortingParameters
   * @return a list of sorted elements.
   */
  @Override
  public List<byte[]> sort(final byte[] key, final SortingParams sortingParameters) {
    checkIsInMultiOrPipeline();
    client.sort(key, sortingParameters);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this commands as blocking
   * versions of LPOP and RPOP able to block if the specified keys don't exist or contain empty
   * lists.
   * <p>
   * The following is a description of the exact semantic. We describe BLPOP but the two commands
   * are identical, the only difference is that BLPOP pops the element from the left (head) of the
   * list, and BRPOP pops from the right (tail).
   * <p>
   * <b>Non blocking behavior</b>
   * <p>
   * When BLPOP is called, if at least one of the specified keys contain a non empty list, an
   * element is popped from the head of the list and returned to the caller together with the name
   * of the key (BLPOP returns a two elements array, the first element is the key, the second the
   * popped value).
   * <p>
   * Keys are scanned from left to right, so for instance if you issue BLPOP list1 list2 list3 0
   * against a dataset where list1 does not exist but list2 and list3 contain non empty lists, BLPOP
   * guarantees to return an element from the list stored at list2 (since it is the first non empty
   * list starting from the left).
   * <p>
   * <b>Blocking behavior</b>
   * <p>
   * If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
   * client performs a LPUSH or an RPUSH operation against one of the lists.
   * <p>
   * Once new data is present on one of the lists, the client finally returns with the name of the
   * key unblocking it and the popped value.
   * <p>
   * When blocking, if a non-zero timeout is specified, the client will unblock returning a nil
   * special value if the specified amount of seconds passed without a push operation against at
   * least one of the specified keys.
   * <p>
   * The timeout argument is interpreted as an integer value. A timeout of zero means instead to
   * block forever.
   * <p>
   * <b>Multiple clients blocking for the same keys</b>
   * <p>
   * Multiple clients can block for the same key. They are put into a queue, so the first to be
   * served will be the one that started to wait earlier, in a first-blpopping first-served fashion.
   * <p>
   * <b>blocking POP inside a MULTI/EXEC transaction</b>
   * <p>
   * BLPOP and BRPOP can be used with pipelining (sending multiple commands and reading the replies
   * in batch), but it does not make sense to use BLPOP or BRPOP inside a MULTI/EXEC block (a Redis
   * transaction).
   * <p>
   * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to return a multi-bulk nil
   * reply, exactly what happens when the timeout is reached. If you like science fiction, think at
   * it like if inside MULTI/EXEC the time will flow at infinite speed :)
   * <p>
   * Time complexity: O(1)
   * @see #brpop(int, byte[]...)
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
   *         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
   *         value is a nil multi bulk reply. Most client values will return false or nil
   *         accordingly to the programming language used.
   */
  @Override
  public List<byte[]> blpop(final int timeout, final byte[]... keys) {
    return blpop(getArgsAddTimeout(timeout, keys));
  }

  private byte[][] getArgsAddTimeout(int timeout, byte[][] keys) {
    int size = keys.length;
    final byte[][] args = new byte[size + 1][];
    for (int at = 0; at != size; ++at) {
      args[at] = keys[at];
    }
    args[size] = Protocol.toByteArray(timeout);
    return args;
  }

  /**
   * Sort a Set or a List accordingly to the specified parameters and store the result at dstkey.
   * @see #sort(byte[], SortingParams)
   * @see #sort(byte[])
   * @see #sort(byte[], byte[])
   * @param key
   * @param sortingParameters
   * @param dstkey
   * @return The number of elements of the list at dstkey.
   */
  @Override
  public Long sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    checkIsInMultiOrPipeline();
    client.sort(key, sortingParameters, dstkey);
    return client.getIntegerReply();
  }

  /**
   * Sort a Set or a List and Store the Result at dstkey.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set value at key and store the result
   * at dstkey. By default sorting is numeric with elements being compared as double precision
   * floating point numbers. This is the simplest form of SORT.
   * @see #sort(byte[])
   * @see #sort(byte[], SortingParams)
   * @see #sort(byte[], SortingParams, byte[])
   * @param key
   * @param dstkey
   * @return The number of elements of the list at dstkey.
   */
  @Override
  public Long sort(final byte[] key, final byte[] dstkey) {
    checkIsInMultiOrPipeline();
    client.sort(key, dstkey);
    return client.getIntegerReply();
  }

  /**
   * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this commands as blocking
   * versions of LPOP and RPOP able to block if the specified keys don't exist or contain empty
   * lists.
   * <p>
   * The following is a description of the exact semantic. We describe BLPOP but the two commands
   * are identical, the only difference is that BLPOP pops the element from the left (head) of the
   * list, and BRPOP pops from the right (tail).
   * <p>
   * <b>Non blocking behavior</b>
   * <p>
   * When BLPOP is called, if at least one of the specified keys contain a non empty list, an
   * element is popped from the head of the list and returned to the caller together with the name
   * of the key (BLPOP returns a two elements array, the first element is the key, the second the
   * popped value).
   * <p>
   * Keys are scanned from left to right, so for instance if you issue BLPOP list1 list2 list3 0
   * against a dataset where list1 does not exist but list2 and list3 contain non empty lists, BLPOP
   * guarantees to return an element from the list stored at list2 (since it is the first non empty
   * list starting from the left).
   * <p>
   * <b>Blocking behavior</b>
   * <p>
   * If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
   * client performs a LPUSH or an RPUSH operation against one of the lists.
   * <p>
   * Once new data is present on one of the lists, the client finally returns with the name of the
   * key unblocking it and the popped value.
   * <p>
   * When blocking, if a non-zero timeout is specified, the client will unblock returning a nil
   * special value if the specified amount of seconds passed without a push operation against at
   * least one of the specified keys.
   * <p>
   * The timeout argument is interpreted as an integer value. A timeout of zero means instead to
   * block forever.
   * <p>
   * <b>Multiple clients blocking for the same keys</b>
   * <p>
   * Multiple clients can block for the same key. They are put into a queue, so the first to be
   * served will be the one that started to wait earlier, in a first-blpopping first-served fashion.
   * <p>
   * <b>blocking POP inside a MULTI/EXEC transaction</b>
   * <p>
   * BLPOP and BRPOP can be used with pipelining (sending multiple commands and reading the replies
   * in batch), but it does not make sense to use BLPOP or BRPOP inside a MULTI/EXEC block (a Redis
   * transaction).
   * <p>
   * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to return a multi-bulk nil
   * reply, exactly what happens when the timeout is reached. If you like science fiction, think at
   * it like if inside MULTI/EXEC the time will flow at infinite speed :)
   * <p>
   * Time complexity: O(1)
   * @see #blpop(int, byte[]...)
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
   *         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
   *         value is a nil multi bulk reply. Most client values will return false or nil
   *         accordingly to the programming language used.
   */
  @Override
  public List<byte[]> brpop(final int timeout, final byte[]... keys) {
    return brpop(getArgsAddTimeout(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(final byte[]... args) {
    checkIsInMultiOrPipeline();
    client.blpop(args);
    client.setTimeoutInfinite();
    try {
      return client.getBinaryMultiBulkReply();
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public List<byte[]> brpop(final byte[]... args) {
    checkIsInMultiOrPipeline();
    client.brpop(args);
    client.setTimeoutInfinite();
    try {
      return client.getBinaryMultiBulkReply();
    } finally {
      client.rollbackTimeout();
    }
  }

  /**
   * Request for authentication in a password protected Redis server. A Redis server can be
   * instructed to require a password before to allow clients to issue commands. This is done using
   * the requirepass directive in the Redis configuration file. If the password given by the client
   * is correct the server replies with an OK status code reply and starts accepting commands from
   * the client. Otherwise an error is returned and the clients needs to try a new password. Note
   * that for the high performance nature of Redis it is possible to try a lot of passwords in
   * parallel in very short time, so make sure to generate a strong and very long password so that
   * this attack is infeasible.
   * @param password
   * @return Status code reply
   */
  @Override
  public String auth(final String password) {
    checkIsInMultiOrPipeline();
    client.auth(password);
    return client.getStatusCodeReply();
  }

  /**
   * Request for authentication with a Redis Server that is using ACL where user are authenticated with
   * username and password.
   * See https://redis.io/topics/acl
   * @param user
   * @param password
   * @return
   */
  @Override
  public String auth(final String user, final String password) {
    checkIsInMultiOrPipeline();
    client.auth(user, password);
    return client.getStatusCodeReply();
  }

  public Pipeline pipelined() {
    pipeline = new Pipeline();
    pipeline.setClient(client);
    return pipeline;
  }

  @Override
  public Long zcount(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    client.zcount(key, min, max);
    return client.getIntegerReply();
  }

  @Override
  public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zcount(key, min, max);
    return client.getIntegerReply();
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
   * actual elements in the specified interval, it just returns the number of matching elements.
   * <p>
   * <b>Exclusive intervals and infinity</b>
   * <p>
   * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
   * smallest element in order to take, for instance, elements "up to a given value".
   * <p>
   * Also while the interval is for default closed (inclusive) it's possible to specify open
   * intervals prefixing the score with a "(" character, so for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (1.3 5}
   * <p>
   * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (5 (10}
   * <p>
   * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
   * elements returned by the command, so if M is constant (for instance you always ask for the
   * first ten elements with LIMIT) you can consider it O(log(N))
   * @see #zrangeByScore(byte[], double, double)
   * @see #zrangeByScore(byte[], double, double, int, int)
   * @see #zrangeByScoreWithScores(byte[], double, double)
   * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
   * @see #zcount(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    client.zrangeByScore(key, min, max);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zrangeByScore(key, min, max);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
   * actual elements in the specified interval, it just returns the number of matching elements.
   * <p>
   * <b>Exclusive intervals and infinity</b>
   * <p>
   * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
   * smallest element in order to take, for instance, elements "up to a given value".
   * <p>
   * Also while the interval is for default closed (inclusive) it's possible to specify open
   * intervals prefixing the score with a "(" character, so for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (1.3 5}
   * <p>
   * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (5 (10}
   * <p>
   * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
   * elements returned by the command, so if M is constant (for instance you always ask for the
   * first ten elements with LIMIT) you can consider it O(log(N))
   * @see #zrangeByScore(byte[], double, double)
   * @see #zrangeByScore(byte[], double, double, int, int)
   * @see #zrangeByScoreWithScores(byte[], double, double)
   * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
   * @see #zcount(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrangeByScore(key, min, max, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrangeByScore(key, min, max, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
   * actual elements in the specified interval, it just returns the number of matching elements.
   * <p>
   * <b>Exclusive intervals and infinity</b>
   * <p>
   * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
   * smallest element in order to take, for instance, elements "up to a given value".
   * <p>
   * Also while the interval is for default closed (inclusive) it's possible to specify open
   * intervals prefixing the score with a "(" character, so for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (1.3 5}
   * <p>
   * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (5 (10}
   * <p>
   * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
   * elements returned by the command, so if M is constant (for instance you always ask for the
   * first ten elements with LIMIT) you can consider it O(log(N))
   * @see #zrangeByScore(byte[], double, double)
   * @see #zrangeByScore(byte[], double, double, int, int)
   * @see #zrangeByScoreWithScores(byte[], double, double)
   * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
   * @see #zcount(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    client.zrangeByScoreWithScores(key, min, max);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zrangeByScoreWithScores(key, min, max);
    return getTupledSet();
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(byte[], double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(byte[], double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(byte[], double, double) ZRANGEBYSCORE} but instead of returning the
   * actual elements in the specified interval, it just returns the number of matching elements.
   * <p>
   * <b>Exclusive intervals and infinity</b>
   * <p>
   * min and max can be -inf and +inf, so that you are not required to know what's the greatest or
   * smallest element in order to take, for instance, elements "up to a given value".
   * <p>
   * Also while the interval is for default closed (inclusive) it's possible to specify open
   * intervals prefixing the score with a "(" character, so for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (1.3 5}
   * <p>
   * Will return all the values with score &gt; 1.3 and &lt;= 5, while for instance:
   * <p>
   * {@code ZRANGEBYSCORE zset (5 (10}
   * <p>
   * Will return all the values with score &gt; 5 and &lt; 10 (5 and 10 excluded).
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
   * elements returned by the command, so if M is constant (for instance you always ask for the
   * first ten elements with LIMIT) you can consider it O(log(N))
   * @see #zrangeByScore(byte[], double, double)
   * @see #zrangeByScore(byte[], double, double, int, int)
   * @see #zrangeByScoreWithScores(byte[], double, double)
   * @see #zrangeByScoreWithScores(byte[], double, double, int, int)
   * @see #zcount(byte[], double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrangeByScoreWithScores(key, min, max, offset, count);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrangeByScoreWithScores(key, min, max, offset, count);
    return getTupledSet();
  }

  protected Set<Tuple> getTupledSet() {
    List<byte[]> membersWithScores = client.getBinaryMultiBulkReply();
    if (membersWithScores.isEmpty()) {
      return Collections.emptySet();
    }
    Set<Tuple> set = new LinkedHashSet<>(membersWithScores.size() / 2, 1.0f);
    Iterator<byte[]> iterator = membersWithScores.iterator();
    while (iterator.hasNext()) {
      set.add(new Tuple(iterator.next(), BuilderFactory.DOUBLE.build(iterator.next())));
    }
    return set;
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScore(key, max, min);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScore(key, max, min);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScore(key, max, min, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScore(key, max, min, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScoreWithScores(key, max, min);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max,
      final double min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScoreWithScores(key, max, min);
    return getTupledSet();
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max,
      final byte[] min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getTupledSet();
  }

  /**
   * Remove all elements in the sorted set at key with rank between start and end. Start and end are
   * 0-based with rank 0 being the element with the lowest score. Both start and end can be negative
   * numbers, where they indicate offsets starting at the element with the highest rank. For
   * example: -1 is the element with the highest score, -2 the element with the second highest score
   * and so forth.
   * <p>
   * <b>Time complexity:</b> O(log(N))+O(M) with N being the number of elements in the sorted set
   * and M the number of elements removed by the operation
   * @param key
   * @param start
   * @param stop
   * @return
   */
  @Override
  public Long zremrangeByRank(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    client.zremrangeByRank(key, start, stop);
    return client.getIntegerReply();
  }

  /**
   * Remove all the elements in the sorted set at key with a score between min and max (including
   * elements with score equal to min or max).
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))+O(M) with N being the number of elements in the sorted set and M the number of
   * elements removed by the operation
   * @param key
   * @param min
   * @param max
   * @return Integer reply, specifically the number of elements removed.
   */
  @Override
  public Long zremrangeByScore(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    client.zremrangeByScore(key, min, max);
    return client.getIntegerReply();
  }

  @Override
  public Long zremrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zremrangeByScore(key, min, max);
    return client.getIntegerReply();
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(byte[], byte[]...)} ZINTERSTORE} command requires
   * an element to be present in each of the given inputs to be inserted in the result. The {@link
   * #zunionstore(byte[], byte[]...)} command inserts all elements across all inputs.
   * <p>
   * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
   * that the score of each element in the sorted set is first multiplied by this weight before
   * being passed to the aggregation. When this option is not given, all weights default to 1.
   * <p>
   * With the AGGREGATE option, it's possible to specify how the results of the union or
   * intersection are aggregated. This option defaults to SUM, where the score of an element is
   * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
   * resulting set will contain the minimum or maximum score of an element across the inputs where
   * it exists.
   * <p>
   * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
   * sorted sets, and M being the number of elements in the resulting sorted set
   * @see #zunionstore(byte[], byte[]...)
   * @see #zunionstore(byte[], ZParams, byte[]...)
   * @see #zinterstore(byte[], byte[]...)
   * @see #zinterstore(byte[], ZParams, byte[]...)
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public Long zunionstore(final byte[] dstkey, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    client.zunionstore(dstkey, sets);
    return client.getIntegerReply();
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The {@link
   * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
   * <p>
   * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
   * that the score of each element in the sorted set is first multiplied by this weight before
   * being passed to the aggregation. When this option is not given, all weights default to 1.
   * <p>
   * With the AGGREGATE option, it's possible to specify how the results of the union or
   * intersection are aggregated. This option defaults to SUM, where the score of an element is
   * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
   * resulting set will contain the minimum or maximum score of an element across the inputs where
   * it exists.
   * <p>
   * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
   * sorted sets, and M being the number of elements in the resulting sorted set
   * @see #zunionstore(byte[], byte[]...)
   * @see #zunionstore(byte[], ZParams, byte[]...)
   * @see #zinterstore(byte[], byte[]...)
   * @see #zinterstore(byte[], ZParams, byte[]...)
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public Long zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    client.zunionstore(dstkey, params, sets);
    return client.getIntegerReply();
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The {@link
   * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
   * <p>
   * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
   * that the score of each element in the sorted set is first multiplied by this weight before
   * being passed to the aggregation. When this option is not given, all weights default to 1.
   * <p>
   * With the AGGREGATE option, it's possible to specify how the results of the union or
   * intersection are aggregated. This option defaults to SUM, where the score of an element is
   * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
   * resulting set will contain the minimum or maximum score of an element across the inputs where
   * it exists.
   * <p>
   * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
   * sorted sets, and M being the number of elements in the resulting sorted set
   * @see #zunionstore(byte[], byte[]...)
   * @see #zunionstore(byte[], ZParams, byte[]...)
   * @see #zinterstore(byte[], byte[]...)
   * @see #zinterstore(byte[], ZParams, byte[]...)
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public Long zinterstore(final byte[] dstkey, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    client.zinterstore(dstkey, sets);
    return client.getIntegerReply();
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(byte[], byte[]...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The {@link
   * #zunionstore(byte[], byte[]...) ZUNIONSTORE} command inserts all elements across all inputs.
   * <p>
   * Using the WEIGHTS option, it is possible to add weight to each input sorted set. This means
   * that the score of each element in the sorted set is first multiplied by this weight before
   * being passed to the aggregation. When this option is not given, all weights default to 1.
   * <p>
   * With the AGGREGATE option, it's possible to specify how the results of the union or
   * intersection are aggregated. This option defaults to SUM, where the score of an element is
   * summed across the inputs where it exists. When this option is set to be either MIN or MAX, the
   * resulting set will contain the minimum or maximum score of an element across the inputs where
   * it exists.
   * <p>
   * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the sizes of the input
   * sorted sets, and M being the number of elements in the resulting sorted set
   * @see #zunionstore(byte[], byte[]...)
   * @see #zunionstore(byte[], ZParams, byte[]...)
   * @see #zinterstore(byte[], byte[]...)
   * @see #zinterstore(byte[], ZParams, byte[]...)
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public Long zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    client.zinterstore(dstkey, params, sets);
    return client.getIntegerReply();
  }

  @Override
  public Long zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zlexcount(key, min, max);
    return client.getIntegerReply();
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zrangeByLex(key, min, max);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrangeByLex(key, min, max, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByLex(key, max, min);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    client.zrevrangeByLex(key, max, min, offset, count);
    return SetFromList.of(client.getBinaryMultiBulkReply());
  }

  @Override
  public Long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    client.zremrangeByLex(key, min, max);
    return client.getIntegerReply();
  }

  /**
   * Synchronously save the DB on disk.
   * <p>
   * Save the whole dataset on disk (this means that all the databases are saved, as well as keys
   * with an EXPIRE set (the expire is preserved). The server hangs while the saving is not
   * completed, no connection is served in the meanwhile. An OK code is returned when the DB was
   * fully stored in disk.
   * <p>
   * The background variant of this command is {@link #bgsave() BGSAVE} that is able to perform the
   * saving in the background while the server continues serving other clients.
   * <p>
   * @return Status code reply
   */
  @Override
  public String save() {
    client.save();
    return client.getStatusCodeReply();
  }

  /**
   * Asynchronously save the DB on disk.
   * <p>
   * Save the DB in background. The OK code is immediately returned. Redis forks, the parent
   * continues to server the clients, the child saves the DB on disk then exit. A client my be able
   * to check if the operation succeeded using the LASTSAVE command.
   * @return Status code reply
   */
  @Override
  public String bgsave() {
    client.bgsave();
    return client.getStatusCodeReply();
  }

  /**
   * Rewrite the append only file in background when it gets too big. Please for detailed
   * information about the Redis Append Only File check the <a
   * href="http://redis.io/topics/persistence#append-only-file">Append Only File Howto</a>.
   * <p>
   * BGREWRITEAOF rewrites the Append Only File in background when it gets too big. The Redis Append
   * Only File is a Journal, so every operation modifying the dataset is logged in the Append Only
   * File (and replayed at startup). This means that the Append Only File always grows. In order to
   * rebuild its content the BGREWRITEAOF creates a new version of the append only file starting
   * directly form the dataset in memory in order to guarantee the generation of the minimal number
   * of commands needed to rebuild the database.
   * <p>
   * @return Status code reply
   */
  @Override
  public String bgrewriteaof() {
    client.bgrewriteaof();
    return client.getStatusCodeReply();
  }

  /**
   * Return the UNIX time stamp of the last successfully saving of the dataset on disk.
   * <p>
   * Return the UNIX TIME of the last DB save executed with success. A client may check if a
   * {@link #bgsave() BGSAVE} command succeeded reading the LASTSAVE value, then issuing a BGSAVE
   * command and checking at regular intervals every N seconds if LASTSAVE changed.
   * @return Integer reply, specifically an UNIX time stamp.
   */
  @Override
  public Long lastsave() {
    client.lastsave();
    return client.getIntegerReply();
  }

  /**
   * Synchronously save the DB on disk, then shutdown the server.
   * <p>
   * Stop all the clients, save the DB, then quit the server. This commands makes sure that the DB
   * is switched off without the lost of any data. This is not guaranteed if the client uses simply
   * {@link #save() SAVE} and then {@link #quit() QUIT} because other clients may alter the DB data
   * between the two commands.
   * @return Status code reply on error. On success nothing is returned since the server quits and
   *         the connection is closed.
   */
  @Override
  public String shutdown() {
    client.shutdown();
    String status;
    try {
      status = client.getStatusCodeReply();
    } catch (JedisException ex) {
      status = null;
    }
    return status;
  }

  /**
   * Provide information and statistics about the server.
   * <p>
   * The info command returns different information and statistics about the server in an format
   * that's simple to parse by computers and easy to read by humans.
   * <p>
   * <b>Format of the returned String:</b>
   * <p>
   * All the fields are in the form field:value
   * 
   * <pre>
   * edis_version:0.07
   * connected_clients:1
   * connected_slaves:0
   * used_memory:3187
   * changes_since_last_save:0
   * last_save_time:1237655729
   * total_connections_received:1
   * total_commands_processed:1
   * uptime_in_seconds:25
   * uptime_in_days:0
   * </pre>
   * 
   * <b>Notes</b>
   * <p>
   * used_memory is returned in bytes, and is the total number of bytes allocated by the program
   * using malloc.
   * <p>
   * uptime_in_days is redundant since the uptime in seconds contains already the full uptime
   * information, this field is only mainly present for humans.
   * <p>
   * changes_since_last_save does not refer to the number of key changes, but to the number of
   * operations that produced some kind of change in the dataset.
   * <p>
   * @return Bulk reply
   */
  @Override
  public String info() {
    client.info();
    return client.getBulkReply();
  }

  @Override
  public String info(final String section) {
    client.info(section);
    return client.getBulkReply();
  }

  /**
   * Dump all the received requests in real time.
   * <p>
   * MONITOR is a debugging command that outputs the whole sequence of commands received by the
   * Redis server. is very handy in order to understand what is happening into the database. This
   * command is used directly via telnet.
   * @param jedisMonitor
   */
  public void monitor(final JedisMonitor jedisMonitor) {
    client.monitor();
    client.getStatusCodeReply();
    jedisMonitor.proceed(client);
  }

  /**
   * Change the replication settings.
   * <p>
   * The SLAVEOF command can change the replication settings of a slave on the fly. If a Redis
   * server is already acting as slave, the command SLAVEOF NO ONE will turn off the replication
   * turning the Redis server into a MASTER. In the proper form SLAVEOF hostname port will make the
   * server a slave of the specific server listening at the specified hostname and port.
   * <p>
   * If a server is already a slave of some master, SLAVEOF hostname port will stop the replication
   * against the old server and start the synchronization against the new one discarding the old
   * dataset.
   * <p>
   * The form SLAVEOF no one will stop replication turning the server into a MASTER but will not
   * discard the replication. So if the old master stop working it is possible to turn the slave
   * into a master and set the application to use the new master in read/write. Later when the other
   * Redis server will be fixed it can be configured in order to work as slave.
   * <p>
   * @param host
   * @param port
   * @return Status code reply
   */
  @Override
  public String slaveof(final String host, final int port) {
    client.slaveof(host, port);
    return client.getStatusCodeReply();
  }

  @Override
  public String slaveofNoOne() {
    client.slaveofNoOne();
    return client.getStatusCodeReply();
  }

  /**
   * Retrieve the configuration of a running Redis server. Not all the configuration parameters are
   * supported.
   * <p>
   * CONFIG GET returns the current configuration parameters. This sub command only accepts a single
   * argument, that is glob style pattern. All the configuration parameters matching this parameter
   * are reported as a list of key-value pairs.
   * <p>
   * <b>Example:</b>
   * 
   * <pre>
   * $ redis-cli config get '*'
   * 1. "dbfilename"
   * 2. "dump.rdb"
   * 3. "requirepass"
   * 4. (nil)
   * 5. "masterauth"
   * 6. (nil)
   * 7. "maxmemory"
   * 8. "0\n"
   * 9. "appendfsync"
   * 10. "everysec"
   * 11. "save"
   * 12. "3600 1 300 100 60 10000"
   * 
   * $ redis-cli config get 'm*'
   * 1. "masterauth"
   * 2. (nil)
   * 3. "maxmemory"
   * 4. "0\n"
   * </pre>
   * @param pattern
   * @return Bulk reply.
   */
  @Override
  public List<byte[]> configGet(final byte[] pattern) {
    checkIsInMultiOrPipeline();
    client.configGet(pattern);
    return client.getBinaryMultiBulkReply();
  }

  /**
   * Reset the stats returned by INFO
   * @return
   */
  @Override
  public String configResetStat() {
    checkIsInMultiOrPipeline();
    client.configResetStat();
    return client.getStatusCodeReply();
  }

  /**
   * The CONFIG REWRITE command rewrites the redis.conf file the server was started with, applying
   * the minimal changes needed to make it reflect the configuration currently used by the server,
   * which may be different compared to the original one because of the use of the CONFIG SET command.
   * 
   * The rewrite is performed in a very conservative way:
   * <ul>
   * <li>Comments and the overall structure of the original redis.conf are preserved as much as possible.</li>
   * <li>If an option already exists in the old redis.conf file, it will be rewritten at the same position (line number).</li>
   * <li>If an option was not already present, but it is set to its default value, it is not added by the rewrite process.</li>
   * <li>If an option was not already present, but it is set to a non-default value, it is appended at the end of the file.</li>
   * <li>Non used lines are blanked. For instance if you used to have multiple save directives, but
   * the current configuration has fewer or none as you disabled RDB persistence, all the lines will be blanked.</li>
   * </ul>
   * 
   * CONFIG REWRITE is also able to rewrite the configuration file from scratch if the original one
   * no longer exists for some reason. However if the server was started without a configuration
   * file at all, the CONFIG REWRITE will just return an error.
   * @return OK when the configuration was rewritten properly. Otherwise an error is returned.
   */
  @Override
  public String configRewrite() {
    checkIsInMultiOrPipeline();
    client.configRewrite();
    return client.getStatusCodeReply();
  }

  /**
   * Alter the configuration of a running Redis server. Not all the configuration parameters are
   * supported.
   * <p>
   * The list of configuration parameters supported by CONFIG SET can be obtained issuing a
   * {@link #configGet(byte[]) CONFIG GET *} command.
   * <p>
   * The configuration set using CONFIG SET is immediately loaded by the Redis server that will
   * start acting as specified starting from the next command.
   * <p>
   * <b>Parameters value format</b>
   * <p>
   * The value of the configuration parameter is the same as the one of the same parameter in the
   * Redis configuration file, with the following exceptions:
   * <p>
   * <ul>
   * <li>The save parameter is a list of space-separated integers. Every pair of integers specify the
   * time and number of changes limit to trigger a save. For instance the command CONFIG SET save
   * "3600 10 60 10000" will configure the server to issue a background saving of the RDB file every
   * 3600 seconds if there are at least 10 changes in the dataset, and every 60 seconds if there are
   * at least 10000 changes. To completely disable automatic snapshots just set the parameter as an
   * empty string.
   * <li>All the integer parameters representing memory are returned and accepted only using bytes
   * as unit.
   * </ul>
   * @param parameter
   * @param value
   * @return Status code reply
   */
  @Override
  public byte[] configSet(final byte[] parameter, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.configSet(parameter, value);
    return client.getBinaryBulkReply();
  }

  public boolean isConnected() {
    return client.isConnected();
  }

  @Override
  public Long strlen(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.strlen(key);
    return client.getIntegerReply();
  }

  public void sync() {
    client.sync();
  }

  @Override
  public Long lpushx(final byte[] key, final byte[]... string) {
    checkIsInMultiOrPipeline();
    client.lpushx(key, string);
    return client.getIntegerReply();
  }

  /**
   * Undo a {@link #expire(byte[], int) expire} at turning the expire key into a normal key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return Integer reply, specifically: 1: the key is now persist. 0: the key is not persist (only
   *         happens when key not set).
   */
  @Override
  public Long persist(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.persist(key);
    return client.getIntegerReply();
  }

  @Override
  public Long rpushx(final byte[] key, final byte[]... string) {
    checkIsInMultiOrPipeline();
    client.rpushx(key, string);
    return client.getIntegerReply();
  }

  @Override
  public byte[] echo(final byte[] string) {
    checkIsInMultiOrPipeline();
    client.echo(string);
    return client.getBinaryBulkReply();
  }

  @Override
  public Long linsert(final byte[] key, final ListPosition where, final byte[] pivot,
      final byte[] value) {
    checkIsInMultiOrPipeline();
    client.linsert(key, where, pivot, value);
    return client.getIntegerReply();
  }

  @Override
  public String debug(final DebugParams params) {
    client.debug(params);
    return client.getStatusCodeReply();
  }

  public Client getClient() {
    return client;
  }

  /**
   * Pop a value from a list, push it to another list and return it; or block until one is available
   * @param source
   * @param destination
   * @param timeout
   * @return the element
   */
  @Override
  public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
    checkIsInMultiOrPipeline();
    client.brpoplpush(source, destination, timeout);
    client.setTimeoutInfinite();
    try {
      return client.getBinaryBulkReply();
    } finally {
      client.rollbackTimeout();
    }
  }

  /**
   * Sets or clears the bit at offset in the string value stored at key
   * @param key
   * @param offset
   * @param value
   * @return
   */
  @Override
  public Boolean setbit(final byte[] key, final long offset, final boolean value) {
    checkIsInMultiOrPipeline();
    client.setbit(key, offset, value);
    return client.getIntegerReply() == 1;
  }

  @Override
  public Boolean setbit(final byte[] key, final long offset, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.setbit(key, offset, value);
    return client.getIntegerReply() == 1;
  }

  /**
   * Returns the bit value at offset in the string value stored at key
   * @param key
   * @param offset
   * @return
   */
  @Override
  public Boolean getbit(final byte[] key, final long offset) {
    checkIsInMultiOrPipeline();
    client.getbit(key, offset);
    return client.getIntegerReply() == 1;
  }

  public Long bitpos(final byte[] key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  public Long bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    checkIsInMultiOrPipeline();
    client.bitpos(key, value, params);
    return client.getIntegerReply();
  }

  @Override
  public Long setrange(final byte[] key, final long offset, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.setrange(key, offset, value);
    return client.getIntegerReply();
  }

  @Override
  public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
    checkIsInMultiOrPipeline();
    client.getrange(key, startOffset, endOffset);
    return client.getBinaryBulkReply();
  }

  @Override
  public Long publish(final byte[] channel, final byte[] message) {
    checkIsInMultiOrPipeline();
    client.publish(channel, message);
    return client.getIntegerReply();
  }

  @Override
  public void subscribe(BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    client.setTimeoutInfinite();
    try {
      jedisPubSub.proceed(client, channels);
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public void psubscribe(BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    client.setTimeoutInfinite();
    try {
      jedisPubSub.proceedWithPatterns(client, patterns);
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public int getDB() {
    return client.getDB();
  }

  /**
   * Evaluates scripts using the Lua interpreter built into Redis starting from version 2.6.0.
   * <p>
   * @param script
   * @param keys
   * @param args
   * @return Script result
   */
  @Override
  public Object eval(final byte[] script, final List<byte[]> keys, final List<byte[]> args) {
    return eval(script, toByteArray(keys.size()), getParamsWithBinary(keys, args));
  }

  protected static byte[][] getParamsWithBinary(List<byte[]> keys, List<byte[]> args) {
    final int keyCount = keys.size();
    final int argCount = args.size();
    byte[][] params = new byte[keyCount + argCount][];

    for (int i = 0; i < keyCount; i++)
      params[i] = keys.get(i);

    for (int i = 0; i < argCount; i++)
      params[keyCount + i] = args.get(i);

    return params;
  }

  @Override
  public Object eval(final byte[] script, final byte[] keyCount, final byte[]... params) {
    checkIsInMultiOrPipeline();
    client.eval(script, keyCount, params);
    client.setTimeoutInfinite();
    try {
      return client.getOne();
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
    return eval(script, toByteArray(keyCount), params);
  }

  @Override
  public Object eval(final byte[] script) {
    return eval(script, 0);
  }

  @Override
  public Object evalsha(final byte[] sha1) {
    return evalsha(sha1, 0);
  }

  @Override
  public Object evalsha(final byte[] sha1, final List<byte[]> keys, final List<byte[]> args) {
    return evalsha(sha1, keys.size(), getParamsWithBinary(keys, args));
  }

  @Override
  public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
    checkIsInMultiOrPipeline();
    client.evalsha(sha1, keyCount, params);
    client.setTimeoutInfinite();
    try {
      return client.getOne();
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public String scriptFlush() {
    client.scriptFlush();
    return client.getStatusCodeReply();
  }

  public Long scriptExists(final byte[] sha1) {
    byte[][] a = new byte[1][];
    a[0] = sha1;
    return scriptExists(a).get(0);
  }

  @Override
  public List<Long> scriptExists(final byte[]... sha1) {
    client.scriptExists(sha1);
    return client.getIntegerMultiBulkReply();
  }

  @Override
  public byte[] scriptLoad(final byte[] script) {
    client.scriptLoad(script);
    return client.getBinaryBulkReply();
  }

  @Override
  public String scriptKill() {
    client.scriptKill();
    return client.getStatusCodeReply();
  }

  @Override
  public String slowlogReset() {
    client.slowlogReset();
    return client.getBulkReply();
  }

  @Override
  public Long slowlogLen() {
    client.slowlogLen();
    return client.getIntegerReply();
  }

  @Override
  public List<byte[]> slowlogGetBinary() {
    client.slowlogGet();
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> slowlogGetBinary(final long entries) {
    client.slowlogGet(entries);
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public Long objectRefcount(final byte[] key) {
    client.objectRefcount(key);
    return client.getIntegerReply();
  }

  @Override
  public byte[] objectEncoding(final byte[] key) {
    client.objectEncoding(key);
    return client.getBinaryBulkReply();
  }

  @Override
  public Long objectIdletime(final byte[] key) {
    client.objectIdletime(key);
    return client.getIntegerReply();
  }

  @Override
  public List<byte[]> objectHelpBinary() {
    client.objectHelp();
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public Long objectFreq(final byte[] key) {
    client.objectFreq(key);
    return client.getIntegerReply();
  }

  @Override
  public Long bitcount(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.bitcount(key);
    return client.getIntegerReply();
  }

  @Override
  public Long bitcount(final byte[] key, final long start, final long end) {
    checkIsInMultiOrPipeline();
    client.bitcount(key, start, end);
    return client.getIntegerReply();
  }

  @Override
  public Long bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
    checkIsInMultiOrPipeline();
    client.bitop(op, destKey, srcKeys);
    return client.getIntegerReply();
  }

  @Override
  public byte[] dump(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.dump(key);
    return client.getBinaryBulkReply();
  }

  @Override
  public String restore(final byte[] key, final int ttl, final byte[] serializedValue) {
    checkIsInMultiOrPipeline();
    client.restore(key, ttl, serializedValue);
    return client.getStatusCodeReply();
  }

  @Override
  public String restoreReplace(final byte[] key, final int ttl, final byte[] serializedValue) {
    checkIsInMultiOrPipeline();
    client.restoreReplace(key, ttl, serializedValue);
    return client.getStatusCodeReply();
  }

  /**
   * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
   * the server. A key with an associated timeout is said to be volatile in Redis terminology.
   * <p>
   * Volatile keys are stored on disk like the other keys, the timeout is persistent too like all the
   * other aspects of the dataset. Saving a dataset containing expires and stopping the server does
   * not stop the flow of time as Redis stores on disk the time when the key will no longer be
   * available as Unix time, and not the remaining milliseconds.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link #persist(byte[]) PERSIST} command.
   * <p>
   * Time complexity: O(1)
   * @see <a href="http://redis.io/commands/pexpire">PEXPIRE Command</a>
   * @param key
   * @param milliseconds
   * @return Integer reply, specifically: 1: the timeout was set. 0: the timeout was not set since
   *         the key already has an associated timeout (this may happen only in Redis versions <
   *         2.1.3, Redis >= 2.1.3 will happily update the timeout), or the key does not exist.
   */
  @Override
  public Long pexpire(final byte[] key, final long milliseconds) {
    checkIsInMultiOrPipeline();
    client.pexpire(key, milliseconds);
    return client.getIntegerReply();
  }

  @Override
  public Long pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    checkIsInMultiOrPipeline();
    client.pexpireAt(key, millisecondsTimestamp);
    return client.getIntegerReply();
  }

  @Override
  public Long pttl(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.pttl(key);
    return client.getIntegerReply();
  }

  /**
   * PSETEX works exactly like {@link #setex(byte[], int, byte[])} with the sole difference that the
   * expire time is specified in milliseconds instead of seconds. Time complexity: O(1)
   * @param key
   * @param milliseconds
   * @param value
   * @return Status code reply
   */
  @Override
  public String psetex(final byte[] key, final long milliseconds, final byte[] value) {
    checkIsInMultiOrPipeline();
    client.psetex(key, milliseconds, value);
    return client.getStatusCodeReply();
  }

  @Override
  public byte[] memoryDoctorBinary() {
    checkIsInMultiOrPipeline();
    client.memoryDoctor();
    return client.getBinaryBulkReply();
  }

  @Override
  public byte[] aclWhoAmIBinary() {
    checkIsInMultiOrPipeline();
    client.aclWhoAmI();
    return client.getBinaryBulkReply();
  }

  @Override
  public byte[] aclGenPassBinary() {
    checkIsInMultiOrPipeline();
    client.aclGenPass();
    return client.getBinaryBulkReply();
  }

  @Override
  public List<byte[]> aclListBinary() {
    checkIsInMultiOrPipeline();
    client.aclList();
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclUsersBinary() {
    checkIsInMultiOrPipeline();
    client.aclUsers();
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public AccessControlUser aclGetUser(byte[] name) {
    checkIsInMultiOrPipeline();
    client.aclGetUser(name);
    return BuilderFactory.ACCESS_CONTROL_USER.build(client.getObjectMultiBulkReply());
  }

  @Override
  public String aclSetUser(byte[] name) {
    checkIsInMultiOrPipeline();
    client.aclSetUser(name);
    return client.getStatusCodeReply();
  }

  @Override
  public String aclSetUser(byte[] name, byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.aclSetUser(name, keys);
    return client.getStatusCodeReply();
  }

  @Override
  public Long aclDelUser(byte[] name) {
    checkIsInMultiOrPipeline();
    client.aclDelUser(name);
    return client.getIntegerReply();
  }

  @Override
  public List<byte[]> aclCatBinary() {
    checkIsInMultiOrPipeline();
    client.aclCat();
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclCat(byte[] category) {
    checkIsInMultiOrPipeline();
    client.aclCat(category);
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public String clientKill(final byte[] ipPort) {
    checkIsInMultiOrPipeline();
    this.client.clientKill(ipPort);
    return this.client.getStatusCodeReply();
  }

  @Override
  public String clientKill(final String ip, final int port) {
    checkIsInMultiOrPipeline();
    this.client.clientKill(ip, port);
    return this.client.getStatusCodeReply();
  }

  @Override
  public Long clientKill(ClientKillParams params) {
    checkIsInMultiOrPipeline();
    this.client.clientKill(params);
    return this.client.getIntegerReply();
  }

  @Override
  public byte[] clientGetnameBinary() {
    checkIsInMultiOrPipeline();
    client.clientGetname();
    return client.getBinaryBulkReply();
  }

  @Override
  public byte[] clientListBinary() {
    checkIsInMultiOrPipeline();
    client.clientList();
    return client.getBinaryBulkReply();
  }

  @Override
  public String clientSetname(final byte[] name) {
    checkIsInMultiOrPipeline();
    client.clientSetname(name);
    return client.getBulkReply();
  }

  public String clientPause(final long timeout) {
    checkIsInMultiOrPipeline();
    client.clientPause(timeout);
    return client.getBulkReply();
  }

  public List<String> time() {
    checkIsInMultiOrPipeline();
    client.time();
    return client.getMultiBulkReply();
  }

  @Override
  public String migrate(final String host, final int port, final byte[] key,
      final int destinationDb, final int timeout) {
    checkIsInMultiOrPipeline();
    client.migrate(host, port, key, destinationDb, timeout);
    return client.getStatusCodeReply();
  }

  @Override
  public String migrate(final String host, final int port, final int destinationDB,
      final int timeout, final MigrateParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.migrate(host, port, destinationDB, timeout, params, keys);
    return client.getStatusCodeReply();
  }

  /**
   * Syncrhonous replication of Redis as described here: http://antirez.com/news/66 Since Java
   * Object class has implemented "wait" method, we cannot use it, so I had to change the name of
   * the method. Sorry :S
   */
  @Override
  public Long waitReplicas(final int replicas, final long timeout) {
    checkIsInMultiOrPipeline();
    client.waitReplicas(replicas, timeout);
    return client.getIntegerReply();
  }

  @Override
  public Long pfadd(final byte[] key, final byte[]... elements) {
    checkIsInMultiOrPipeline();
    client.pfadd(key, elements);
    return client.getIntegerReply();
  }

  @Override
  public long pfcount(final byte[] key) {
    checkIsInMultiOrPipeline();
    client.pfcount(key);
    return client.getIntegerReply();
  }

  @Override
  public String pfmerge(final byte[] destkey, final byte[]... sourcekeys) {
    checkIsInMultiOrPipeline();
    client.pfmerge(destkey, sourcekeys);
    return client.getStatusCodeReply();
  }

  @Override
  public Long pfcount(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    client.pfcount(keys);
    return client.getIntegerReply();
  }

  public ScanResult<byte[]> scan(final byte[] cursor) {
    return scan(cursor, new ScanParams());
  }

  public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    client.scan(cursor, params);
    List<Object> result = client.getObjectMultiBulkReply();
    byte[] newcursor = (byte[]) result.get(0);
    List<byte[]> rawResults = (List<byte[]>) result.get(1);
    return new ScanResult<>(newcursor, rawResults);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor,
      final ScanParams params) {
    checkIsInMultiOrPipeline();
    client.hscan(key, cursor, params);
    List<Object> result = client.getObjectMultiBulkReply();
    byte[] newcursor = (byte[]) result.get(0);
    List<Map.Entry<byte[], byte[]>> results = new ArrayList<>();
    List<byte[]> rawResults = (List<byte[]>) result.get(1);
    Iterator<byte[]> iterator = rawResults.iterator();
    while (iterator.hasNext()) {
      results.add(new AbstractMap.SimpleEntry<byte[], byte[]>(iterator.next(), iterator.next()));
    }
    return new ScanResult<>(newcursor, results);
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    client.sscan(key, cursor, params);
    List<Object> result = client.getObjectMultiBulkReply();
    byte[] newcursor = (byte[]) result.get(0);
    List<byte[]> rawResults = (List<byte[]>) result.get(1);
    return new ScanResult<>(newcursor, rawResults);
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    client.zscan(key, cursor, params);
    List<Object> result = client.getObjectMultiBulkReply();
    byte[] newcursor = (byte[]) result.get(0);
    List<Tuple> results = new ArrayList<>();
    List<byte[]> rawResults = (List<byte[]>) result.get(1);
    Iterator<byte[]> iterator = rawResults.iterator();
    while (iterator.hasNext()) {
      results.add(new Tuple(iterator.next(), BuilderFactory.DOUBLE.build(iterator.next())));
    }
    return new ScanResult<>(newcursor, results);
  }

  @Override
  public Long geoadd(final byte[] key, final double longitude, final double latitude, final byte[] member) {
    checkIsInMultiOrPipeline();
    client.geoadd(key, longitude, latitude, member);
    return client.getIntegerReply();
  }

  @Override
  public Long geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    checkIsInMultiOrPipeline();
    client.geoadd(key, memberCoordinateMap);
    return client.getIntegerReply();
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    checkIsInMultiOrPipeline();
    client.geodist(key, member1, member2);
    String dval = client.getBulkReply();
    return (dval != null ? new Double(dval) : null);
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    client.geodist(key, member1, member2, unit);
    String dval = client.getBulkReply();
    return (dval != null ? new Double(dval) : null);
  }

  @Override
  public List<byte[]> geohash(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    client.geohash(key, members);
    return client.getBinaryMultiBulkReply();
  }

  @Override
  public List<GeoCoordinate> geopos(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    client.geopos(key, members);
    return BuilderFactory.GEO_COORDINATE_LIST.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    client.georadius(key, longitude, latitude, radius, unit);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    client.georadiusReadonly(key, longitude, latitude, radius, unit);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    client.georadius(key, longitude, latitude, radius, unit, param);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    client.georadiusReadonly(key, longitude, latitude, radius, unit, param);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }
  
  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member, final double radius,
      final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    client.georadiusByMember(key, member, radius, unit);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius,
      final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    client.georadiusByMemberReadonly(key, member, radius, unit);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member, final double radius,
      final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    client.georadiusByMember(key, member, radius, unit, param);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius,
      final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    client.georadiusByMemberReadonly(key, member, radius, unit, param);
    return BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT.build(client.getObjectMultiBulkReply());
  }

  /**
   * A decorator to implement Set from List. Assume that given List do not contains duplicated
   * values. The resulting set displays the same ordering, concurrency, and performance
   * characteristics as the backing list. This class should be used only for Redis commands which
   * return Set result.
   * @param <E>
   */
  protected static class SetFromList<E> extends AbstractSet<E> implements Serializable {
    private static final long serialVersionUID = -2850347066962734052L;
    private final List<E> list;

    private SetFromList(List<E> list) {
      if (list == null) {
        throw new NullPointerException("list");
      }
      this.list = list;
    }

    @Override
    public void clear() {
      list.clear();
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return list.contains(o);
    }

    @Override
    public boolean remove(Object o) {
      return list.remove(o);
    }

    @Override
    public boolean add(E e) {
      return !contains(e) && list.add(e);
    }

    @Override
    public Iterator<E> iterator() {
      return list.iterator();
    }

    @Override
    public Object[] toArray() {
      return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return list.toArray(a);
    }

    @Override
    public String toString() {
      return list.toString();
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) return false;
      if (o == this) return true;
      if (!(o instanceof Set)) return false;

      Collection<?> c = (Collection<?>) o;
      if (c.size() != size()) {
        return false;
      }

      return containsAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return list.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return list.retainAll(c);
    }

    protected static <E> SetFromList<E> of(List<E> list) {
      return new SetFromList<>(list);
    }
  }

  @Override
  public List<Long> bitfield(final byte[] key, final byte[]... arguments) {
    checkIsInMultiOrPipeline();
    client.bitfield(key, arguments);
    return client.getIntegerMultiBulkReply();
  }

  @Override
  public List<Long> bitfieldReadonly(byte[] key, final byte[]... arguments) {
    checkIsInMultiOrPipeline();
    client.bitfieldReadonly(key, arguments);
    return client.getIntegerMultiBulkReply();
  }

  @Override
  public Long hstrlen(final byte[] key, final byte[] field) {
    checkIsInMultiOrPipeline();
    client.hstrlen(key, field);
    return client.getIntegerReply();
  }

  @Override
  public List<byte[]> xread(int count, long block, Map<byte[], byte[]> streams) {
    checkIsInMultiOrPipeline();
    client.xread(count, block, streams);
    client.setTimeoutInfinite();
    try {
      return client.getBinaryMultiBulkReply();
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, int count, long block, boolean noAck,
      Map<byte[], byte[]> streams) {
    checkIsInMultiOrPipeline();
    client.xreadGroup(groupname, consumer, count, block, noAck, streams);
    client.setTimeoutInfinite();
    try {
      return client.getBinaryMultiBulkReply();
    } finally {
      client.rollbackTimeout();
    }
  }

  @Override
  public byte[] xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash, long maxLen, boolean approximateLength) {
    checkIsInMultiOrPipeline();
    client.xadd(key, id, hash, maxLen, approximateLength);
    return client.getBinaryBulkReply();  
  }

  @Override
  public Long xlen(byte[] key) {
    checkIsInMultiOrPipeline();
    client.xlen(key);
    return client.getIntegerReply();  
  }

  @Override
  public List<byte[]> xrange(byte[] key, byte[] start, byte[] end, long count) {
    checkIsInMultiOrPipeline();
    client.xrange(key, start, end, count);
    return client.getBinaryMultiBulkReply();  
  }

  @Override
  public List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    checkIsInMultiOrPipeline();
    client.xrevrange(key, end, start, count);
    return client.getBinaryMultiBulkReply();  
  }

  @Override
  public Long xack(byte[] key, byte[] group, byte[]... ids) {
    checkIsInMultiOrPipeline();
    client.xack(key, group, ids);
    return client.getIntegerReply();
  }

  @Override
  public String xgroupCreate(byte[] key, byte[] consumer, byte[] id, boolean makeStream) {
    checkIsInMultiOrPipeline();
    client.xgroupCreate(key, consumer, id, makeStream);
    return client.getStatusCodeReply();
  }

  @Override
  public String xgroupSetID(byte[] key, byte[] consumer, byte[] id) {
    checkIsInMultiOrPipeline();
    client.xgroupSetID(key, consumer, id);
    return client.getStatusCodeReply();
  }

  @Override
  public Long xgroupDestroy(byte[] key, byte[] consumer) {
    checkIsInMultiOrPipeline();
    client.xgroupDestroy(key, consumer);
    return client.getIntegerReply();
  }

  @Override
  public Long xgroupDelConsumer(byte[] key, byte[] consumer, byte[] consumerName) {
    checkIsInMultiOrPipeline();
    client.xgroupDelConsumer(key, consumer, consumerName);
    return client.getIntegerReply();
  }

  @Override
  public Long xdel(byte[] key, byte[]... ids) {
    checkIsInMultiOrPipeline();
    client.xdel(key, ids);
    return client.getIntegerReply();
  }

  @Override
  public Long xtrim(byte[] key, long maxLen, boolean approximateLength) {
    checkIsInMultiOrPipeline();
    client.xtrim(key, maxLen, approximateLength);
    return client.getIntegerReply();
  }

  @Override
  public List<byte[]> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername) {
    checkIsInMultiOrPipeline();
    client.xpending(key, groupname, start, end, count, consumername);
    return client.getBinaryMultiBulkReply();  }

  @Override
  public   List<byte[]> xclaim(byte[] key, byte[] groupname, byte[] consumername, long minIdleTime, long newIdleTime, int retries, boolean force, byte[][] ids){
    checkIsInMultiOrPipeline();
    client.xclaim(key, groupname, consumername, minIdleTime, newIdleTime, retries, force, ids);
    return client.getBinaryMultiBulkReply();  
  }

  public Object sendCommand(ProtocolCommand cmd, byte[]... args) {
    checkIsInMultiOrPipeline();
    client.sendCommand(cmd, args);
    return client.getOne();
  }

  @Override
  public StreamInfo xinfoStream(byte[] key) {
    checkIsInMultiOrPipeline();
    client.xinfoStream(key);

    return BuilderFactory.STREAM_INFO.build(client.getOne());

  }

  @Override
  public List<StreamGroupInfo> xinfoGroup (byte[] key) {
    checkIsInMultiOrPipeline();
    client.xinfoGroup(key);

    return BuilderFactory.STREAM_GROUP_INFO_LIST.build(client.getBinaryMultiBulkReply());
  }
  @Override
  public List<StreamConsumersInfo> xinfoConsumers (byte[] key, byte[] group) {
    checkIsInMultiOrPipeline();
    client.xinfoConsumers(key,group);

    return BuilderFactory.STREAM_CONSUMERS_INFO_LIST.build(client.getBinaryMultiBulkReply());
  }

  public Object sendCommand(ProtocolCommand cmd) {
    return sendCommand(cmd, dummyArray);
  }
}
