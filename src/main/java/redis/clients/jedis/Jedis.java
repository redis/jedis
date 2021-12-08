package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.*;
import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.util.SafeEncoder.encode;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.Protocol.*;
import redis.clients.jedis.args.*;
import redis.clients.jedis.commands.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.Pool;

public class Jedis implements ServerCommands, DatabaseCommands, JedisCommands, JedisBinaryCommands,
    ControlCommands, ControlBinaryCommands, ClusterCommands, ModuleCommands,
    GenericControlCommands, Closeable {

  protected final Connection connection;
  private final CommandObjects commandObjects = new CommandObjects();
  private int db = 0;
  private Transaction transaction = null;
  private boolean isInMulti = false;
  private boolean isInWatch = false;
  private Pipeline pipeline = null;
  protected static final byte[][] DUMMY_ARRAY = new byte[0][];

  private Pool<Jedis> dataSource = null;

  public Jedis() {
    connection = new Connection();
  }

  /**
   * This constructor only accepts a URI string. {@link JedisURIHelper#isValid(java.net.URI)} can be
   * used before this.
   * @param url
   */
  public Jedis(final String url) {
    this(URI.create(url));
  }

  public Jedis(final HostAndPort hp) {
    connection = new Connection(hp);
  }

  public Jedis(final String host, final int port) {
    connection = new Connection(host, port);
  }

  public Jedis(final String host, final int port, final JedisClientConfig config) {
    this(new HostAndPort(host, port), config);
  }

  public Jedis(final HostAndPort hostPort, final JedisClientConfig config) {
    connection = new Connection(hostPort, config);
  }

  public Jedis(final String host, final int port, final boolean ssl) {
    this(host, port, DefaultJedisClientConfig.builder().ssl(ssl).build());
  }

  public Jedis(final String host, final int port, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder().ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(final String host, final int port, final int timeout) {
    this(host, port, timeout, timeout);
  }

  public Jedis(final String host, final int port, final int timeout, final boolean ssl) {
    this(host, port, timeout, timeout, ssl);
  }

  public Jedis(final String host, final int port, final int timeout, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, timeout, timeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public Jedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).build());
  }

  public Jedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).build());
  }

  public Jedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).ssl(ssl)
        .build());
  }

  public Jedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final boolean ssl, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout).ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(final String host, final int port, final int connectionTimeout,
      final int soTimeout, final int infiniteSoTimeout, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(host, port, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(soTimeout)
        .blockingSocketTimeoutMillis(infiniteSoTimeout).ssl(ssl)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(URI uri) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI \"%s\".", uri.toString()));
    }
    connection = new Connection(new HostAndPort(uri.getHost(), uri.getPort()),
        DefaultJedisClientConfig.builder().user(JedisURIHelper.getUser(uri))
            .password(JedisURIHelper.getPassword(uri)).database(JedisURIHelper.getDBIndex(uri))
            .ssl(JedisURIHelper.isRedisSSLScheme(uri)).build());
  }

  public Jedis(URI uri, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(final URI uri, final int timeout) {
    this(uri, timeout, timeout);
  }

  public Jedis(final URI uri, final int timeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, timeout, timeout, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public Jedis(final URI uri, final int connectionTimeout, final int soTimeout) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).build());
  }

  public Jedis(final URI uri, final int connectionTimeout, final int soTimeout,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).sslSocketFactory(sslSocketFactory)
        .sslParameters(sslParameters).hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(final URI uri, final int connectionTimeout, final int soTimeout,
      final int infiniteSoTimeout, final SSLSocketFactory sslSocketFactory,
      final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier) {
    this(uri, DefaultJedisClientConfig.builder().connectionTimeoutMillis(connectionTimeout)
        .socketTimeoutMillis(soTimeout).blockingSocketTimeoutMillis(infiniteSoTimeout)
        .sslSocketFactory(sslSocketFactory).sslParameters(sslParameters)
        .hostnameVerifier(hostnameVerifier).build());
  }

  public Jedis(final URI uri, JedisClientConfig config) {
    if (!JedisURIHelper.isValid(uri)) {
      throw new InvalidURIException(String.format(
        "Cannot open Redis connection due invalid URI \"%s\".", uri.toString()));
    }
    connection = new Connection(new HostAndPort(uri.getHost(), uri.getPort()),
        DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(config.getConnectionTimeoutMillis())
            .socketTimeoutMillis(config.getSocketTimeoutMillis())
            .blockingSocketTimeoutMillis(config.getBlockingSocketTimeoutMillis())
            .user(JedisURIHelper.getUser(uri)).password(JedisURIHelper.getPassword(uri))
            .database(JedisURIHelper.getDBIndex(uri)).clientName(config.getClientName())
            .ssl(JedisURIHelper.isRedisSSLScheme(uri)).sslSocketFactory(config.getSslSocketFactory())
            .sslParameters(config.getSslParameters()).hostnameVerifier(config.getHostnameVerifier())
            .build());
  }

  public Jedis(final JedisSocketFactory jedisSocketFactory) {
    connection = new Connection(jedisSocketFactory);
  }

  public Jedis(final JedisSocketFactory jedisSocketFactory, final JedisClientConfig clientConfig) {
    connection = new Connection(jedisSocketFactory, clientConfig);
  }

  public Jedis(final Connection connection) {
    this.connection = connection;
  }

  @Override
  public String toString() {
    return "Jedis{" + connection + '}';
  }

  // Legacy
  public Connection getClient() {
    return getConnection();
  }

  public Connection getConnection() {
    return connection;
  }

  // Legacy
  public void connect() {
    connection.connect();
  }

  // Legacy
  public void disconnect() {
    connection.disconnect();
  }

  public boolean isConnected() {
    return connection.isConnected();
  }

  public boolean isBroken() {
    return connection.isBroken();
  }

  public void resetState() {
    if (isConnected()) {
      if (transaction != null) {
        transaction.close();
      }

      if (pipeline != null) {
        pipeline.close();
      }

//      connection.resetState();
      if (isInWatch) {
        connection.sendCommand(UNWATCH);
        connection.getStatusCodeReply();
        isInWatch = false;
      }
    }

    transaction = null;
    pipeline = null;
  }

  protected void setDataSource(Pool<Jedis> jedisPool) {
    this.dataSource = jedisPool;
  }

  @Override
  public void close() {
    if (dataSource != null) {
      Pool<Jedis> pool = this.dataSource;
      this.dataSource = null;
      if (isBroken()) {
        pool.returnBrokenResource(this);
      } else {
        pool.returnResource(this);
      }
    } else {
      connection.close();
    }
  }

  // Legacy
  public Transaction multi() {
    transaction = new Transaction(this);
    return transaction;
  }

  // Legacy
  public Pipeline pipelined() {
    pipeline = new Pipeline(this);
    return pipeline;
  }

  // Legacy
  protected void checkIsInMultiOrPipeline() {
//    if (connection.isInMulti()) {
    if (transaction != null) {
      throw new IllegalStateException(
          "Cannot use Jedis when in Multi. Please use Transaction or reset jedis state.");
    } else if (pipeline != null && pipeline.hasPipelinedResponse()) {
      throw new IllegalStateException(
          "Cannot use Jedis when in Pipeline. Please use Pipeline or reset jedis state.");
    }
  }

  public int getDB() {
    return this.db;
  }

  /**
   * COPY source destination [DB destination-db] [REPLACE]
   *
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param db
   * @param replace
   */
  @Override
  public boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.copy(srcKey, dstKey, db, replace));
  }

  /**
   * COPY source destination [DB destination-db] [REPLACE]
   *
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param replace
   */
  @Override
  public boolean copy(byte[] srcKey, byte[] dstKey, boolean replace) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  /**
   * @return <code>PONG</code>
   */
  @Override
  public String ping() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.PING);
    return connection.getStatusCodeReply();
  }

  /**
   * Works same as {@link #ping()} but returns argument message instead of <code>PONG</code>.
   * @param message
   * @return message
   */
  public byte[] ping(final byte[] message) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.PING, message);
    return connection.getBinaryBulkReply();
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
    return connection.executeCommand(commandObjects.set(key, value));
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
    return connection.executeCommand(commandObjects.set(key, value, params));
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
    return connection.executeCommand(commandObjects.get(key));
  }

  /**
   * Get the value of key and delete the key. This command is similar to GET, except for the fact
   * that it also deletes the key on success (if and only if the key's value type is a string).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return the value of key
   * @since Redis 6.2
   */
  @Override
  public byte[] getDel(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getDel(key));
  }

  @Override
  public byte[] getEx(final byte[] key, final GetExParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getEx(key, params));
  }

  /**
   * Ask the server to silently close the connection.
   */
  @Override
  public String quit() {
    checkIsInMultiOrPipeline();
    return connection.quit();
  }

  /**
   * Test if the specified keys exist. The command returns the number of keys exist.
   * Time complexity: O(N)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys exist,
   *         0 if none of the specified keys exist.
   */
  @Override
  public long exists(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.exists(keys));
  }

  /**
   * Test if the specified key exists. The command returns true if the key exists, otherwise false is
   * returned. Note that even keys set with an empty string as value will return true. Time
   * complexity: O(1)
   * @param key
   * @return Boolean reply, true if the key exists, otherwise false
   */
  @Override
  public boolean exists(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.exists(key));
  }

  /**
   * Remove the specified keys. If a given key does not exist no operation is performed for this
   * key. The command returns the number of keys removed. Time complexity: O(1)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys were removed
   *         0 if none of the specified key existed
   */
  @Override
  public long del(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.del(keys));
  }

  @Override
  public long del(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.del(key));
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
  public long unlink(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public long unlink(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.unlink(key));
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
    return connection.executeCommand(commandObjects.type(key));
  }

  /**
   * Delete all the keys of the currently selected DB. This command never fails.
   * @return Status code reply
   */
  @Override
  public String flushDB() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FLUSHDB);
    return connection.getStatusCodeReply();
  }

  /**
   * Delete all the keys of the currently selected DB. This command never fails.
   * @param flushMode
   * @return Status code reply
   */
  @Override
  public String flushDB(FlushMode flushMode) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FLUSHDB, flushMode.getRaw());
    return connection.getStatusCodeReply();
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
    return connection.executeCommand(commandObjects.keys(pattern));
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
    return connection.executeCommand(commandObjects.randomBinaryKey());
  }

  /**
   * Atomically renames the key oldkey to newkey. If the source and destination name are the same an
   * error is returned. If newkey already exists it is overwritten.
   * <p>
   * Time complexity: O(1)
   * @param oldkey
   * @param newkey
   * @return Status code reply
   */
  @Override
  public String rename(final byte[] oldkey, final byte[] newkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rename(oldkey, newkey));
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
  public long renamenx(final byte[] oldkey, final byte[] newkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  /**
   * Return the number of keys in the currently selected database.
   * @return Integer reply
   */
  @Override
  public long dbSize() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(DBSIZE);
    return connection.getIntegerReply();
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
  public long expire(final byte[] key, final long seconds) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.expire(key, seconds));
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
  public long expireAt(final byte[] key, final long unixTime) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.expireAt(key, unixTime));
  }

  /**
   * The TTL command returns the remaining time to live in seconds of a key that has an
   * {@link #expire(byte[], int) EXPIRE} set. This introspection capability allows a Redis connection to
 check how many seconds a given key will continue to be part of the dataset.
   * @param key
   * @return Integer reply, returns the remaining time to live in seconds of a key that has an
   *         EXPIRE. If the Key does not exists or does not have an associated expire, -1 is
   *         returned.
   */
  @Override
  public long ttl(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.ttl(key));
  }

  /**
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @param keys
   * @return Integer reply: The number of keys that were touched.
   */
  @Override
  public long touch(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.touch(keys));
  }

  @Override
  public long touch(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.touch(key));
  }

  /**
   * Select the DB with having the specified zero-based numeric index. For default every new connection
 connection is automatically selected to DB 0.
   * @param index
   * @return Status code reply
   */
  @Override
  public String select(final int index) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(SELECT, toByteArray(index));
    String statusCodeReply = connection.getStatusCodeReply();
    this.db = index;
    return statusCodeReply;
  }

  @Override
  public String swapDB(final int index1, final int index2) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(SWAPDB, toByteArray(index1), toByteArray(index2));
    return connection.getStatusCodeReply();
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
  public long move(final byte[] key, final int dbIndex) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MOVE, key, toByteArray(dbIndex));
    return connection.getIntegerReply();
  }

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one. This
   * command never fails.
   * @return Status code reply
   */
  @Override
  public String flushAll() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FLUSHALL);
    return connection.getStatusCodeReply();
  }

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one. This
   * command never fails.
   * @param flushMode
   * @return Status code reply
   */
  @Override
  public String flushAll(FlushMode flushMode) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FLUSHALL, flushMode.getRaw());
    return connection.getStatusCodeReply();
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
    return connection.executeCommand(commandObjects.getSet(key, value));
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
    return connection.executeCommand(commandObjects.mget(keys));
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
  public long setnx(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setnx(key, value));
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
  public String setex(final byte[] key, final long seconds, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setex(key, seconds, value));
  }

  /**
   * Set the the respective keys to the respective values. MSET will replace old values with new
   * values, while {@link Jedis#msetnx(byte[][]) MSETNX} will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
 Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
 are modified, another connection talking to Redis can either see the changes to both A and B at
 once, or no modification at all.
   * @see Jedis#msetnx(byte[][])
   * @param keysvalues
   * @return Status code reply Basically +OK as MSET can't fail
   */
  @Override
  public String mset(final byte[]... keysvalues) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.mset(keysvalues));
  }

  /**
   * Set the the respective keys to the respective values. {@link Jedis#mset(byte[][]) MSET} will
   * replace old values with new values, while MSETNX will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
 Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
 are modified, another connection talking to Redis can either see the changes to both A and B at
 once, or no modification at all.
   * @see Jedis#mset(byte[][])
   * @param keysvalues
   * @return Integer reply, specifically: 1 if the all the keys were set 0 if no key was set (at
   *         least one key already existed)
   */
  @Override
  public long msetnx(final byte[]... keysvalues) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.msetnx(keysvalues));
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
  public long decrBy(final byte[] key, final long decrement) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.decrBy(key, decrement));
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
  public long decr(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.decr(key));
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
  public long incrBy(final byte[] key, final long increment) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incrBy(key, increment));
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
  public double incrByFloat(final byte[] key, final double increment) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incrByFloat(key, increment));
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
  public long incr(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incr(key));
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
  public long append(final byte[] key, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.append(key, value));
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
    return connection.executeCommand(commandObjects.substr(key, start, end));
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
  public long hset(final byte[] key, final byte[] field, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(final byte[] key, final Map<byte[], byte[]> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hset(key, hash));
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
    return connection.executeCommand(commandObjects.hget(key, field));
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
  public long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hsetnx(key, field, value));
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
    return connection.executeCommand(commandObjects.hmset(key, hash));
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
    return connection.executeCommand(commandObjects.hmget(key, fields));
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
  public long hincrBy(final byte[] key, final byte[] field, final long value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hincrBy(key, field, value));
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
  public double hincrByFloat(final byte[] key, final byte[] field, final double value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  /**
   * Test for existence of a specified field in a hash. <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @return Return true if the hash stored at key contains the specified field. Return false if the key is
   *         not found or the field is not present.
   */
  @Override
  public boolean hexists(final byte[] key, final byte[] field) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hexists(key, field));
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
  public long hdel(final byte[] key, final byte[]... fields) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hdel(key, fields));
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
  public long hlen(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hlen(key));
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
    return connection.executeCommand(commandObjects.hkeys(key));
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
    return connection.executeCommand(commandObjects.hvals(key));
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
    return connection.executeCommand(commandObjects.hgetAll(key));
  }

  /**
   * Get one random field from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @return one random field from a hash.
   */
  @Override
  public byte[] hrandfield(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfield(key));
  }

  /**
   * Get multiple random fields from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @return multiple random fields from a hash.
   */
  @Override
  public List<byte[]> hrandfield(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfield(key, count));
  }

  /**
   * Get one or multiple random fields with values from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @return one or multiple random fields with values from a hash.
   */
  @Override
  public Map<byte[], byte[]> hrandfieldWithValues(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public long rpush(final byte[] key, final byte[]... strings) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpush(key, strings));
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public long lpush(final byte[] key, final byte[]... strings) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpush(key, strings));
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
  public long llen(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.llen(key));
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
    return connection.executeCommand(commandObjects.lrange(key, start, stop));
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
    return connection.executeCommand(commandObjects.ltrim(key, start, stop));
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
    return connection.executeCommand(commandObjects.lindex(key, index));
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
    return connection.executeCommand(commandObjects.lset(key, index, value));
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
  public long lrem(final byte[] key, final long count, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lrem(key, count, value));
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
    return connection.executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<byte[]> lpop(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpop(key, count));
  }

  /**
   * Returns the index of the first matching element inside a redis list. If the element is found,
   * its index (the zero-based position in the list) is returned. Otherwise, if no match is found,
   * 'nil' is returned.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the list
   * @see #lpos(byte[], byte[])
   * @param key
   * @param element
   * @return Integer Reply, specifically: The index of first matching element in the list. Value will
   * be 'nil' when the element is not present in the list.
   */
  @Override
  public Long lpos(final byte[] key, final byte[] element) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element));
  }

  /**
   * In case there are multiple matches Rank option specifies the "rank" of the element to return.
   * A rank of 1 returns the first match, 2 to return the second match, and so forth.
   * If list `foo` has elements ("a","b","c","1","2","3","c","c"), The function call to get the
   * index of second occurrence of "c" will be as follows lpos("foo","c", LPosParams.lPosParams().rank(2)).
   * <p>
   * Maxlen option compares the element provided only with a given maximum number of list items.
   * A value of 1000 will make sure that the command performs only 1000 comparisons. The
   * comparison is made for the first part or the last part depending on the fact we use a positive or
   * negative rank.
   * Following is how we could use the Maxlen option lpos("foo", "b", LPosParams.lPosParams().rank(1).maxlen(2)).
   * @see   #lpos(byte[], byte[], LPosParams)
   * @param key
   * @param element
   * @param params
   * @return Integer Reply
   */
  @Override
  public Long lpos(final byte[] key, final byte[] element, final LPosParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element, params));
  }

  /**
   * Count will return list of position of all the first N matching elements. It is possible to
   * specify 0 as the number of matches, as a way to tell the command we want all the matches
   * found returned as an array of indexes. When count is used and no match is found, an empty list
   * is returned.
   * <p>
   * Time complexity: O(N) where N is the number of elements in the list
   * @see #lpos(byte[], byte[], LPosParams, long)
   * @param key
   * @param element
   * @param params
   * @param count
   * @return Returns value will be a list containing position of the matching elements inside the list.
   */
  @Override
  public List<Long> lpos(final byte[] key, final byte[] element, final LPosParams params,
      final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element, params, count));
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
    return connection.executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<byte[]> rpop(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpop(key, count));
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
    return connection.executeCommand(commandObjects.rpoplpush(srckey, dstkey));
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
  public long sadd(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sadd(key, members));
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
    return connection.executeCommand(commandObjects.smembers(key));
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
  public long srem(final byte[] key, final byte[]... member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.srem(key, member));
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
    return connection.executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<byte[]> spop(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.spop(key, count));
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
  public long smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.smove(srckey, dstkey, member));
  }

  /**
   * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
   * for empty sets.
   * @param key
   * @return Integer reply, specifically: the cardinality (number of elements) of the set as an
   *         integer.
   */
  @Override
  public long scard(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scard(key));
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
  public boolean sismember(final byte[] key, final byte[] member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sismember(key, member));
  }

  /**
   * Returns whether each member is a member of the set stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements being checked for membership
   * @param key
   * @param members
   * @return List representing the membership of the given elements, in the same order as they are requested.
   */
  @Override
  public List<Boolean> smismember(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.smismember(key, members));
  }

  /**
   * Return the members of a set resulting from the intersection of all the sets hold at the
   * specified keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the
 connection as a multi-bulk reply (see the protocol specification for more information). If just a
 single key is specified, then this command produces the same result as
 {@link #smembers(byte[]) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
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
    return connection.executeCommand(commandObjects.sinter(keys));
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
  public long sinterstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  /**
   * Return the members of a set resulting from the union of all the sets hold at the specified
   * keys. Like in {@link #lrange(byte[], long, long)} LRANGE} the result is sent to the connection as a
 multi-bulk reply (see the protocol specification for more information). If just a single key is
 specified, then this command produces the same result as {@link #smembers(byte[]) SMEMBERS}.
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
    return connection.executeCommand(commandObjects.sunion(keys));
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
  public long sunionstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sunionstore(dstkey, keys));
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
    return connection.executeCommand(commandObjects.sdiff(keys));
  }

  /**
   * This command works exactly like {@link Jedis#sdiff(byte[][]) SDIFF} but instead of being returned
   * the resulting set is stored in dstkey.
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public long sdiffstore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sdiffstore(dstkey, keys));
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
    return connection.executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<byte[]> srandmember(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.srandmember(key, count));
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
  public long zadd(final byte[] key, final double score, final byte[] member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(final byte[] key, final double score, final byte[] member,
      final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(final byte[] key, final double score, final byte[] member, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public List<byte[]> zrange(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrange(key, start, stop));
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
  public long zrem(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrem(key, members));
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
  public double zincrby(final byte[] key, final double increment, final byte[] member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(final byte[] key, final double increment, final byte[] member,
      final ZIncrByParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zincrby(key, increment, member, params));
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
    return connection.executeCommand(commandObjects.zrank(key, member));
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
    return connection.executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public List<byte[]> zrevrange(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public byte[] zrandmember(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public List<byte[]> zrandmember(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(final byte[] key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmemberWithScores(key, count));
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
  public long zcard(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcard(key));
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
    return connection.executeCommand(commandObjects.zscore(key, member));
  }

  /**
   * Returns the scores associated with the specified members in the sorted set stored at key.
   * For every member that does not exist in the sorted set, a nil value is returned.
   * <p>
   * <b>Time complexity:</b> O(N) where N is the number of members being requested.
   * @param key
   * @param members
   * @return the scores
   */
  @Override
  public List<Double> zmscore(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(final byte[] key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmin(key, count));
  }

  public String watch(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(WATCH, keys);
//    return connection.getStatusCodeReply();
    String status = connection.getStatusCodeReply();
    isInWatch = true;
    return status;
  }

  public String unwatch() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(UNWATCH);
    return connection.getStatusCodeReply();
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
    return connection.executeCommand(commandObjects.sort(key));
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
    return connection.executeCommand(commandObjects.sort(key, sortingParameters));
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
  public long sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key, sortingParameters, dstkey));
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
  public long sort(final byte[] key, final byte[] dstkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key, dstkey));
  }

  /**
   * Pop an element from a list, push it to another list and return it
   * @param srcKey
   * @param dstKey
   * @param from
   * @param to
   */
  @Override
  public byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  /**
   * Pop an element from a list, push it to another list and return it; or block until one is available
   * @param srcKey
   * @param dstKey
   * @param from
   * @param to
   * @param timeout
   */
  @Override
  public byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
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
 If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
 connection performs a LPUSH or an RPUSH operation against one of the lists.
 <p>
 Once new data is present on one of the lists, the connection finally returns with the name of the
 key unblocking it and the popped value.
 <p>
 When blocking, if a non-zero timeout is specified, the connection will unblock returning a nil
 special value if the specified amount of seconds passed without a push operation against at
 least one of the specified keys.
 <p>
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
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
         value is a nil multi bulk reply. Most connection values will return false or nil
         accordingly to the programming language used.
   */
  @Override
  public List<byte[]> blpop(final int timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public List<byte[]> blpop(final double timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.blpop(timeout, keys));
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
 If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
 connection performs a LPUSH or an RPUSH operation against one of the lists.
 <p>
 Once new data is present on one of the lists, the connection finally returns with the name of the
 key unblocking it and the popped value.
 <p>
 When blocking, if a non-zero timeout is specified, the connection will unblock returning a nil
 special value if the specified amount of seconds passed without a push operation against at
 least one of the specified keys.
 <p>
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
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
         value is a nil multi bulk reply. Most connection values will return false or nil
         accordingly to the programming language used.
   */
  @Override
  public List<byte[]> brpop(final int timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public List<byte[]> brpop(final double timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public List<byte[]> bzpopmax(final double timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public List<byte[]> bzpopmin(final double timeout, final byte[]... keys) {
    return connection.executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  /**
   * Request for authentication in a password protected Redis server. A Redis server can be
 instructed to require a password before to allow clients to issue commands. This is done using
 the requirepass directive in the Redis configuration file. If the password given by the connection
 is correct the server replies with an OK status code reply and starts accepting commands from
 the connection. Otherwise an error is returned and the clients needs to try a new password. Note
 that for the high performance nature of Redis it is possible to try a lot of passwords in
 parallel in very short time, so make sure to generate a strong and very long password so that
 this attack is infeasible.
   * @param password
   * @return Status code reply
   */
  @Override
  public String auth(final String password) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.AUTH, password);
    return connection.getStatusCodeReply();
  }

  /**
   * Request for authentication with a Redis Server that is using ACL where user are authenticated with
   * username and password.
   * See https://redis.io/topics/acl
   * @param user
   * @param password
   * @return OK
   */
  @Override
  public String auth(final String user, final String password) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.AUTH, user, password);
    return connection.getStatusCodeReply();
  }

  @Override
  public long zcount(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Set<byte[]> zdiff(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public Set<Tuple> zdiffWithScores(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  public long zdiffStore(final byte[] dstkey, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiffStore(dstkey, keys));
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
  public List<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max));
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
  public List<byte[]> zrangeByScore(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
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
  public List<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
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
  public List<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max,
      final double min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max,
      final byte[] min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
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
   */
  @Override
  public long zremrangeByRank(final byte[] key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByRank(key, start, stop));
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
  public long zremrangeByScore(final byte[] key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  /**
   * Add multiple sorted sets, This command is similar to ZUNIONSTORE, but instead of storing the
 resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   */
  @Override
  public Set<byte[]> zunion(final ZParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunion(params, keys));
  }

  /**
   * Add multiple sorted sets with scores, This command is similar to ZUNIONSTORE, but instead of storing the
 resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   */
  @Override
  public Set<Tuple> zunionWithScores(final ZParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionWithScores(params, keys));
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
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zunionstore(final byte[] dstkey, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionstore(dstkey, sets));
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
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }

  /**
   * Intersect multiple sorted sets, This command is similar to ZINTERSTORE, but instead of storing
 the resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   */
  @Override
  public Set<byte[]> zinter(final ZParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinter(params, keys));
  }

  /**
   * Intersect multiple sorted sets, This command is similar to ZINTERSTORE, but instead of storing
 the resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   */
  @Override
  public Set<Tuple> zinterWithScores(final ZParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterWithScores(params, keys));
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
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zinterstore(final byte[] dstkey, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterstore(dstkey, sets));
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
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public long zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public List<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public List<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByLex(key, min, max));
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
    connection.sendCommand(Command.SAVE);
    return connection.getStatusCodeReply();
  }

  /**
   * Asynchronously save the DB on disk.
   * <p>
 Save the DB in background. The OK code is immediately returned. Redis forks, the parent
 continues to server the clients, the child saves the DB on disk then exit. A connection my be able
 to check if the operation succeeded using the LASTSAVE command.
   * @return Status code reply
   */
  @Override
  public String bgsave() {
    connection.sendCommand(BGSAVE);
    return connection.getStatusCodeReply();
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
    connection.sendCommand(BGREWRITEAOF);
    return connection.getStatusCodeReply();
  }

  /**
   * Return the UNIX time stamp of the last successfully saving of the dataset on disk.
   * <p>
 Return the UNIX TIME of the last DB save executed with success. A connection may check if a
 {@link #bgsave() BGSAVE} command succeeded reading the LASTSAVE value, then issuing a BGSAVE
   * command and checking at regular intervals every N seconds if LASTSAVE changed.
   * @return Integer reply, specifically an UNIX time stamp.
   */
  @Override
  public long lastsave() {
    connection.sendCommand(LASTSAVE);
    return connection.getIntegerReply();
  }

  /**
   * Synchronously save the DB on disk, then shutdown the server.
   * <p>
   * Stop all the clients, save the DB, then quit the server. This commands makes sure that the DB
   * is switched off without the lost of any data. This is not guaranteed if the connection uses
   * simply {@link #save() SAVE} and then {@link #quit() QUIT} because other clients may alter the
   * DB data between the two commands.
   * @throws JedisException with the status code reply on error. On success nothing is thrown since
   *         the server quits and the connection is closed.
   */
  @Override
  public void shutdown() throws JedisException {
    connection.sendCommand(SHUTDOWN);
    try {
      throw new JedisException(connection.getStatusCodeReply());
    } catch (JedisConnectionException jce) {
      // expected
      connection.setBroken();
    }
  }

  @Override
  public void shutdown(final SaveMode saveMode) throws JedisException {
    connection.sendCommand(SHUTDOWN, saveMode.getRaw());
    try {
      throw new JedisException(connection.getStatusCodeReply());
    } catch (JedisConnectionException jce) {
      // expected
      connection.setBroken();
    }
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
    connection.sendCommand(Command.INFO);
    return connection.getBulkReply();
  }

  @Override
  public String info(final String section) {
    connection.sendCommand(Command.INFO, section);
    return connection.getBulkReply();
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
//    connection.monitor();
    connection.sendCommand(MONITOR);
    connection.getStatusCodeReply();
    jedisMonitor.proceed(connection);
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
    connection.sendCommand(SLAVEOF, encode(host), toByteArray(port));
    return connection.getStatusCodeReply();
  }

  @Override
  public String slaveofNoOne() {
    connection.sendCommand(SLAVEOF, NO.getRaw(), ONE.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public List<Object> roleBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ROLE);
    return BuilderFactory.RAW_OBJECT_LIST.build(connection.getOne());
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
    connection.sendCommand(CONFIG, Keyword.GET.getRaw(), pattern);
    return connection.getBinaryMultiBulkReply();
  }

  /**
   * Reset the stats returned by INFO
   */
  @Override
  public String configResetStat() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CONFIG, Keyword.RESETSTAT);
    return connection.getStatusCodeReply();
  }

  /**
   * The CONFIG REWRITE command rewrites the redis.conf file the server was started with, applying
   * the minimal changes needed to make it reflect the configuration currently used by the server,
   * which may be different compared to the original one because of the use of the CONFIG SET
   * command.
   * <p>
   * The rewrite is performed in a very conservative way:
   * <ul>
   * <li>Comments and the overall structure of the original redis.conf are preserved as much as
   * possible.</li>
   * <li>If an option already exists in the old redis.conf file, it will be rewritten at the same
   * position (line number).</li>
   * <li>If an option was not already present, but it is set to its default value, it is not added
   * by the rewrite process.</li>
   * <li>If an option was not already present, but it is set to a non-default value, it is appended
   * at the end of the file.</li>
   * <li>Non used lines are blanked. For instance if you used to have multiple save directives, but
   * the current configuration has fewer or none as you disabled RDB persistence, all the lines will
   * be blanked.</li>
   * </ul>
   * <p>
   * CONFIG REWRITE is also able to rewrite the configuration file from scratch if the original one
   * no longer exists for some reason. However if the server was started without a configuration
   * file at all, the CONFIG REWRITE will just return an error.
   * @return OK when the configuration was rewritten properly. Otherwise an error is returned.
   */
  @Override
  public String configRewrite() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CONFIG, Keyword.REWRITE);
    return connection.getStatusCodeReply();
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
   * <li>The save parameter is a list of space-separated integers. Every pair of integers specify
   * the time and number of changes limit to trigger a save. For instance the command CONFIG SET
   * save "3600 10 60 10000" will configure the server to issue a background saving of the RDB file
   * every 3600 seconds if there are at least 10 changes in the dataset, and every 60 seconds if
   * there are at least 10000 changes. To completely disable automatic snapshots just set the
   * parameter as an empty string.
   * <li>All the integer parameters representing memory are returned and accepted only using bytes
   * as unit.
   * </ul>
   * @param parameter
   * @param value
   * @return Status code reply
   */
  @Override
  public String configSet(final byte[] parameter, final byte[] value) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CONFIG, Keyword.SET.getRaw(), parameter, value);
    return connection.getStatusCodeReply();
  }

  @Override
  public long strlen(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strlen(key));
  }

  @Override
  public LCSMatchResult strAlgoLCSKeys(final byte[] keyA, final byte[] keyB, final StrAlgoLCSParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }

  public LCSMatchResult strAlgoLCSStrings(final byte[] strA, final byte[] strB, final StrAlgoLCSParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strAlgoLCSStrings(strA, strB, params));
  }

  @Override
  public long lpushx(final byte[] key, final byte[]... string) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpushx(key, string));
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
  public long persist(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.persist(key));
  }

  @Override
  public long rpushx(final byte[] key, final byte[]... string) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpushx(key, string));
  }

  @Override
  public byte[] echo(final byte[] string) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ECHO, string);
    return connection.getBinaryBulkReply();
  }

  @Override
  public long linsert(final byte[] key, final ListPosition where, final byte[] pivot,
      final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  /**
   * Pop a value from a list, push it to another list and return it; or block until one is available
   */
  @Override
  public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  /**
   * Sets or clears the bit at offset in the string value stored at key
   */
  @Override
  public boolean setbit(final byte[] key, final long offset, final boolean value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setbit(key, offset, value));
  }

  /**
   * Returns the bit value at offset in the string value stored at key
   */
  @Override
  public boolean getbit(final byte[] key, final long offset) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long bitpos(final byte[] key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  @Override
  public long bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public long setrange(final byte[] key, final long offset, final byte[] value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  public long publish(final byte[] channel, final byte[] message) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.publish(channel, message));
  }

  public void subscribe(BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceed(connection, channels);
    } finally {
      connection.rollbackTimeout();
    }
  }

  public void psubscribe(BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    } finally {
      connection.rollbackTimeout();
    }
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
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script, keys, args));
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
  public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(final byte[] script) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object evalsha(final byte[] sha1) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(final byte[] sha1, final List<byte[]> keys, final List<byte[]> args) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public String scriptFlush() {
    connection.sendCommand(SCRIPT, FLUSH);
    return connection.getStatusCodeReply();
  }

  @Override
  public String scriptFlush(final FlushMode flushMode) {
    connection.sendCommand(SCRIPT, FLUSH.getRaw(), flushMode.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public Boolean scriptExists(final byte[] sha1) {
    byte[][] a = new byte[1][];
    a[0] = sha1;
    return scriptExists(a).get(0);
  }

  @Override
  public List<Boolean> scriptExists(final byte[]... sha1) {
    connection.sendCommand(SCRIPT, joinParameters(Keyword.EXISTS.getRaw(), sha1));
    return BuilderFactory.BOOLEAN_LIST.build(connection.getOne());
  }

  @Override
  public byte[] scriptLoad(final byte[] script) {
    connection.sendCommand(SCRIPT, LOAD.getRaw(), script);
    return connection.getBinaryBulkReply();
  }

  @Override
  public String scriptKill() {
    connection.sendCommand(SCRIPT, KILL);
    return connection.getStatusCodeReply();
  }

  @Override
  public String slowlogReset() {
    connection.sendCommand(SLOWLOG, RESET);
    return connection.getBulkReply();
  }

  @Override
  public long slowlogLen() {
    connection.sendCommand(SLOWLOG, LEN);
    return connection.getIntegerReply();
  }

  @Override
  public List<Object> slowlogGetBinary() {
    connection.sendCommand(SLOWLOG, Keyword.GET);
    return connection.getObjectMultiBulkReply();
  }

  @Override
  public List<Object> slowlogGetBinary(final long entries) {
    connection.sendCommand(SLOWLOG, Keyword.GET.getRaw(), toByteArray(entries));
    return connection.getObjectMultiBulkReply();
  }

  @Override
  public Long objectRefcount(final byte[] key) {
    connection.sendCommand(OBJECT, REFCOUNT.getRaw(), key);
    return connection.getIntegerReply();
  }

  @Override
  public byte[] objectEncoding(final byte[] key) {
    connection.sendCommand(OBJECT, ENCODING.getRaw(), key);
    return connection.getBinaryBulkReply();
  }

  @Override
  public Long objectIdletime(final byte[] key) {
    connection.sendCommand(OBJECT, IDLETIME.getRaw(), key);
    return connection.getIntegerReply();
  }

  @Override
  public List<byte[]> objectHelpBinary() {
    connection.sendCommand(OBJECT, HELP);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public Long objectFreq(final byte[] key) {
    connection.sendCommand(OBJECT, FREQ.getRaw(), key);
    return connection.getIntegerReply();
  }

  @Override
  public long bitcount(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(final byte[] key, final long start, final long end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public byte[] dump(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(final byte[] key, final long ttl, final byte[] serializedValue) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(final byte[] key, final long ttl, final byte[] serializedValue,
      final RestoreParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  /**
   * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
   * the server. A key with an associated timeout is said to be volatile in Redis terminology.
   * <p>
   * Volatile keys are stored on disk like the other keys, the timeout is persistent too like all
   * the other aspects of the dataset. Saving a dataset containing expires and stopping the server
   * does not stop the flow of time as Redis stores on disk the time when the key will no longer be
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
  public long pexpire(final byte[] key, final long milliseconds) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pttl(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pttl(key));
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
    return connection.executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public byte[] memoryDoctorBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, DOCTOR);
    return connection.getBinaryBulkReply();
  }

  @Override
  public Long memoryUsage(final byte[] key) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, USAGE.getRaw(), key);
    return connection.getIntegerReply();
  }

  @Override
  public Long memoryUsage(final byte[] key, final int samples) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, USAGE.getRaw(), key, SAMPLES.getRaw(), toByteArray(samples));
    return connection.getIntegerReply();
  }

  @Override
  public String failover() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FAILOVER);
    connection.setTimeoutInfinite();
    try {
      return connection.getStatusCodeReply();
    } finally {
      connection.rollbackTimeout();
    }
  }

  @Override
  public String failover(FailoverParams failoverParams) {
    checkIsInMultiOrPipeline();
    CommandArguments args = new ClusterCommandArguments(FAILOVER).addParams(failoverParams);
    connection.sendCommand(args);
    connection.setTimeoutInfinite();
    try {
      return connection.getStatusCodeReply();
    } finally {
      connection.rollbackTimeout();
    }
  }

  @Override
  public String failoverAbort() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(FAILOVER, ABORT);
    return connection.getStatusCodeReply();
  }

  @Override
  public byte[] aclWhoAmIBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, WHOAMI);
    return connection.getBinaryBulkReply();
  }

  @Override
  public byte[] aclGenPassBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, GENPASS);
    return connection.getBinaryBulkReply();
  }

  @Override
  public byte[] aclGenPassBinary(int bits) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, GENPASS.getRaw(), toByteArray(bits));
    return connection.getBinaryBulkReply();
  }

  @Override
  public List<byte[]> aclListBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LIST);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclUsersBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, USERS);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public AccessControlUser aclGetUser(byte[] name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, GETUSER.getRaw(), name);
    return BuilderFactory.ACCESS_CONTROL_USER.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public String aclSetUser(byte[] name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, SETUSER.getRaw(), name);
    return connection.getStatusCodeReply();
  }

  @Override
  public String aclSetUser(byte[] name, byte[]... keys) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, joinParameters(SETUSER.getRaw(), name, keys));
    return connection.getStatusCodeReply();
  }

  @Override
  public long aclDelUser(byte[] name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, DELUSER.getRaw(), name);
    return connection.getIntegerReply();
  }

  @Override
  public long aclDelUser(byte[] name, byte[]... names) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, joinParameters(DELUSER.getRaw(), name, names));
    return connection.getIntegerReply();
  }

  @Override
  public List<byte[]> aclCatBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, CAT);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclCat(byte[] category) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, CAT.getRaw(), category);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclLogBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOG);
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public List<byte[]> aclLogBinary(int limit) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOG.getRaw(), toByteArray(limit));
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public String aclLogReset() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOG.getRaw(), RESET.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public String clientKill(final byte[] ipPort) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, KILL.getRaw(), ipPort);
    return this.connection.getStatusCodeReply();
  }

  @Override
  public String clientKill(final String ip, final int port) {
    return clientKill(ip + ':' + port);
  }

  @Override
  public long clientKill(ClientKillParams params) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, joinParameters(KILL.getRaw(), params.getByteParams()));
    return this.connection.getIntegerReply();
  }

  @Override
  public byte[] clientGetnameBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, GETNAME);
    return connection.getBinaryBulkReply();
  }

  @Override
  public byte[] clientListBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, LIST);
    return connection.getBinaryBulkReply();
  }

  @Override
  public byte[] clientListBinary(ClientType type) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, LIST.getRaw(), type.getRaw());
    return connection.getBinaryBulkReply();
  }

  @Override
  public byte[] clientListBinary(final long... clientIds) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, clientListParams(clientIds));
    return connection.getBinaryBulkReply();
  }

  private byte[][] clientListParams(final long... clientIds) {
    final byte[][] params = new byte[2 + clientIds.length][];
    int index = 0;
    params[index++] = Keyword.LIST.getRaw();
    params[index++] = ID.getRaw();
    for (final long clientId : clientIds) {
      params[index++] = toByteArray(clientId);
    }
    return params;
  }

  @Override
  public byte[] clientInfoBinary() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, Keyword.INFO);
    return connection.getBinaryBulkReply();
  }

  @Override
  public String clientSetname(final byte[] name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, SETNAME.getRaw(), name);
    return connection.getBulkReply();
  }

  @Override
  public long clientId() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, ID);
    return connection.getIntegerReply();
  }

  /**
   * Unblock a connection blocked in a blocking command from a different connection.
   * @param clientId
   */
  @Override
  public long clientUnblock(final long clientId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, UNBLOCK.getRaw(), toByteArray(clientId));
    return connection.getIntegerReply();
  }

  /**
   * Unblock a connection blocked in a blocking command from a different connection.
   * @param clientId
   * @param unblockType
   */
  @Override
  public long clientUnblock(final long clientId, final UnblockType unblockType) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, UNBLOCK.getRaw(), toByteArray(clientId), unblockType.getRaw());
    return connection.getIntegerReply();
  }

  @Override
  public String clientPause(final long timeout) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, PAUSE.getRaw(), toByteArray(timeout));
    return connection.getBulkReply();
  }

  @Override
  public String clientPause(final long timeout, final ClientPauseMode mode) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, PAUSE.getRaw(), toByteArray(timeout), mode.getRaw());
    return connection.getBulkReply();
  }

  public List<String> time() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.TIME);
    return connection.getMultiBulkReply();
  }

  @Override
  public String migrate(final String host, final int port, final byte[] key,
      final int destinationDb, final int timeout) {
    checkIsInMultiOrPipeline();
    CommandArguments args = new CommandArguments(MIGRATE).add(host).add(port).key(key).add(destinationDb).add(timeout);
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  @Override
  public String migrate(final String host, final int port, final int destinationDB,
      final int timeout, final MigrateParams params, final byte[]... keys) {
    checkIsInMultiOrPipeline();
    CommandArguments args = new CommandArguments(MIGRATE).add(host).add(port).add(new byte[0]).add(destinationDB)
        .add(timeout).addParams(params).add(Keyword.KEYS).keys((Object[]) keys);
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  @Override
  public String migrate(String host, int port, byte[] key, int timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.migrate(host, port, timeout, params, keys));
  }

  @Override
  public long waitReplicas(final int replicas, final long timeout) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(WAIT, toByteArray(replicas), toByteArray(timeout));
    return connection.getIntegerReply();
  }

  @Override
  public long pfadd(final byte[] key, final byte[]... elements) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public long pfcount(final byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public String pfmerge(final byte[] destkey, final byte[]... sourcekeys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long pfcount(final byte[]... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfcount(keys));
  }

  @Override
  public ScanResult<byte[]> scan(final byte[] cursor) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params, final byte[] type) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor,
      final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public long geoadd(final byte[] key, final double longitude, final double latitude,
      final byte[] member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(final byte[] key, final GeoAddParams params, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2,
      final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<byte[]> geohash(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(final byte[] key, final byte[]... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public long georadiusStore(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param,
      final GeoRadiusStoreParam storeParam) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public long georadiusByMemberStore(final byte[] key, final byte[] member, final double radius,
      final GeoUnit unit, final GeoRadiusParam param, final GeoRadiusStoreParam storeParam) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public List<Long> bitfield(final byte[] key, final byte[]... arguments) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(byte[] key, final byte[]... arguments) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public long hstrlen(final byte[] key, final byte[] field) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public List<byte[]> xread(XReadParams xReadParams, Entry<byte[], byte[]>... streams) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public List<byte[]> xreadGroup(byte[] groupname, byte[] consumer,
      XReadGroupParams xReadGroupParams, Entry<byte[], byte[]>... streams) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xreadGroup(groupname, consumer, xReadGroupParams, streams));
  }

  @Override
  public byte[] xadd(final byte[] key, final XAddParams params, final Map<byte[], byte[]> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public long xlen(byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<byte[]> xrange(byte[] key, byte[] start, byte[] end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<byte[]> xrange(byte[] key, byte[] start, byte[] end, int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start, int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(byte[] key, byte[] group, byte[]... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(byte[] key, byte[] consumer, byte[] id, boolean makeStream) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupCreate(key, consumer, id, makeStream));
  }

  @Override
  public String xgroupSetID(byte[] key, byte[] consumer, byte[] id) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupSetID(key, consumer, id));
  }

  @Override
  public long xgroupDestroy(byte[] key, byte[] consumer) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupDestroy(key, consumer));
  }

  @Override
  public long xgroupDelConsumer(byte[] key, byte[] consumer, byte[] consumerName) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupDelConsumer(key, consumer, consumerName));
  }

  @Override
  public long xdel(byte[] key, byte[]... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(byte[] key, long maxLen, boolean approximateLength) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xtrim(key, maxLen, approximateLength));
  }

  @Override
  public long xtrim(byte[] key, XTrimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public List<Object> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count,
      byte[] consumername) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public Object xpending(final byte[] key, final byte[] groupname) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public List<Object> xpending(final byte[] key, final byte[] groupname, final XPendingParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumername, long minIdleTime,
      XClaimParams params, byte[]... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumername, long minIdleTime,
      XClaimParams params, byte[]... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xautoclaim(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params));
  }

  @Override
  public Object xinfoStream(byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public List<Object> xinfoGroup(byte[] key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public List<Object> xinfoConsumers(byte[] key, byte[] group) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  public Object sendCommand(ProtocolCommand cmd, byte[]... args) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(cmd, args);
    return connection.getOne();
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, byte[]... args) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(cmd, args);
    connection.setTimeoutInfinite();
    try {
      return connection.getOne();
    } finally {
      connection.rollbackTimeout();
    }
  }

  public Object sendCommand(ProtocolCommand cmd) {
    return sendCommand(cmd, DUMMY_ARRAY);
  }

  /**
   * COPY source destination [DB destination-db] [REPLACE]
   *
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param db
   * @param replace
   * @return
   */
  @Override
  public boolean copy(String srcKey, String dstKey, int db, boolean replace) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.copy(srcKey, dstKey, db, replace));
  }

  /**
   * COPY source destination [DB destination-db] [REPLACE]
   *
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param replace
   * @return
   */
  @Override
  public boolean copy(String srcKey, String dstKey, boolean replace) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  /**
   * Works same as <tt>ping()</tt> but returns argument message instead of <tt>PONG</tt>.
   * @param message
   * @return message
   */
  @Override
  public String ping(final String message) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(Command.PING, message);
    return connection.getBulkReply();
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
  public String set(final String key, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.set(key, value));
  }

  /**
   * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
   * GB).
   * @param key
   * @param value
   * @param params NX|XX, NX -- Only set the key if it does not already exist. XX -- Only set the
   *          key if it already exist. EX|PX, expire time units: EX = seconds; PX = milliseconds
   * @return Status code reply
   */
  @Override
  public String set(final String key, final String value, final SetParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.set(key, value, params));
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
  public String get(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.get(key));
  }

  /**
   * Get the value of key and delete the key. This command is similar to GET, except for the fact
   * that it also deletes the key on success (if and only if the key's value type is a string).
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return the value of key
   * @since Redis 6.2
   */
  @Override
  public String getDel(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getDel(key));
  }

  @Override
  public String getEx(String key, GetExParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getEx(key, params));
  }

  /**
   * Test if the specified keys exist. The command returns the number of keys exist.
   * Time complexity: O(N)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys exist,
   *         0 if none of the specified keys exist.
   */
  @Override
  public long exists(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.exists(keys));
  }

  /**
   * Test if the specified key exists. The command returns true if the key exists, otherwise false is
   * returned. Note that even keys set with an empty string as value will return true. Time
   * complexity: O(1)
   * @param key
   * @return Boolean reply, true if the key exists, otherwise false
   */
  @Override
  public boolean exists(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.exists(key));
  }

  /**
   * Remove the specified keys. If a given key does not exist no operation is performed for this
   * key. The command returns the number of keys removed. Time complexity: O(1)
   * @param keys
   * @return Integer reply, specifically: an integer greater than 0 if one or more keys were removed
   *         0 if none of the specified key existed
   */
  @Override
  public long del(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.del(keys));
  }

  @Override
  public long del(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.del(key));
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
  public long unlink(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.unlink(keys));
  }

  @Override
  public long unlink(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.unlink(key));
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
  public String type(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.type(key));
  }

  @Override
  public Set<String> keys(final String pattern) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.keys(pattern));
  }

  /**
   * Return a randomly selected key from the currently selected DB.
   * <p>
   * Time complexity: O(1)
   * @return Singe line reply, specifically the randomly selected key or an empty string is the
   *         database is empty
   */
  @Override
  public String randomKey() {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.randomKey());
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
  public String rename(final String oldkey, final String newkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rename(oldkey, newkey));
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
  public long renamenx(final String oldkey, final String newkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.renamenx(oldkey, newkey));
  }

  /**
   * Set a timeout on the specified key. After the timeout the key will be automatically deleted by
   * the server. A key with an associated timeout is said to be volatile in Redis terminology.
   * <p>
   * Volatile keys are stored on disk like the other keys, the timeout is persistent too like all
   * the other aspects of the dataset. Saving a dataset containing expires and stopping the server
   * does not stop the flow of time as Redis stores on disk the time when the key will no longer be
   * available as Unix time, and not the remaining seconds.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link #persist(String) PERSIST} command.
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
  public long expire(final String key, final long seconds) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.expire(key, seconds));
  }

  /**
   * EXPIREAT works exactly like {@link #expire(String, int) EXPIRE} but instead to get the number
   * of seconds representing the Time To Live of the key as a second argument (that is a relative
   * way of specifying the TTL), it takes an absolute one in the form of a UNIX timestamp (Number of
   * seconds elapsed since 1 Gen 1970).
   * <p>
   * EXPIREAT was introduced in order to implement the Append Only File persistence mode so that
   * EXPIRE commands are automatically translated into EXPIREAT commands for the append only file.
   * Of course EXPIREAT can also used by programmers that need a way to simply specify that a given
   * key should expire at a given time in the future.
   * <p>
   * Since Redis 2.1.3 you can update the value of the timeout of a key already having an expire
   * set. It is also possible to undo the expire at all turning the key into a normal key using the
   * {@link #persist(String) PERSIST} command.
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
  public long expireAt(final String key, final long unixTime) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.expireAt(key, unixTime));
  }

  /**
   * The TTL command returns the remaining time to live in seconds of a key that has an
   * {@link #expire(String, int) EXPIRE} set. This introspection capability allows a Redis connection to
 check how many seconds a given key will continue to be part of the dataset.
   * @param key
   * @return Integer reply, returns the remaining time to live in seconds of a key that has an
   *         EXPIRE. In Redis 2.6 or older, if the Key does not exists or does not have an
   *         associated expire, -1 is returned. In Redis 2.8 or newer, if the Key does not have an
   *         associated expire, -1 is returned or if the Key does not exists, -2 is returned.
   */
  @Override
  public long ttl(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.ttl(key));
  }

  /**
   * Alters the last access time of a key(s). A key is ignored if it does not exist.
   * Time complexity: O(N) where N is the number of keys that will be touched.
   * @param keys
   * @return Integer reply: The number of keys that were touched.
   */
  @Override
  public long touch(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.touch(keys));
  }

  @Override
  public long touch(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.touch(key));
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
  public long move(final String key, final int dbIndex) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MOVE, encode(key), toByteArray(dbIndex));
    return connection.getIntegerReply();
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
  public String getSet(final String key, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getSet(key, value));
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
  public List<String> mget(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.mget(keys));
  }

  /**
   * SETNX works exactly like {@link #set(String, String) SET} with the only difference that if the
   * key already exists no operation is performed. SETNX actually means "SET if Not eXists".
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param value
   * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
   */
  @Override
  public long setnx(final String key, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setnx(key, value));
  }

  /**
   * The command is exactly equivalent to the following group of commands:
   * {@link #set(String, String) SET} + {@link #expire(String, int) EXPIRE}. The operation is
   * atomic.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param seconds
   * @param value
   * @return Status code reply
   */
  @Override
  public String setex(final String key, final long seconds, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setex(key, seconds, value));
  }

  /**
   * Set the the respective keys to the respective values. MSET will replace old values with new
   * values, while {@link #msetnx(String...) MSETNX} will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
 Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
 are modified, another connection talking to Redis can either see the changes to both A and B at
 once, or no modification at all.
   * @see #msetnx(String...)
   * @param keysvalues
   * @return Status code reply Basically +OK as MSET can't fail
   */
  @Override
  public String mset(final String... keysvalues) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.mset(keysvalues));
  }

  /**
   * Set the the respective keys to the respective values. {@link #mset(String...) MSET} will
   * replace old values with new values, while MSETNX will not perform any operation at all even if
   * just a single key already exists.
   * <p>
   * Because of this semantic MSETNX can be used in order to set different keys representing
   * different fields of an unique logic object in a way that ensures that either all the fields or
   * none at all are set.
   * <p>
 Both MSET and MSETNX are atomic operations. This means that for instance if the keys A and B
 are modified, another connection talking to Redis can either see the changes to both A and B at
 once, or no modification at all.
   * @see #mset(String...)
   * @param keysvalues
   * @return Integer reply, specifically: 1 if the all the keys were set 0 if no key was set (at
   *         least one key already existed)
   */
  @Override
  public long msetnx(final String... keysvalues) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.msetnx(keysvalues));
  }

  /**
   * IDECRBY work just like {@link #decr(String) INCR} but instead to decrement by 1 the decrement
   * is integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incr(String)
   * @see #decr(String)
   * @see #incrBy(String, long)
   * @param key
   * @param decrement
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public long decrBy(final String key, final long decrement) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.decrBy(key, decrement));
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
   * @see #incr(String)
   * @see #incrBy(String, long)
   * @see #decrBy(String, long)
   * @param key
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public long decr(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.decr(key));
  }

  /**
   * INCRBY work just like {@link #incr(String) INCR} but instead to increment by 1 the increment is
   * integer.
   * <p>
   * INCR commands are limited to 64 bit signed integers.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "integer" types.
   * Simply the string stored at the key is parsed as a base 10 64 bit signed integer, incremented,
   * and then converted back as a string.
   * <p>
   * Time complexity: O(1)
   * @see #incr(String)
   * @see #decr(String)
   * @see #decrBy(String, long)
   * @param key
   * @param increment
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public long incrBy(final String key, final long increment) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incrBy(key, increment));
  }

  /**
   * INCRBYFLOAT
   * <p>
   * INCRBYFLOAT commands are limited to double precision floating point values.
   * <p>
   * Note: this is actually a string operation, that is, in Redis there are not "double" types.
   * Simply the string stored at the key is parsed as a base double precision floating point value,
   * incremented, and then converted back as a string. There is no DECRYBYFLOAT but providing a
   * negative value will work as expected.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param increment
   * @return Double reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public double incrByFloat(final String key, final double increment) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incrByFloat(key, increment));
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
   * @see #incrBy(String, long)
   * @see #decr(String)
   * @see #decrBy(String, long)
   * @param key
   * @return Integer reply, this commands will reply with the new value of key after the increment.
   */
  @Override
  public long incr(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.incr(key));
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
  public long append(final String key, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.append(key, value));
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
  public String substr(final String key, final int start, final int end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.substr(key, start, end));
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
  public long hset(final String key, final String field, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(final String key, final Map<String, String> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hset(key, hash));
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
  public String hget(final String key, final String field) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hget(key, field));
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
  public long hsetnx(final String key, final String field, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hsetnx(key, field, value));
  }

  /**
   * Set the respective fields to the respective values. HMSET replaces old values with new values.
   * <p>
   * If key does not exist, a new key holding a hash is created.
   * <p>
   * <b>Time complexity:</b> O(N) (with N being the number of fields)
   * @param key
   * @param hash
   * @return Return OK or Exception if hash is empty
   */
  @Override
  public String hmset(final String key, final Map<String, String> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hmset(key, hash));
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
  public List<String> hmget(final String key, final String... fields) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hmget(key, fields));
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
  public long hincrBy(final String key, final String field, final long value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hincrBy(key, field, value));
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
  public double hincrByFloat(final String key, final String field, final double value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  /**
   * Test for existence of a specified field in a hash. <b>Time complexity:</b> O(1)
   * @param key
   * @param field
   * @return Return true if the hash stored at key contains the specified field. Return false if the key is
   *         not found or the field is not present.
   */
  @Override
  public boolean hexists(final String key, final String field) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hexists(key, field));
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
  public long hdel(final String key, final String... fields) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hdel(key, fields));
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
  public long hlen(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hlen(key));
  }

  /**
   * Return all the fields in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields names contained into a hash.
   */
  @Override
  public Set<String> hkeys(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hkeys(key));
  }

  /**
   * Return all the values in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields values contained into a hash.
   */
  @Override
  public List<String> hvals(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hvals(key));
  }

  /**
   * Return all the fields and associated values in a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the total number of entries
   * @param key
   * @return All the fields and values contained into a hash.
   */
  @Override
  public Map<String, String> hgetAll(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hgetAll(key));
  }

  /**
   * Get one random field from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @return one random field from a hash.
   */
  @Override
  public String hrandfield(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfield(key));
  }

  /**
   * Get multiple random fields from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @param count
   * @return multiple random fields from a hash.
   */
  @Override
  public List<String> hrandfield(final String key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfield(key, count));
  }

  /**
   * Get one or multiple random fields with values from a hash.
   * <p>
   * <b>Time complexity:</b> O(N), where N is the number of fields returned
   * @param key
   * @param count
   * @return one or multiple random fields with values from a hash.
   */
  @Override
  public Map<String, String> hrandfieldWithValues(final String key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public long rpush(final String key, final String... strings) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpush(key, strings));
  }

  /**
   * Add the string value to the head (LPUSH) or tail (RPUSH) of the list stored at key. If the key
   * does not exist an empty list is created just before the append operation. If the key exists but
   * is not a List an error is returned.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @param strings
   * @return Integer reply, specifically, the number of elements inside the list after the push
   *         operation.
   */
  @Override
  public long lpush(final String key, final String... strings) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpush(key, strings));
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
  public long llen(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.llen(key));
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
  public List<String> lrange(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lrange(key, start, stop));
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
  public String ltrim(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.ltrim(key, start, stop));
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
  public String lindex(final String key, final long index) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lindex(key, index));
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
   * @see #lindex(String, long)
   * @param key
   * @param index
   * @param value
   * @return Status code reply
   */
  @Override
  public String lset(final String key, final long index, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lset(key, index, value));
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
  public long lrem(final String key, final long count, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lrem(key, count, value));
  }

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" LPOP will return "a" and the list will become
   * "b","c".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned.
   * @see #rpop(String)
   * @param key
   * @return Bulk reply
   */
  @Override
  public String lpop(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<String> lpop(final String key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public Long lpos(final String key, final String element) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(final String key, final String element, final LPosParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(final String key, final String element, final LPosParams params,
      final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpos(key, element, params, count));
  }

  /**
   * Atomically return and remove the first (LPOP) or last (RPOP) element of the list. For example
   * if the list contains the elements "a","b","c" RPOP will return "c" and the list will become
   * "a","b".
   * <p>
   * If the key does not exist or the list is already empty the special value 'nil' is returned.
   * @see #lpop(String)
   * @param key
   * @return Bulk reply
   */
  @Override
  public String rpop(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<String> rpop(final String key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpop(key, count));
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
  public String rpoplpush(final String srckey, final String dstkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpoplpush(srckey, dstkey));
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
  public long sadd(final String key, final String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sadd(key, members));
  }

  /**
   * Return all the members (elements) of the set value stored at key. This is just syntax glue for
   * {@link #sinter(String...) SINTER}.
   * <p>
   * Time complexity O(N)
   * @param key
   * @return Multi bulk reply
   */
  @Override
  public Set<String> smembers(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.smembers(key));
  }

  /**
   * Remove the specified member from the set value stored at key. If member was not a member of the
   * set no operation is performed. If key does not hold a set value an error is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param members
   * @return Integer reply, specifically: 1 if the new element was removed 0 if the new element was
   *         not a member of the set
   */
  @Override
  public long srem(final String key, final String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.srem(key, members));
  }

  /**
   * Remove a random element from a Set returning it as return value. If the Set is empty or the key
   * does not exist, a nil object is returned.
   * <p>
   * The {@link #srandmember(String)} command does a similar work but the returned element is not
   * removed from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @return Bulk reply
   */
  @Override
  public String spop(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<String> spop(final String key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.spop(key, count));
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
  public long smove(final String srckey, final String dstkey, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.smove(srckey, dstkey, member));
  }

  /**
   * Return the set cardinality (number of elements). If the key does not exist 0 is returned, like
   * for empty sets.
   * @param key
   * @return Integer reply, specifically: the cardinality (number of elements) of the set as an
   *         integer.
   */
  @Override
  public long scard(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scard(key));
  }

  /**
   * Return true if member is a member of the set stored at key, otherwise false is returned.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param member
   * @return Boolean reply, specifically: true if the element is a member of the set false if the
   *         element is not a member of the set OR if the key does not exist
   */
  @Override
  public boolean sismember(final String key, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sismember(key, member));
  }

  /**
   * Returns whether each member is a member of the set stored at key.
   * <p>
   * Time complexity O(N) where N is the number of elements being checked for membership
   * @param key
   * @param members
   * @return List representing the membership of the given elements, in the same order as they are
   *         requested.
   */
  @Override
  public List<Boolean> smismember(final String key, final String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.smismember(key, members));
  }

  /**
   * Return the members of a set resulting from the intersection of all the sets hold at the
   * specified keys. Like in {@link #lrange(String, long, long) LRANGE} the result is sent to the
 connection as a multi-bulk reply (see the protocol specification for more information). If just a
 single key is specified, then this command produces the same result as
 {@link #smembers(String) SMEMBERS}. Actually SMEMBERS is just syntax sugar for SINTER.
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
  public Set<String> sinter(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sinter(keys));
  }

  /**
   * This command works exactly like {@link #sinter(String...) SINTER} but instead of being returned
   * the resulting set is stored as dstkey.
   * <p>
   * Time complexity O(N*M) worst case where N is the cardinality of the smallest set and M the
   * number of sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public long sinterstore(final String dstkey, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sinterstore(dstkey, keys));
  }

  /**
   * Return the members of a set resulting from the union of all the sets hold at the specified
   * keys. Like in {@link #lrange(String, long, long) LRANGE} the result is sent to the connection as a
 multi-bulk reply (see the protocol specification for more information). If just a single key is
 specified, then this command produces the same result as {@link #smembers(String) SMEMBERS}.
   * <p>
   * Non existing keys are considered like empty sets.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param keys
   * @return Multi bulk reply, specifically the list of common elements.
   */
  @Override
  public Set<String> sunion(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sunion(keys));
  }

  /**
   * This command works exactly like {@link #sunion(String...) SUNION} but instead of being returned
   * the resulting set is stored as dstkey. Any existing value in dstkey will be over-written.
   * <p>
   * Time complexity O(N) where N is the total number of elements in all the provided sets
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public long sunionstore(final String dstkey, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sunionstore(dstkey, keys));
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
  public Set<String> sdiff(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sdiff(keys));
  }

  /**
   * This command works exactly like {@link #sdiff(String...) SDIFF} but instead of being returned
   * the resulting set is stored in dstkey.
   * @param dstkey
   * @param keys
   * @return Status code reply
   */
  @Override
  public long sdiffstore(final String dstkey, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sdiffstore(dstkey, keys));
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
  public String srandmember(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.srandmember(key));
  }

  /**
   * Return a random elements from a Set, without removing the elements. If the Set is empty or the
   * key does not exist, an empty list is returned.
   * <p>
   * The SPOP command does a similar work but the returned elements is popped (removed) from the Set.
   * <p>
   * Time complexity O(1)
   * @param key
   * @param count if positive, return an array of distinct elements.
   *        If negative the behavior changes and the command is allowed to
   *        return the same element multiple times  
   * @return list of elements
   */
  @Override
  public List<String> srandmember(final String key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.srandmember(key, count));
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
  public long zadd(final String key, final double score, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(final String key, final double score, final String member,
      final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(final String key, final Map<String, Double> scoreMembers) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(final String key, final double score, final String member, final ZAddParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public Set<String> zdiff(String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiff(keys));
  }

  @Override
  public Set<Tuple> zdiffWithScores(String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  public long zdiffStore(final String dstkey, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zdiffStore(dstkey, keys));
  }

  @Override
  public List<String> zrange(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrange(key, start, stop));
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
  public long zrem(final String key, final String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrem(key, members));
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
  public double zincrby(final String key, final double increment, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(final String key, final double increment, final String member,
      final ZIncrByParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  /**
   * Return the rank (or index) of member in the sorted set at key, with scores being ordered from
   * low to high.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))
   * @see #zrevrank(String, String)
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  @Override
  public Long zrank(final String key, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrank(key, member));
  }

  /**
   * Return the rank (or index) of member in the sorted set at key, with scores being ordered from
   * high to low.
   * <p>
   * When the given member does not exist in the sorted set, the special value 'nil' is returned.
   * The returned rank (or index) of the member is 0-based for both commands.
   * <p>
   * <b>Time complexity:</b>
   * <p>
   * O(log(N))
   * @see #zrank(String, String)
   * @param key
   * @param member
   * @return Integer reply or a nil bulk reply, specifically: the rank of the element as an integer
   *         reply if the element exists. A nil bulk reply if there is no such element.
   */
  @Override
  public Long zrevrank(final String key, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public List<String> zrevrange(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public List<Tuple> zrangeWithScores(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public List<Tuple> zrevrangeWithScores(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public String zrandmember(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public List<String> zrandmember(final String key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public List<Tuple> zrandmemberWithScores(final String key, final long count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrandmemberWithScores(key, count));
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
  public long zcard(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcard(key));
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
  public Double zscore(final String key, final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zscore(key, member));
  }

  /**
   * Returns the scores associated with the specified members in the sorted set stored at key. For
   * every member that does not exist in the sorted set, a nil value is returned.
   * <p>
   * <b>Time complexity:</b> O(N) where N is the number of members being requested.
   * @param key
   * @param members
   * @return the scores
   */
  @Override
  public List<Double> zmscore(final String key, final String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public List<Tuple> zpopmax(final String key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public List<Tuple> zpopmin(final String key, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zpopmin(key, count));
  }

  public String watch(final String... keys) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(WATCH, keys);
//    return connection.getStatusCodeReply();
    String status = connection.getStatusCodeReply();
    isInWatch = true;
    return status;
  }

  /**
   * Sort a Set or a List.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set value at key. By default sorting is
   * numeric with elements being compared as double precision floating point numbers. This is the
   * simplest form of SORT.
   * @see #sort(String, String)
   * @see #sort(String, SortingParams)
   * @see #sort(String, SortingParams, String)
   * @param key
   * @return Assuming the Set/List at key contains a list of numbers, the return value will be the
   *         list of numbers ordered from the smallest to the biggest number.
   */
  @Override
  public List<String> sort(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key));
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
   * @see #sort(String)
   * @see #sort(String, SortingParams, String)
   * @param key
   * @param sortingParameters
   * @return a list of sorted elements.
   */
  @Override
  public List<String> sort(final String key, final SortingParams sortingParameters) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key, sortingParameters));
  }

  /**
   * Sort a Set or a List accordingly to the specified parameters and store the result at dstkey.
   * @see #sort(String, SortingParams)
   * @see #sort(String)
   * @see #sort(String, String)
   * @param key
   * @param sortingParameters
   * @param dstkey
   * @return The number of elements of the list at dstkey.
   */
  @Override
  public long sort(final String key, final SortingParams sortingParameters, final String dstkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key, sortingParameters, dstkey));
  }

  /**
   * Sort a Set or a List and Store the Result at dstkey.
   * <p>
   * Sort the elements contained in the List, Set, or Sorted Set value at key and store the result
   * at dstkey. By default sorting is numeric with elements being compared as double precision
   * floating point numbers. This is the simplest form of SORT.
   * @see #sort(String)
   * @see #sort(String, SortingParams)
   * @see #sort(String, SortingParams, String)
   * @param key
   * @param dstkey
   * @return The number of elements of the list at dstkey.
   */
  @Override
  public long sort(final String key, final String dstkey) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sort(key, dstkey));
  }

  @Override
  public String lmove(final String srcKey, final String dstKey, final ListDirection from,
      final ListDirection to) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public String blmove(final String srcKey, final String dstKey, final ListDirection from,
      final ListDirection to, final double timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
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
 If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
 connection performs a LPUSH or an RPUSH operation against one of the lists.
 <p>
 Once new data is present on one of the lists, the connection finally returns with the name of the
 key unblocking it and the popped value.
 <p>
 When blocking, if a non-zero timeout is specified, the connection will unblock returning a nil
 special value if the specified amount of seconds passed without a push operation against at
 least one of the specified keys.
 <p>
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
   * @see #brpop(int, String...)
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
         value is a nil multi bulk reply. Most connection values will return false or nil
         accordingly to the programming language used.
   */
  @Override
  public List<String> blpop(final int timeout, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public KeyedListElement blpop(final double timeout, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blpop(timeout, keys));
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
 If none of the specified keys exist or contain non empty lists, BLPOP blocks until some other
 connection performs a LPUSH or an RPUSH operation against one of the lists.
 <p>
 Once new data is present on one of the lists, the connection finally returns with the name of the
 key unblocking it and the popped value.
 <p>
 When blocking, if a non-zero timeout is specified, the connection will unblock returning a nil
 special value if the specified amount of seconds passed without a push operation against at
 least one of the specified keys.
 <p>
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
   * @see #blpop(int, String...)
   * @param timeout
   * @param keys
   * @return BLPOP returns a two-elements array via a multi bulk reply in order to return both the
   *         unblocking key and the popped value.
   *         <p>
         When a non-zero timeout is specified, and the BLPOP operation timed out, the return
         value is a nil multi bulk reply. Most connection values will return false or nil
         accordingly to the programming language used.
   */
  @Override
  public List<String> brpop(final int timeout, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public KeyedListElement brpop(final double timeout, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public KeyedZSetElement bzpopmax(double timeout, String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public KeyedZSetElement bzpopmin(double timeout, String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public List<String> blpop(final int timeout, final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public KeyedListElement blpop(double timeout, String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public List<String> brpop(final int timeout, final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public KeyedListElement brpop(double timeout, String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public long zcount(final String key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zcount(key, min, max));
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead of returning the
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
   * @see #zrangeByScore(String, double, double)
   * @see #zrangeByScore(String, double, double, int, int)
   * @see #zrangeByScoreWithScores(String, double, double)
   * @see #zrangeByScoreWithScores(String, String, String)
   * @see #zrangeByScoreWithScores(String, double, double, int, int)
   * @see #zcount(String, double, double)
   * @param key
   * @param min a double or Double.NEGATIVE_INFINITY for "-inf"
   * @param max a double or Double.POSITIVE_INFINITY for "+inf"
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public List<String> zrangeByScore(final String key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public List<String> zrangeByScore(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead of returning the
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
   * @see #zrangeByScore(String, double, double)
   * @see #zrangeByScore(String, double, double, int, int)
   * @see #zrangeByScoreWithScores(String, double, double)
   * @see #zrangeByScoreWithScores(String, double, double, int, int)
   * @see #zcount(String, double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public List<String> zrangeByScore(final String key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public List<String> zrangeByScore(final String key, final String min, final String max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead of returning the
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
   * @see #zrangeByScore(String, double, double)
   * @see #zrangeByScore(String, double, double, int, int)
   * @see #zrangeByScoreWithScores(String, double, double)
   * @see #zrangeByScoreWithScores(String, double, double, int, int)
   * @see #zcount(String, double, double)
   * @param key
   * @param min
   * @param max
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public List<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  /**
   * Return the all the elements in the sorted set at key with a score between min and max
   * (including elements with score equal to min or max).
   * <p>
   * The elements having the same score are returned sorted lexicographically as ASCII strings (this
   * follows from a property of Redis sorted sets and does not involve further computation).
   * <p>
   * Using the optional {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's possible
   * to get only a range of the matching elements in an SQL-alike way. Note that if offset is large
   * the commands needs to traverse the list for offset elements and this adds up to the O(M)
   * figure.
   * <p>
   * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
   * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead of returning the
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
   * @see #zrangeByScore(String, double, double)
   * @see #zrangeByScore(String, double, double, int, int)
   * @see #zrangeByScoreWithScores(String, double, double)
   * @see #zrangeByScoreWithScores(String, double, double, int, int)
   * @see #zcount(String, double, double)
   * @param key
   * @param min
   * @param max
   * @param offset
   * @param count
   * @return Multi bulk reply specifically a list of elements in the specified score range.
   */
  @Override
  public List<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(final String key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrevrangeByScore(final String key, final String max, final String min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public List<String> zrevrangeByScore(final String key, final double max, final double min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
      final double min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
      final String min, final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public List<String> zrevrangeByScore(final String key, final String max, final String min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public List<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
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
  public long zremrangeByRank(final String key, final long start, final long stop) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByRank(key, start, stop));
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
  public long zremrangeByScore(final String key, final double min, final double max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  /**
   * Add multiple sorted sets, This command is similar to ZUNIONSTORE, but instead of storing the
 resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   * @return
   */
  @Override
  public Set<String> zunion(ZParams params, String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunion(params, keys));
  }

  /**
   * Add multiple sorted sets with scores, This command is similar to ZUNIONSTORE, but instead of storing the
 resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   * @return
   */
  @Override
  public Set<Tuple> zunionWithScores(ZParams params, String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionWithScores(params, keys));
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(String, String...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The
   * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all elements across all
   * inputs.
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
   * @see #zunionstore(String, String...)
   * @see #zunionstore(String, ZParams, String...)
   * @see #zinterstore(String, String...)
   * @see #zinterstore(String, ZParams, String...)
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zunionstore(final String dstkey, final String... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionstore(dstkey, sets));
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(String, String...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The
   * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all elements across all
   * inputs.
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
   * @see #zunionstore(String, String...)
   * @see #zunionstore(String, ZParams, String...)
   * @see #zinterstore(String, String...)
   * @see #zinterstore(String, ZParams, String...)
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zunionstore(final String dstkey, final ZParams params, final String... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zunionstore(dstkey, params, sets));
  }

  /**
   * Intersect multiple sorted sets, This command is similar to ZINTERSTORE, but instead of storing
 the resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   * @return
   */
  @Override
  public Set<String> zinter(final ZParams params, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinter(params, keys));
  }

  /**
   * Intersect multiple sorted sets, This command is similar to ZINTERSTORE, but instead of storing
 the resulting sorted set, it is returned to the connection.
   * @param params
   * @param keys
   * @return
   */
  @Override
  public Set<Tuple> zinterWithScores(final ZParams params, final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterWithScores(params, keys));
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(String, String...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The
   * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all elements across all
   * inputs.
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
   * @see #zunionstore(String, String...)
   * @see #zunionstore(String, ZParams, String...)
   * @see #zinterstore(String, String...)
   * @see #zinterstore(String, ZParams, String...)
   * @param dstkey
   * @param sets
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zinterstore(final String dstkey, final String... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterstore(dstkey, sets));
  }

  /**
   * Creates a union or intersection of N sorted sets given by keys k1 through kN, and stores it at
   * dstkey. It is mandatory to provide the number of input keys N, before passing the input keys
   * and the other (optional) arguments.
   * <p>
   * As the terms imply, the {@link #zinterstore(String, String...) ZINTERSTORE} command requires an
   * element to be present in each of the given inputs to be inserted in the result. The
   * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all elements across all
   * inputs.
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
   * @see #zunionstore(String, String...)
   * @see #zunionstore(String, ZParams, String...)
   * @see #zinterstore(String, String...)
   * @see #zinterstore(String, ZParams, String...)
   * @param dstkey
   * @param sets
   * @param params
   * @return Integer reply, specifically the number of elements in the sorted set at dstkey
   */
  @Override
  public long zinterstore(final String dstkey, final ZParams params, final String... sets) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zinterstore(dstkey, params, sets));
  }

  @Override
  public long zlexcount(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public List<String> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public List<String> zrevrangeByLex(final String key, final String max, final String min) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public List<String> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(final String key, final String min, final String max) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public long strlen(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strlen(key));
  }

  /**
   * Calculate the longest common subsequence of keyA and keyB.
   * @param keyA keyA
   * @param keyB keyB
   * @param params the params
   * @return According to StrAlgoLCSParams to decide to return content to fill LCSMatchResult.
   */
  @Override
  public LCSMatchResult strAlgoLCSKeys(final String keyA, final String keyB, final StrAlgoLCSParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }

  /**
   * Calculate the longest common subsequence of strA and strB.
   * @param strA strA
   * @param strB strB
   * @param params the params
   * @return According to StrAlgoLCSParams to decide to return content to fill LCSMatchResult.
   */
  public LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.strAlgoLCSStrings(strA, strB, params));
  }

  @Override
  public long lpushx(final String key, final String... string) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.lpushx(key, string));
  }

  /**
   * Undo a {@link #expire(String, int) expire} at turning the expire key into a normal key.
   * <p>
   * Time complexity: O(1)
   * @param key
   * @return Integer reply, specifically: 1: the key is now persist. 0: the key is not persist (only
   *         happens when key not set).
   */
  @Override
  public long persist(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.persist(key));
  }

  @Override
  public long rpushx(final String key, final String... string) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.rpushx(key, string));
  }

  @Override
  public String echo(final String string) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ECHO, string);
    return connection.getBulkReply();
  }

  @Override
  public long linsert(final String key, final ListPosition where, final String pivot,
      final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  /**
   * Pop a value from a list, push it to another list and return it; or block until one is available
   * @param source
   * @param destination
   * @param timeout
   * @return the element
   */
  @Override
  public String brpoplpush(final String source, final String destination, final int timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  /**
   * Sets or clears the bit at offset in the string value stored at key
   * @param key
   * @param offset
   * @param value
   * @return
   */
  @Override
  public boolean setbit(final String key, final long offset, final boolean value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setbit(key, offset, value));
  }

  /**
   * Returns the bit value at offset in the string value stored at key
   * @param key
   * @param offset
   * @return
   */
  @Override
  public boolean getbit(final String key, final long offset) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(final String key, final long offset, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public String getrange(final String key, final long startOffset, final long endOffset) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public long bitpos(final String key, final boolean value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(final String key, final boolean value, final BitPosParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public List<Object> role() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ROLE);
    return BuilderFactory.ENCODED_OBJECT_LIST.build(connection.getOne());
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
  public List<String> configGet(final String pattern) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CONFIG, Keyword.GET.name(), pattern);
    return connection.getMultiBulkReply();
  }

  /**
   * Alter the configuration of a running Redis server. Not all the configuration parameters are
   * supported.
   * <p>
   * The list of configuration parameters supported by CONFIG SET can be obtained issuing a
   * {@link #configGet(String) CONFIG GET *} command.
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
   * <li>The save parameter is a list of space-separated integers. Every pair of integers specify
   * the time and number of changes limit to trigger a save. For instance the command CONFIG SET
   * save "3600 10 60 10000" will configure the server to issue a background saving of the RDB file
   * every 3600 seconds if there are at least 10 changes in the dataset, and every 60 seconds if
   * there are at least 10000 changes. To completely disable automatic snapshots just set the
   * parameter as an empty string.
   * <li>All the integer parameters representing memory are returned and accepted only using bytes
   * as unit.
   * </ul>
   * @param parameter
   * @param value
   * @return Status code reply
   */
  @Override
  public String configSet(final String parameter, final String value) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CONFIG, Keyword.SET.name(), parameter, value);
    return connection.getStatusCodeReply();
  }

  public long publish(final String channel, final String message) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(PUBLISH, channel, message);
    return connection.getIntegerReply();
  }

  public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceed(connection, channels);
    } finally {
      connection.rollbackTimeout();
    }
  }

  public void psubscribe(final JedisPubSub jedisPubSub, final String... patterns) {
    checkIsInMultiOrPipeline();
    connection.setTimeoutInfinite();
    try {
      jedisPubSub.proceedWithPatterns(connection, patterns);
    } finally {
      connection.rollbackTimeout();
    }
  }

  public List<String> pubsubChannels() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(PUBSUB, CHANNELS);
    return connection.getMultiBulkReply();
  }

  public List<String> pubsubChannels(final String pattern) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(PUBSUB, CHANNELS.name(), pattern);
    return connection.getMultiBulkReply();
  }

  public Long pubsubNumPat() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(PUBSUB, NUMPAT);
    return connection.getIntegerReply();
  }

  public Map<String, Long> pubsubNumSub(String... channels) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(PUBSUB, joinParameters(NUMSUB.name(), channels));
    return BuilderFactory.PUBSUB_NUMSUB_MAP.build(connection.getOne());
  }

  @Override
  public Object eval(final String script, final int keyCount, final String... params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Object eval(final String script, final List<String> keys, final List<String> args) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Object eval(final String script) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.eval(script));
  }

  @Override
  public Object evalsha(final String sha1) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Object evalsha(final String sha1, final List<String> keys, final List<String> args) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Object evalsha(final String sha1, final int keyCount, final String... params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Boolean scriptExists(final String sha1) {
    String[] a = new String[1];
    a[0] = sha1;
    return scriptExists(a).get(0);
  }

  @Override
  public List<Boolean> scriptExists(final String... sha1) {
    connection.sendCommand(SCRIPT, joinParameters(Keyword.EXISTS.name(), sha1));
    return BuilderFactory.BOOLEAN_LIST.build(connection.getOne());
  }

  @Override
  public String scriptLoad(final String script) {
    connection.sendCommand(SCRIPT, LOAD.name(), script);
    return connection.getBulkReply();
  }

  @Override
  public List<Slowlog> slowlogGet() {
    connection.sendCommand(SLOWLOG, Keyword.GET);
    return Slowlog.from(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<Slowlog> slowlogGet(final long entries) {
    connection.sendCommand(SLOWLOG, Keyword.GET.getRaw(), toByteArray(entries));
    return Slowlog.from(connection.getObjectMultiBulkReply());
  }

  @Override
  public Long objectRefcount(final String key) {
    connection.sendCommand(OBJECT, REFCOUNT.name(), key);
    return connection.getIntegerReply();
  }

  @Override
  public String objectEncoding(final String key) {
    connection.sendCommand(OBJECT, ENCODING.name(), key);
    return connection.getBulkReply();
  }

  @Override
  public Long objectIdletime(final String key) {
    connection.sendCommand(OBJECT, IDLETIME.name(), key);
    return connection.getIntegerReply();
  }

  @Override
  public List<String> objectHelp() {
    connection.sendCommand(OBJECT, HELP);
    return connection.getMultiBulkReply();
  }

  @Override
  public Long objectFreq(final String key) {
    connection.sendCommand(OBJECT, FREQ.name(), key);
    return connection.getIntegerReply();
  }

  @Override
  public long bitcount(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(final String key, final long start, final long end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitop(final BitOP op, final String destKey, final String... srcKeys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public byte[] dump(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(final String key, final long ttl, final byte[] serializedValue) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(final String key, final long ttl, final byte[] serializedValue,
      final RestoreParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public long pexpire(final String key, final long milliseconds) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long pexpireAt(final String key, final long millisecondsTimestamp) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long pttl(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pttl(key));
  }

  /**
   * PSETEX works exactly like {@link #setex(String, int, String)} with the sole difference that the
   * expire time is specified in milliseconds instead of seconds. Time complexity: O(1)
   * @param key
   * @param milliseconds
   * @param value
   * @return Status code reply
   */

  @Override
  public String psetex(final String key, final long milliseconds, final String value) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public String aclSetUser(final String name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, SETUSER.name(), name);
    return connection.getStatusCodeReply();
  }

  @Override
  public String aclSetUser(String name, String... params) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, joinParameters(SETUSER.name(), name, params));
    return connection.getStatusCodeReply();
  }

  @Override
  public long aclDelUser(final String name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, DELUSER.name(), name);
    return connection.getIntegerReply();
  }

  @Override
  public long aclDelUser(final String name, String... names) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, joinParameters(DELUSER.name(), name, names));
    return connection.getIntegerReply();
  }

  @Override
  public AccessControlUser aclGetUser(final String name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, GETUSER.name(), name);
    return BuilderFactory.ACCESS_CONTROL_USER.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<String> aclUsers() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, USERS);
    return BuilderFactory.STRING_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<String> aclList() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LIST);
    return connection.getMultiBulkReply();
  }

  @Override
  public String aclWhoAmI() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, WHOAMI);
    return connection.getStatusCodeReply();
  }

  @Override
  public List<String> aclCat() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, CAT);
    return BuilderFactory.STRING_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<String> aclCat(String category) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, CAT.name(), category);
    return BuilderFactory.STRING_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<AccessControlLogEntry> aclLog() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOG);
    return BuilderFactory.ACCESS_CONTROL_LOG_ENTRY_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<AccessControlLogEntry> aclLog(int limit) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOG.getRaw(), toByteArray(limit));
    return BuilderFactory.ACCESS_CONTROL_LOG_ENTRY_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public String aclLoad() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, LOAD);
    return connection.getStatusCodeReply();
  }

  @Override
  public String aclSave() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, Keyword.SAVE);
    return connection.getStatusCodeReply();
  }

  @Override
  public String aclGenPass() {
    connection.sendCommand(ACL, GENPASS);
    return connection.getBulkReply();
  }

  @Override
  public String aclGenPass(int bits) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ACL, GENPASS.getRaw(), toByteArray(bits));
    return connection.getBulkReply();
  }


  @Override
  public String clientKill(final String ipPort) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, KILL.name(), ipPort);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clientGetname() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, GETNAME);
    return connection.getBulkReply();
  }

  @Override
  public String clientList() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, LIST);
    return connection.getBulkReply();
  }

  @Override
  public String clientList(ClientType type) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, LIST.getRaw(), Keyword.TYPE.getRaw(), type.getRaw());
    return connection.getBulkReply();
  }

  @Override
  public String clientList(final long... clientIds) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, clientListParams(clientIds));
    return connection.getBulkReply();
  }

  @Override
  public String clientInfo() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, Keyword.INFO);
    return connection.getBulkReply();
  }

  @Override
  public String clientSetname(final String name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLIENT, SETNAME.name(), name);
    return connection.getStatusCodeReply();
  }

  @Override
  public String migrate(final String host, final int port, final String key,
      final int destinationDb, final int timeout) {
    checkIsInMultiOrPipeline();
    CommandArguments args = new CommandArguments(MIGRATE).add(host).add(port).key(key).add(destinationDb).add(timeout);
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  @Override
  public String migrate(final String host, final int port, final int destinationDB,
      final int timeout, final MigrateParams params, final String... keys) {
    checkIsInMultiOrPipeline();
    CommandArguments args = new CommandArguments(MIGRATE).add(host).add(port).add(new byte[0]).add(destinationDB)
        .add(timeout).addParams(params).add(Keyword.KEYS).keys((Object[]) keys);
    connection.sendCommand(args);
    return connection.getStatusCodeReply();
  }

  @Override
  public String migrate(String host, int port, String key, int timeout) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.migrate(host, port, key, timeout));
  }

  @Override
  public String migrate(String host, int port, int timeout, MigrateParams params, String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.migrate(host, port, timeout, params, keys));
  }

  @Override
  public ScanResult<String> scan(final String cursor) {
    return connection.executeCommand(commandObjects.scan(cursor));
  }

  @Override
  public ScanResult<String> scan(final String cursor, final ScanParams params) {
    return connection.executeCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public ScanResult<String> scan(final String cursor, final ScanParams params, final String type) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public ScanResult<Map.Entry<String, String>> hscan(final String key, final String cursor,
      final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public ScanResult<String> sscan(final String key, final String cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public ScanResult<Tuple> zscan(final String key, final String cursor, final ScanParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public String readonly() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(READONLY);
    return connection.getStatusCodeReply();
  }

  @Override
  public String readwrite() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(READWRITE);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterNodes() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.NODES);
    return connection.getBulkReply();
  }

  @Override
  public String clusterReplicas(final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.REPLICAS.name(), nodeId);
    return connection.getBulkReply();
  }

  @Override
  public String clusterMeet(final String ip, final int port) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.MEET.name(), ip, Integer.toString(port));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterReset() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.RESET);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterReset(final ClusterResetType resetType) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.RESET.getRaw(), resetType.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterAddSlots(final int... slots) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, joinParameters(ClusterKeyword.ADDSLOTS.getRaw(), joinParameters(slots)));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterDelSlots(final int... slots) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, joinParameters(ClusterKeyword.DELSLOTS.getRaw(), joinParameters(slots)));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterInfo() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.INFO);
    return connection.getStatusCodeReply();
  }

  @Override
  public List<String> clusterGetKeysInSlot(final int slot, final int count) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.GETKEYSINSLOT.getRaw(), toByteArray(slot), toByteArray(count));
    return connection.getMultiBulkReply();
  }

  @Override
  public List<byte[]> clusterGetKeysInSlotBinary(final int slot, final int count) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.GETKEYSINSLOT.getRaw(), toByteArray(slot), toByteArray(count));
    return connection.getBinaryMultiBulkReply();
  }

  @Override
  public String clusterSetSlotNode(final int slot, final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SETSLOT.getRaw(), toByteArray(slot), ClusterKeyword.NODE.getRaw(), encode(nodeId));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterSetSlotMigrating(final int slot, final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SETSLOT.getRaw(), toByteArray(slot), ClusterKeyword.MIGRATING.getRaw(), encode(nodeId));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterSetSlotImporting(final int slot, final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SETSLOT.getRaw(), toByteArray(slot), ClusterKeyword.IMPORTING.getRaw(), encode(nodeId));
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterSetSlotStable(final int slot) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SETSLOT.getRaw(), toByteArray(slot), ClusterKeyword.STABLE.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterForget(final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.FORGET.name(), nodeId);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterFlushSlots() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.FLUSHSLOTS);
    return connection.getStatusCodeReply();
  }

  @Override
  public long clusterKeySlot(final String key) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.KEYSLOT.name(), key);
    return connection.getIntegerReply();
  }

  @Override
  public long clusterCountKeysInSlot(final int slot) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.COUNTKEYSINSLOT.getRaw(), toByteArray(slot));
    return connection.getIntegerReply();
  }

  @Override
  public String clusterSaveConfig() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SAVECONFIG);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterReplicate(final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.REPLICATE.name(), nodeId);
    return connection.getStatusCodeReply();
  }

  @Override
  public List<String> clusterSlaves(final String nodeId) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SLAVES.name(), nodeId);
    return connection.getMultiBulkReply();
  }

  @Override
  public String clusterFailover() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.FAILOVER);
    return connection.getStatusCodeReply();
  }

  @Override
  public String clusterFailover(ClusterFailoverOption failoverOption) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.FAILOVER.getRaw(), failoverOption.getRaw());
    return connection.getStatusCodeReply();
  }

  @Override
  public List<Object> clusterSlots() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.SLOTS);
    return connection.getObjectMultiBulkReply();
  }

  @Override
  public String clusterMyId() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(CLUSTER, ClusterKeyword.MYID);
    return connection.getBulkReply();
  }

  @Override
  public String asking() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(ASKING);
    return connection.getStatusCodeReply();
  }

  @Override
  public long pfadd(final String key, final String... elements) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public long pfcount(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfcount(key));
  }

  @Override
  public long pfcount(final String... keys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfcount(keys));
  }

  @Override
  public String pfmerge(final String destkey, final String... sourcekeys) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public long geoadd(final String key, final double longitude, final double latitude,
      final String member) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(final String key, final GeoAddParams params, final Map<String, GeoCoordinate> memberCoordinateMap) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2,
      final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<String> geohash(final String key, String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(final String key, String... members) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public long georadiusStore(final String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public long georadiusByMemberStore(final String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public String moduleLoad(final String path) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MODULE, LOAD.name(), path);
    return connection.getStatusCodeReply();
  }

  @Override
  public String moduleUnload(final String name) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MODULE, UNLOAD.name(), name);
    return connection.getStatusCodeReply();
  }

  @Override
  public List<Module> moduleList() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MODULE, LIST);
    return BuilderFactory.MODULE_LIST.build(connection.getObjectMultiBulkReply());
  }

  @Override
  public List<Long> bitfield(final String key, final String... arguments) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(final String key, final String... arguments) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public long hstrlen(final String key, final String field) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public String memoryDoctor() {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, DOCTOR);
    return connection.getBulkReply();
  }

  @Override
  public Long memoryUsage(final String key) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, USAGE.name(), key);
    return connection.getIntegerReply();
  }

  @Override
  public Long memoryUsage(final String key, final int samples) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(MEMORY, USAGE.getRaw(), encode(key), SAMPLES.getRaw(), toByteArray(samples));
    return connection.getIntegerReply();
  }

  @Override
  public StreamEntryID xadd(final String key, final StreamEntryID id, final Map<String, String> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xadd(key, id, hash));
  }

  @Override
  public StreamEntryID xadd_v2(final String key, final XAddParams params, final Map<String, String> hash) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public long xlen(final String key) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<StreamEntry> xrange(final String key, final StreamEntryID start, final StreamEntryID end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(final String key, final StreamEntryID start,
      final StreamEntryID end, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(final String key, final StreamEntryID end,
      final StreamEntryID start) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(final String key, final StreamEntryID end,
      final StreamEntryID start, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public List<StreamEntry> xrange(final String key, final String start, final String end) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(final String key, final String start, final String end, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(final String key, final String end, final String start) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(final String key, final String end, final String start, final int count) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xread(final XReadParams xReadParams, final Map<String, StreamEntryID> streams) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public long xack(final String key, final String group, final StreamEntryID... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(final String key, final String groupname, final StreamEntryID id,
      final boolean makeStream) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupCreate(key, groupname, id, makeStream));
  }

  @Override
  public String xgroupSetID(final String key, final String groupname, final StreamEntryID id) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupSetID(key, groupname, id));
  }

  @Override
  public long xgroupDestroy(final String key, final String groupname) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupDestroy(key, groupname));
  }

  @Override
  public long xgroupDelConsumer(final String key, final String groupname, final String consumerName) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xgroupDelConsumer(key, groupname, consumerName));
  }

  @Override
  public long xdel(final String key, final StreamEntryID... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(final String key, final long maxLen, final boolean approximateLength) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xtrim(key, maxLen, approximateLength));
  }

  @Override
  public long xtrim(final String key, final XTrimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public List<Map.Entry<String, List<StreamEntry>>> xreadGroup(final String groupname,
      final String consumer, final XReadGroupParams xReadGroupParams,
      final Map<String, StreamEntryID> streams) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xreadGroup(groupname, consumer, xReadGroupParams, streams));
  }

  @Override
  public StreamPendingSummary xpending(final String key, final String groupname) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public List<StreamPendingEntry> xpending(final String key, final String groupname,
      final StreamEntryID start, final StreamEntryID end, final int count, final String consumername) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public List<StreamPendingEntry> xpending(final String key, final String groupname, final XPendingParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime,
      XClaimParams params, StreamEntryID... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumername,
      long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xautoclaim(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    checkIsInMultiOrPipeline();
    return connection.executeCommand(commandObjects.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public StreamInfo xinfoStream(String key) {
    return connection.executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public List<StreamGroupInfo> xinfoGroup(String key) {
    return connection.executeCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    return connection.executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  public Object sendCommand(ProtocolCommand cmd, String... args) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(cmd, args);
    return connection.getOne();
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, String... args) {
    checkIsInMultiOrPipeline();
    connection.sendCommand(cmd, args);
    connection.setTimeoutInfinite();
    try {
      return connection.getOne();
    } finally {
      connection.rollbackTimeout();
    }
  }

  private static byte[][] joinParameters(int... params) {
    byte[][] result = new byte[params.length][];
    for (int i = 0; i < params.length; i++) {
      result[i] = toByteArray(params[i]);
    }
    return result;
  }

  private static byte[][] joinParameters(byte[] first, byte[][] rest) {
    byte[][] result = new byte[rest.length + 1][];
    result[0] = first;
    System.arraycopy(rest, 0, result, 1, rest.length);
    return result;
  }

  private static byte[][] joinParameters(byte[] first, byte[] second, byte[][] rest) {
    byte[][] result = new byte[rest.length + 2][];
    result[0] = first;
    result[1] = second;
    System.arraycopy(rest, 0, result, 2, rest.length);
    return result;
  }

  private static String[] joinParameters(String first, String[] rest) {
    String[] result = new String[rest.length + 1];
    result[0] = first;
    System.arraycopy(rest, 0, result, 1, rest.length);
    return result;
  }

  private static String[] joinParameters(String first, String second, String[] rest) {
    String[] result = new String[rest.length + 2];
    result[0] = first;
    result[1] = second;
    System.arraycopy(rest, 0, result, 2, rest.length);
    return result;
  }

}
