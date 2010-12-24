package redis.clients.jedis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

interface RawJedis {

    /**
     * If the key already exists and is a string, this command appends the
     * provided value at the end of the string. If the key does not exist it is
     * created and set as an empty string, so APPEND will be very similar to SET
     * in this special case.
     * <p>
     * Time complexity: O(1). The amortized time complexity is O(1) assuming the
     * appended value is small and the already present value is of any size,
     * since the dynamic string library used by Redis will double the free space
     * available on every reallocation.
     * 
     * @param key
     * @param value
     * @return Integer reply, specifically the total length of the string after
     *         the append operation.
     */
    Long append(final byte[] key, final byte[] value);

    /**
     * Request for authentication in a password protected Redis server.
     * <p>
     * A Redis server can be instructed to require a password before to allow
     * clients to issue commands. This is done using the requirepass directive
     * in the Redis configuration file. If the password given by the client is
     * correct the server replies with an OK status code reply and starts
     * accepting commands from the client. Otherwise an error is returned and
     * the clients needs to try a new password. Note that for the high
     * performance nature of Redis it is possible to try a lot of passwords in
     * parallel in very short time, so make sure to generate a strong and very
     * long password so that this attack is infeasible.
     * 
     * @param password
     * @return Status code reply
     */
    String auth(final byte[] password);

    /**
     * Rewrite the append only file in background when it gets too big. Please
     * for detailed information about the Redis Append Only File check the <a
     * href="http://code.google.com/p/redis/wiki/AppendOnlyFileHowto">Append
     * Only File Howto</a>.
     * <p>
     * BGREWRITEAOF rewrites the Append Only File in background when it gets too
     * big. The Redis Append Only File is a Journal, so every operation
     * modifying the dataset is logged in the Append Only File (and replayed at
     * startup). This means that the Append Only File always grows. In order to
     * rebuild its content the BGREWRITEAOF creates a new version of the append
     * only file starting directly form the dataset in memory in order to
     * guarantee the generation of the minimal number of commands needed to
     * rebuild the database.
     * <p>
     * 
     * @return Status code reply
     */
    String bgrewriteaof();

    /**
     * Asynchronously save the DB on disk.
     * <p>
     * Save the DB in background. The OK code is immediately returned. Redis
     * forks, the parent continues to server the clients, the child saves the DB
     * on disk then exit. A client my be able to check if the operation
     * succeeded using the LASTSAVE command.
     * 
     * @return Status code reply
     */
    String bgsave();

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this
     * commands as blocking versions of LPOP and RPOP able to block if the
     * specified keys don't exist or contain empty lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP
     * but the two commands are identical, the only difference is that BLPOP
     * pops the element from the left (head) of the list, and BRPOP pops from
     * the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non
     * empty list, an element is popped from the head of the list and returned
     * to the caller together with the name of the key (BLPOP returns a two
     * elements array, the first element is the key, the second the popped
     * value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP
     * list1 list2 list3 0 against a dataset where list1 does not exist but
     * list2 and list3 contain non empty lists, BLPOP guarantees to return an
     * element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain empty lists, BLPOP blocks
     * until some other client performs a LPUSH or an RPUSH operation against
     * one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns
     * with the name of the key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will
     * unblock returning a nil special value if the specified amount of seconds
     * passed without a push operation against at least one of the specified
     * keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of
     * zero means instead to block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue,
     * so the first to be served will be the one that started to wait earlier,
     * in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple commands
     * and reading the replies in batch), but it does not make sense to use
     * BLPOP or BRPOP inside a MULTI/EXEC block (a Redis transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to
     * return a multi-bulk nil reply, exactly what happens when the timeout is
     * reached. If you like science fiction, think at it like if inside
     * MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     * 
     * @see #brpop(int, String...)
     * 
     * @param timeout
     *            in seconds or 0 for infinite
     * @param keys
     *            the keys to pop for, the order will be respected
     * @return BLPOP returns a two-elements array via a multi bulk reply in
     *         order to return both the unblocking key and the popped value.
     *         <p>
     *         When a non-zero timeout is specified, and the BLPOP operation
     *         timed out, the return value is a nil multi bulk reply. Most
     *         client values will return false or nil accordingly to the
     *         programming language used.
     */
    List<byte[]> blpop(final long timeout, final byte[] key1,
	    final byte[]... keyN);

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this
     * commands as blocking versions of LPOP and RPOP able to block if the
     * specified keys don't exist or contain empty lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP
     * but the two commands are identical, the only difference is that BLPOP
     * pops the element from the left (head) of the list, and BRPOP pops from
     * the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non
     * empty list, an element is popped from the head of the list and returned
     * to the caller together with the name of the key (BLPOP returns a two
     * elements array, the first element is the key, the second the popped
     * value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP
     * list1 list2 list3 0 against a dataset where list1 does not exist but
     * list2 and list3 contain non empty lists, BLPOP guarantees to return an
     * element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain empty lists, BLPOP blocks
     * until some other client performs a LPUSH or an RPUSH operation against
     * one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns
     * with the name of the key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will
     * unblock returning a nil special value if the specified amount of seconds
     * passed without a push operation against at least one of the specified
     * keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of
     * zero means instead to block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue,
     * so the first to be served will be the one that started to wait earlier,
     * in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple commands
     * and reading the replies in batch), but it does not make sense to use
     * BLPOP or BRPOP inside a MULTI/EXEC block (a Redis transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to
     * return a multi-bulk nil reply, exactly what happens when the timeout is
     * reached. If you like science fiction, think at it like if inside
     * MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     * 
     * @see #blpop(int, String...)
     * 
     * @param timeout
     *            in seconds or 0 for infinite
     * @param keys
     *            the keys to pop for, the order will be respected
     * @return BLPOP returns a two-elements array via a multi bulk reply in
     *         order to return both the unblocking key and the popped value.
     *         <p>
     *         When a non-zero timeout is specified, and the BLPOP operation
     *         timed out, the return value is a nil multi bulk reply. Most
     *         client values will return false or nil accordingly to the
     *         programming language used.
     */
    List<byte[]> brpop(final long timeout, final byte[] key1,
	    final byte[]... keyN);

    /**
     * Retrieve the configuration of a running Redis server. Not all the
     * configuration parameters are supported.
     * <p>
     * CONFIG GET returns the current configuration parameters. This sub command
     * only accepts a single argument, that is glob style pattern. All the
     * configuration parameters matching this parameter are reported as a list
     * of key-value pairs.
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
     * 
     * @param pattern
     * @return Bulk reply.
     */
    List<String> configGet(final String pattern);

    /**
     * Alter the configuration of a running Redis server. Not all the
     * configuration parameters are supported.
     * <p>
     * The list of configuration parameters supported by CONFIG SET can be
     * obtained issuing a {@link #configGet(String) CONFIG GET *} command.
     * <p>
     * The configuration set using CONFIG SET is immediately loaded by the Redis
     * server that will start acting as specified starting from the next
     * command.
     * <p>
     * 
     * <b>Parameters value format</b>
     * <p>
     * The value of the configuration parameter is the same as the one of the
     * same parameter in the Redis configuration file, with the following
     * exceptions:
     * <p>
     * <ul>
     * <li>The save paramter is a list of space-separated integers. Every pair
     * of integers specify the time and number of changes limit to trigger a
     * save. For instance the command CONFIG SET save "3600 10 60 10000" will
     * configure the server to issue a background saving of the RDB file every
     * 3600 seconds if there are at least 10 changes in the dataset, and every
     * 60 seconds if there are at least 10000 changes. To completely disable
     * automatic snapshots just set the parameter as an empty string.
     * <li>All the integer parameters representing memory are returned and
     * accepted only using bytes as unit.
     * </ul>
     * 
     * @param parameter
     * @param value
     * @return Status code reply
     */
    String configSet(final String parameter, final String value);

    /**
     * Open connection to redis.
     * 
     * @throws UnknownHostException
     * @throws IOException
     */
    void connect() throws UnknownHostException, IOException;

    /**
     * Return the number of keys in the currently selected database.
     * 
     * @return Integer reply
     */
    Long dbSize();

    String debug(final DebugParams params);

    /**
     * Decrement the number stored at key by one. If the key does not exist or
     * contains a value of a wrong type, set the key to the value of "0" before
     * to perform the decrement operation.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #incrBy(String, int)
     * @see #decrBy(String, int)
     * 
     * @param key
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    Long decr(final byte[] key);

    /**
     * IDECRBY work just like {@link #decr(String) INCR} but instead to
     * decrement by 1 the decrement is integer.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #decr(String)
     * @see #incrBy(String, int)
     * 
     * @param key
     * @param value
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    Long decrBy(final byte[] key, final long value);

    /**
     * Remove the specified keys.
     * 
     * If a given key does not exist no operation is performed for this key. The
     * command returns the number of keys removed.
     * 
     * Time complexity: O(1)
     * 
     * @param key1
     *            the first key
     * @param keyN
     *            all other keys
     * 
     * @return the number of keys removed
     */
    Long del(final byte[] key1, final byte[]... keyN);

    /**
     * Disconnect the jedis client from the redis server.
     * 
     * @throws IOException
     */
    void disconnect() throws IOException;

    /**
     * Simple echo.
     * 
     * @param value
     *            the value
     * @return the value
     */
    byte[] echo(final byte[] value);

    /**
     * Get reply after pipelened mode.
     * 
     * @throws IllegalStateException
     *             if not in pipelined mode.
     * @return a list of the results from the commands while in pipelined mode.
     */
    List<byte[]> executeRaw();

    /**
     * Test if the specified key exists. The command returns "0" if the key
     * exists, otherwise "1" is returned. Note that even keys set with an empty
     * string as value will return "1".
     * 
     * Time complexity: O(1)
     * 
     * @param key
     * @return Integer reply, "1" if the key exists, otherwise "0"
     */
    Boolean exists(final byte[] key);

    /**
     * Set a timeout on the specified key. After the timeout the key will be
     * automatically deleted by the server. A key with an associated timeout is
     * said to be volatile in Redis terminology.
     * <p>
     * Voltile keys are stored on disk like the other keys, the timeout is
     * persistent too like all the other aspects of the dataset. Saving a
     * dataset containing expires and stopping the server does not stop the flow
     * of time as Redis stores on disk the time when the key will no longer be
     * available as Unix time, and not the remaining seconds.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key
     * already having an expire set. It is also possible to undo the expire at
     * all turning the key into a normal key using the {@link #persist(String)
     * PERSIST} command.
     * <p>
     * Time complexity: O(1)
     * 
     * @see <ahref="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     * 
     * @param key
     * @param seconds
     * @return Integer reply, specifically: 1: the timeout was set. 0: the
     *         timeout was not set since the key already has an associated
     *         timeout (this may happen only in Redis versions < 2.1.3, Redis >=
     *         2.1.3 will happily update the timeout), or the key does not
     *         exist.
     */
    Long expire(final byte[] key, final int seconds);

    /**
     * EXPIREAT works exctly like {@link #expire(String, int) EXPIRE} but
     * instead to get the number of seconds representing the Time To Live of the
     * key as a second argument (that is a relative way of specifing the TTL),
     * it takes an absolute one in the form of a UNIX timestamp (Number of
     * seconds elapsed since 1 Gen 1970).
     * <p>
     * EXPIREAT was introduced in order to implement the Append Only File
     * persistence mode so that EXPIRE commands are automatically translated
     * into EXPIREAT commands for the append only file. Of course EXPIREAT can
     * also used by programmers that need a way to simply specify that a given
     * key should expire at a given time in the future.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key
     * already having an expire set. It is also possible to undo the expire at
     * all turning the key into a normal key using the {@link #persist(String)
     * PERSIST} command.
     * <p>
     * Time complexity: O(1)
     * 
     * @see <ahref="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     * 
     * @param key
     * @param unixTime
     * @return Integer reply, specifically: 1: the timeout was set. 0: the
     *         timeout was not set since the key already has an associated
     *         timeout (this may happen only in Redis versions < 2.1.3, Redis >=
     *         2.1.3 will happily update the timeout), or the key does not
     *         exist.
     */
    Long expireAt(final byte[] key, final long unixTime);

    /**
     * Delete all the keys of all the existing databases, not just the currently
     * selected one.
     * 
     * @return always true, this command never fails.
     */
    Boolean flushAll();

    /**
     * Delete all the keys of the currently selected DB.
     * 
     * @return always true, this command never fails.
     */
    Boolean flushDB();

    /**
     * Get the value of the specified key. If the key does not exist the special
     * value 'nil' is returned. If the value stored at key is not a string an
     * error is returned because GET can only handle string values.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return Bulk reply
     */
    byte[] get(final byte[] key);

    /**
     * Get the config.
     * 
     * @return the config.
     */
    JedisConfig getJedisConfig();

    /**
     * Atomically sets key to value and returns the old value stored at key.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @return the old value or null if unset.
     * @throws JedisException
     *             when key exists but does not hold a string value.
     * @throws NullPointerException
     *             if any parameter is null.
     * 
     */
    byte[] getSet(final byte[] key, final byte[] value);

    /**
     * Remove the specified field from an hash stored at key.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @return If the field was present in the hash it is deleted and 1 is
     *         returned, otherwise 0 is returned and no operation is performed.
     */
    Long hdel(final byte[] key, final byte[] field);

    /**
     * Test for existence of a specified field in a hash.
     * 
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @return Return 1 if the hash stored at key contains the specified field.
     *         Return 0 if the key is not found or the field is not present.
     */
    Boolean hexists(final byte[] key, final byte[] field);

    /**
     * If key holds a hash, retrieve the value associated to the specified
     * field.
     * <p>
     * If the field is not found or the key does not exist, a special 'nil'
     * value is returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @return Bulk reply
     */
    byte[] hget(final byte[] key, final byte[] field);

    /**
     * Return all the fields and associated values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields and values contained into a hash.
     */
    Map<byte[], byte[]> hgetAll(final byte[] key);

    /**
     * Increment the number stored at field in the hash at key by value. If key
     * does not exist, a new key holding a hash is created. If field does not
     * exist or holds a string, the value is set to 0 before applying the
     * operation. Since the value argument is signed you can use this command to
     * perform both increments and decrements.
     * <p>
     * The range of values supported by HINCRBY is limited to 64 bit signed
     * integers.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return Integer reply The new value at field after the increment
     *         operation.
     */
    Long hincrBy(final byte[] key, final byte[] field, final long value);

    /**
     * Return all the fields in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields names contained into a hash.
     */
    Set<byte[]> hkeys(final byte[] key);

    /**
     * Return the number of items in a hash.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @return The number of entries (fields) contained in the hash stored at
     *         key. If the specified key does not exist, 0 is returned assuming
     *         an empty hash.
     */
    Long hlen(final byte[] key);

    /**
     * Retrieve the values associated to the specified fields.
     * <p>
     * If some of the specified fields do not exist, nil values are returned.
     * Non existing keys are considered like empty hashes.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     * 
     * @param key
     * @param fields
     * @return Multi Bulk Reply specifically a list of all the values associated
     *         with the specified fields, in the same order of the request.
     */
    List<byte[]> hmget(final byte[] key, final byte[]... fields);

    /**
     * Set the respective fields to the respective values. HMSET replaces old
     * values with new values.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     * 
     * @param key
     * @param hash
     * @return Always OK because HMSET can't fail
     */
    String hmset(final byte[] key, final Map<byte[], byte[]> hash);

    /**
     * 
     * Set the specified hash field to the specified value.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, and the HSET just produced an update
     *         of the value, 0 is returned, otherwise if a new field is created
     *         1 is returned.
     */
    Long hset(final byte[] key, final byte[] field, final byte[] value);

    /**
     * 
     * Set the specified hash field to the specified value if the field not
     * exists. <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, 0 is returned, otherwise if a new
     *         field is created 1 is returned.
     */
    Long hsetnx(final byte[] key, final byte[] field, final byte[] value);

    /**
     * Return all the values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields values contained into a hash.
     */
    List<byte[]> hvals(final byte[] key);

    /**
     * Increment the number stored at key by one. If the key does not exist or
     * contains a value of a wrong type, set the key to the value of "0" before
     * to perform the increment operation.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incrBy(String, int)
     * @see #decr(String)
     * @see #decrBy(String, int)
     * 
     * @param key
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    Long incr(final byte[] key);

    /**
     * INCRBY work just like {@link #incr(String) INCR} but instead to increment
     * by 1 the increment is integer.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #decr(String)
     * @see #decrBy(String, int)
     * 
     * @param key
     * @param integer
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    Long incrBy(final byte[] key, final long integer);

    /**
     * Provide information and statistics about the server.
     * <p>
     * The info command returns different information and statistics about the
     * server in an format that's simple to parse by computers and easy to read
     * by humans.
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
     * used_memory is returned in bytes, and is the total number of bytes
     * allocated by the program using malloc.
     * <p>
     * uptime_in_days is redundant since the uptime in seconds contains already
     * the full uptime information, this field is only mainly present for
     * humans.
     * <p>
     * changes_since_last_save does not refer to the number of key changes, but
     * to the number of operations that produced some kind of change in the
     * dataset.
     * <p>
     * 
     * @return Bulk reply
     */
    String info();

    boolean isConnected();

    /**
     * Returns all the keys matching the glob-style pattern as space separated
     * strings. For example if you have in the database the keys "foo" and
     * "foobar" the command "KEYS foo*" will return "foo foobar".
     * <p>
     * Note that while the time complexity for this operation is O(n) the
     * constant times are pretty low. For example Redis running on an entry
     * level laptop can scan a 1 million keys database in 40 milliseconds.
     * <b>Still it's better to consider this one of the slow commands that may
     * ruin the DB performance if not used with care.</b>
     * <p>
     * In other words this command is intended only for debugging and special
     * operations like creating a script to change the DB schema. Don't use it
     * in your normal code. Use Redis Sets in order to group together a subset
     * of objects.
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
     * Time complexity: O(n) (with n being the number of keys in the DB, and
     * assuming keys and pattern of limited length)
     * 
     * @param pattern
     * @return Multi bulk reply
     */
    Set<byte[]> keys(final byte[] pattern);

    /**
     * Return the UNIX time stamp of the last successfully saving of the dataset
     * on disk.
     * <p>
     * Return the UNIX TIME of the last DB save executed with success. A client
     * may check if a {@link #bgsave() BGSAVE} command succeeded reading the
     * LASTSAVE value, then issuing a BGSAVE command and checking at regular
     * intervals every N seconds if LASTSAVE changed.
     * 
     * @return Integer reply, specifically an UNIX time stamp.
     */
    Long lastsave();

    /**
     * Return the specified element of the list stored at the specified key. 0
     * is the first element, 1 the second and so on. Negative indexes are
     * supported, for example -1 is the last element, -2 the penultimate and so
     * on.
     * <p>
     * If the value stored at key is not of list type an error is returned. If
     * the index is out of range a 'nil' reply is returned.
     * <p>
     * Note that even if the average time complexity is O(n) asking for the
     * first or the last element of the list is O(1).
     * <p>
     * Time complexity: O(n) (with n being the length of the list)
     * 
     * @param key
     * @param index
     * @return Bulk reply, specifically the requested element
     */
    byte[] lindex(final byte[] key, final int index);

    /**
     * Add a value to a list after a element in the list.
     * 
     * After the first occurrence of element the value will be added one time.
     * 
     * 
     * @param key
     *            the key of the list
     * @param element
     *            the element in the list to search for
     * @param value
     *            the value to insert
     * @return the new size of the list or -1 if the element is not in list or 0
     *         if no list at key
     * 
     * @throws JedisException
     *             if key is other data type list
     */
    Long linsertAfter(final byte[] key, final byte[] element, final byte[] value);

    /**
     * Add a value to a list before a element in the list.
     * 
     * Before the first occurrence of element the value will be added one time.
     * 
     * 
     * @param key
     *            the key of the list
     * @param element
     *            the element in the list to search for
     * @param value
     *            the value to insert
     * @return the new size of the list or -1 if the element is not in list or 0
     *         if no list at key
     * 
     * @throws JedisException
     *             if key is other data type list
     */
    Long linsertBefore(final byte[] key, final byte[] element,
	    final byte[] value);

    /**
     * Return the length of the list stored at the specified key. If the key
     * does not exist zero is returned (the same behaviour as for empty lists).
     * If the value stored at key is not a list an error is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return The length of the list.
     */
    Long llen(final byte[] key);

    /**
     * Atomic return and remove the first (left) value of the list.
     * 
     * For example if the list contains the elements "a","b","c" lpop will
     * return "a" and the list will become "b","c".
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#rpop(String)
     * 
     * @param key
     *            the key
     * @return the first (left) value or null if list is empty or null if no
     *         list at key.
     * 
     * @throws JedisException
     *             if data type at key is other then list
     */
    byte[] lpop(final byte[] key);

    /**
     * Add the value to the head (left) of the list stored at key.
     * 
     * If the key does not exist an empty list is created just before the append
     * operation. If the key exists but is not a List an error is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#lpushx(String, String)
     * @see Jedis#rpush(String, String)
     * @see Jedis#rpushx(String, String)
     * 
     * @param key
     *            the key
     * @param value
     *            the value to push
     * @return the number of elements inside the list after the push operation.
     * 
     * @throws JedisException
     *             if data type at key is other then list.
     */
    Long lpush(final byte[] key, final byte[] value);

    /**
     * Add the value to the head (left) if there is a non empty list at key.
     * 
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#rpushx(byte[], byte[])
     * @see Jedis#lpush(byte[], byte[])
     * @see Jedis#rpush(byte[], byte[])
     * 
     * @param key
     *            the key
     * @param value
     *            the value to push
     * @return the number of elements inside the list after the push operation,
     *         0 if there is no list or a empty list at key.
     * 
     * @throws JedisException
     *             if data type at key is other then list.
     */
    Long lpushx(final byte[] key, final byte[] value);

    /**
     * Return the specified elements of the list stored at the specified key.
     * Start and end are zero-based indexes. 0 is the first element of the list
     * (the list head), 1 the next element and so on.
     * <p>
     * For example LRANGE foobar 0 2 will return the first three elements of the
     * list.
     * <p>
     * start and end can also be negative numbers indicating offsets from the
     * end of the list. For example -1 is the last element of the list, -2 the
     * penultimate element and so on.
     * <p>
     * <b>Consistency with range functions in various programming languages</b>
     * <p>
     * Note that if you have a list of numbers from 0 to 100, LRANGE 0 10 will
     * return 11 elements, that is, rightmost item is included. This may or may
     * not be consistent with behavior of range-related functions in your
     * programming language of choice (think Ruby's Range.new, Array#slice or
     * Python's range() function).
     * <p>
     * LRANGE behavior is consistent with one of Tcl.
     * <p>
     * <b>Out-of-range indexes</b>
     * <p>
     * Indexes out of range will not produce an error: if start is over the end
     * of the list, or start > end, an empty list is returned. If end is over
     * the end of the list Redis will threat it just like the last element of
     * the list.
     * <p>
     * Time complexity: O(start+n) (with n being the length of the range and
     * start being the start offset)
     * 
     * @param key
     * @param start
     * @param end
     * @return Multi bulk reply, specifically a list of elements in the
     *         specified range.
     */
    List<byte[]> lrange(final byte[] key, final long start, final long end);

    /**
     * Remove the first count occurrences of the value element from the list. If
     * count is zero all the elements are removed. If count is negative elements
     * are removed from tail to head, instead to go from head to tail that is
     * the normal behaviour. So for example LREM with count -2 and hello as
     * value to remove against the list (a,b,c,hello,x,hello,hello) will lave
     * the list (a,b,c,hello,x). The number of removed elements is returned as
     * an integer, see below for more information about the returned value. Note
     * that non existing keys are considered like empty lists by LREM, so LREM
     * against non existing keys will always return 0.
     * <p>
     * Time complexity: O(N) (with N being the length of the list)
     * 
     * @param key
     * @param count
     * @param value
     * @return Integer Reply, specifically: The number of removed elements if
     *         the operation succeeded
     */
    Long lrem(final byte[] key, final int count, final byte[] value);

    /**
     * Set a new value as the element at index position of the List at key.
     * <p>
     * Out of range indexes will generate an error.
     * <p>
     * Similarly to other list commands accepting indexes, the index can be
     * negative to access elements starting from the end of the list. So -1 is
     * the last element, -2 is the penultimate, and so forth.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(N) (with N being the length of the list), setting the first or last
     * elements of the list is O(1).
     * 
     * @see #lindex(String, int)
     * 
     * @param key
     * @param index
     * @param value
     * @return Status code reply
     */
    String lset(final byte[] key, final int index, final byte[] value);

    /**
     * Trim an existing list so that it will contain only the specified range of
     * elements specified. Start and end are zero-based indexes. 0 is the first
     * element of the list (the list head), 1 the next element and so on.
     * <p>
     * For example LTRIM foobar 0 2 will modify the list stored at foobar key so
     * that only the first three elements of the list will remain.
     * <p>
     * start and end can also be negative numbers indicating offsets from the
     * end of the list. For example -1 is the last element of the list, -2 the
     * penultimate element and so on.
     * <p>
     * Indexes out of range will not produce an error: if start is over the end
     * of the list, or start > end, an empty list is left as value. If end over
     * the end of the list Redis will threat it just like the last element of
     * the list.
     * <p>
     * Hint: the obvious use of LTRIM is together with LPUSH/RPUSH. For example:
     * <p>
     * {@code lpush("mylist", "someelement"); ltrim("mylist", 0, 99); * }
     * <p>
     * The above two commands will push elements in the list taking care that
     * the list will not grow without limits. This is very useful when using
     * Redis to store logs for example. It is important to note that when used
     * in this way LTRIM is an O(1) operation because in the average case just
     * one element is removed from the tail of the list.
     * <p>
     * Time complexity: O(n) (with n being len of list - len of range)
     * 
     * @param key
     * @param start
     * @param end
     * @return Status code reply
     */
    String ltrim(final byte[] key, final int start, final int end);

    /**
     * Get the values of all the specified keys. If one or more keys dont exist
     * or is not of type String, a 'nil' value is returned instead of the value
     * of the specified key, but the operation never fails.
     * <p>
     * Time complexity: O(1) for every key
     * 
     * @param keys
     * @return Multi bulk reply
     */
    List<byte[]> mget(final byte[]... keys);

    /**
     * Dump all the received requests in real time.
     * <p>
     * MONITOR is a debugging command that outputs the whole sequence of
     * commands received by the Redis server. is very handy in order to
     * understand what is happening into the database. This command is used
     * directly via telnet.
     * 
     * @param jedisMonitor
     */
    void monitor(final JedisMonitor jedisMonitor);

    /**
     * Move the specified key from the currently selected DB to the specified
     * destination DB. Note that this command returns 1 only if the key was
     * successfully moved, and 0 if the target key was already there or if the
     * source key was not found at all, so it is possible to use MOVE as a
     * locking primitive.
     * 
     * @param key
     * @param dbIndex
     * @return Integer reply, specifically: 1 if the key was moved 0 if the key
     *         was not moved because already present on the target DB or was not
     *         found in the current DB.
     */
    Long move(final byte[] key, final int dbIndex);

    /**
     * Set the the respective keys to the respective values. MSET will replace
     * old values with new values, while {@link #msetnx(String...) MSETNX} will
     * not perform any operation at all even if just a single key already
     * exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different
     * keys representing different fields of an unique logic object in a way
     * that ensures that either all the fields or none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance
     * if the keys A and B are modified, another client talking to Redis can
     * either see the changes to both A and B at once, or no modification at
     * all.
     * 
     * @see #msetnx(String...)
     * 
     * @param keysvalues
     * @return Status code reply Basically +OK as MSET can't fail
     */
    String mset(final byte[]... keysvalues);

    /**
     * Set the the respective keys to the respective values.
     * {@link #mset(String...) MSET} will replace old values with new values,
     * while MSETNX will not perform any operation at all even if just a single
     * key already exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different
     * keys representing different fields of an unique logic object in a way
     * that ensures that either all the fields or none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance
     * if the keys A and B are modified, another client talking to Redis can
     * either see the changes to both A and B at once, or no modification at
     * all.
     * 
     * @see #mset(String...)
     * 
     * @param keysvalues
     * @return Integer reply, specifically: 1 if the all the keys were set 0 if
     *         no key was set (at least one key already existed)
     */
    Long msetnx(final byte[]... keysvalues);

    /**
     * Undo a {@link #expire(String, int) expire} at turning the expire key into
     * a normal key.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return Integer reply, specifically: 1: the key is now persist. 0: the
     *         key is not persist (only happens when key not set).
     */
    Long persist(final byte[] key);

    /**
     * Ping the server.
     * 
     * @return true for a pong, fals if not
     */
    Boolean ping();

    void psubscribe(final JedisPubSub jedisPubSub, final String pattern1,
	    final String... patternN);

    Long publish(final String channel, final String message);

    /**
     * Ask the server to silently close the connection.
     */
    void quit();

    /**
     * Return a randomly selected key from the currently selected DB.
     * <p>
     * Time complexity: O(1)
     * 
     * @return Singe line reply, specifically the randomly selected key or an
     *         empty string is the database is empty
     */
    byte[] randomBinaryKey();

    /**
     * Atomically renames the key oldkey to newkey. If the source and
     * destination name are the same an error is returned. If newkey already
     * exists it is overwritten.
     * <p>
     * Time complexity: O(1)
     * 
     * @param oldkey
     * @param newkey
     * @return Status code repy
     */
    String rename(final byte[] oldkey, final byte[] newkey);

    /**
     * Rename oldkey into newkey but fails if the destination key newkey already
     * exists.
     * <p>
     * Time complexity: O(1)
     * 
     * @param oldkey
     * @param newkey
     * @return Integer reply, specifically: 1 if the key was renamed 0 if the
     *         target key already exist
     */
    Long renamenx(final byte[] oldkey, final byte[] newkey);

    /**
     * Atomic return and remove the last (right) value of the list.
     * 
     * For example if the list contains the elements "a","b","c" rpop will
     * return "c" and the list will become "a","b".
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#lpop(byte[])
     * 
     * @param key
     *            the key
     * @return the last (right) value or null if list is empty or null if no
     *         list at key.
     * 
     * @throws JedisException
     *             if data type at key is other then list
     */
    byte[] rpop(final byte[] key);

    /**
     * Atomic return and remove the last (tail) element of the list at key src,
     * and push the element as the first (head) element of the list at key dst.
     * 
     * For example if the source list contains the elements "a","b","c" and the
     * destination list contains the elements "1","2" after an rpoplpush command
     * the content of the two lists will be "a","b" and "c","1","2".
     * <p>
     * If the srckey and dstkey are the same the operation is equivalent to
     * removing the last element from the list and pusing it as first element of
     * the list, so it's a "list rotation" command.
     * <p>
     * Time complexity: O(1)
     * 
     * @param srcKey
     *            the source list
     * @param dstKey
     *            the destination list
     * @return the value from the rpop operation or null if empty/no list at src
     * @throws JedisException
     *             if one of the keys is data type other then list
     */
    byte[] rpoplpush(final byte[] srcKey, final byte[] dstKey);

    /**
     * Add the value to the tail (right) of the list stored at key.
     * 
     * If the key does not exist an empty list is created just before the append
     * operation. If the key exists but is not a List an error is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#rpushx(byte[], byte[])
     * @see Jedis#lpush(byte[], byte[])
     * @see Jedis#lpushx(byte[], byte[])
     * 
     * @param key
     *            the key
     * @param value
     *            the value to push
     * @return the number of elements inside the list after the push operation.
     * @throws JedisException
     *             if the key is data type other then list
     */
    Long rpush(final byte[] key, final byte[] value);

    /**
     * Add the value to the tail (right) if there is a non empty list at key.
     * 
     * <p>
     * Time complexity: O(1)
     * 
     * @see Jedis#rpushx(byte[], byte[])
     * @see Jedis#lpush(byte[], byte[])
     * @see Jedis#rpush(byte[], byte[])
     * 
     * @param key
     *            the key
     * @param value
     *            the value to push
     * @return the number of elements inside the list after the push operation,
     *         0 if there is no list or a empty list at key.
     * 
     * @throws JedisException
     *             if data type at key is other then list.
     */
    Long rpushx(final byte[] key, final byte[] value);

    /**
     * Add a value to the set stored at key or create new set.
     * 
     * If value is already a member of the set no operation is performed. If no
     * set does exist at key a new set with the specified value as sole member
     * is created.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @param value
     * @return true if the new element was added or false if the element was
     *         already a member of the set
     * @throws JedisException
     *             if data type at key is other then set
     */
    Boolean sadd(final byte[] key, final byte[] member);

    /**
     * Synchronously save the DB on disk.
     * <p>
     * Save the whole dataset on disk (this means that all the databases are
     * saved, as well as keys with an EXPIRE set (the expire is preserved). The
     * server hangs while the saving is not completed, no connection is served
     * in the meanwhile. An OK code is returned when the DB was fully stored in
     * disk.
     * <p>
     * The background variant of this command is {@link #bgsave() BGSAVE} that
     * is able to perform the saving in the background while the server
     * continues serving other clients.
     * <p>
     * 
     * @return Status code reply
     */
    String save();

    /**
     * Return the set cardinality (number of members).
     * 
     * @param key
     *            the key of the set
     * @return the cardinality (number of elements) of the set, 0 for empty and
     *         no set
     * @throws JedisException
     *             if data type other then set
     */
    Long scard(final byte[] key);

    /**
     * Return the difference between the Set stored at key1 and all the Sets
     * key2, ..., keyN.
     * <p>
     * Non existing keys are considered like empty sets.
     * <p>
     * Time complexity: O(N) with N being the total number of elements of all
     * the sets
     * 
     * @param key1
     *            the key of the first set
     * @param keyN
     *            all other set keys
     * @return Return the members of a set resulting from the difference between
     *         the first set provided and all the successive sets. If just one
     *         set is given it behaves as {@link #smembers(String)}.
     * @throws JedisException
     *             if one data type is other then set.
     * @throws NullPointerException
     *             if any key is null
     */
    Set<byte[]> sdiff(final byte[] key1, final byte[]... keyN);

    /**
     * Store a difference of sets at destination set.
     * <p>
     * This command works exactly like {@link #sinter(byte[], byte[]...)} but
     * instead of being returned the resulting set is stored as dstkey. Any
     * existing value in dstkey will be over-written.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the
     * provided sets
     * 
     * @param key1
     *            TODO
     * @param dstKey
     *            the key to store the result to
     * @param key1
     *            the first set, with only one set given this would act as a set
     *            copy
     * @param keyN
     *            the following sets
     * 
     * @return The cardinality of the list at dstKey
     * 
     * @throws JedisException
     *             if any key is not of data type set
     * @throws NullPointerException
     *             if dstKey or key1 is null
     */
    Long sdiffstore(final byte[] dstKey, final byte[] key1,
	    final byte[]... keyN);

    /**
     * Select the DB with having the specified zero-based numeric index. For
     * default every new client connection is automatically selected to DB 0.
     * 
     * @param index
     * @return Status code reply
     */
    String select(final int index);

    /**
     * Set key to hold the string value.
     * <p>
     * If key already holds a value, it is overwritten, regardless of its type.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     *            to set
     * @param value
     *            to set
     * @return always true since set can't fail.
     * @throws NullPointerException
     *             if key or value is null
     */
    Boolean set(final byte[] key, final byte[] value);

    /**
     * The command is exactly equivalent to the following group of commands:
     * {@link #set(String, String) SET} + {@link #expire(String, int) EXPIRE}.
     * The operation is atomic.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param seconds
     * @param value
     * @return Status code reply
     */
    String setex(final byte[] key, final int seconds, final byte[] value);

    /**
     * Set the config.
     * 
     * @param config
     *            the config.
     */
    void setJedisConfig(JedisConfig config);

    /**
     * SETNX works exactly like {@link #set(String, String) SET} with the only
     * difference that if the key already exists no operation is performed.
     * SETNX actually means "SET if Not eXists".
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Integer reply, specifically: 1 if the key was set 0 if the key
     *         was not set
     */
    Long setnx(final byte[] key, final byte[] value);

    /**
     * Synchronously save the DB on disk, then shutdown the server.
     * <p>
     * Stop all the clients, save the DB, then quit the server. This commands
     * makes sure that the DB is switched off without the lost of any data. This
     * is not guaranteed if the client uses simply {@link #save() SAVE} and then
     * {@link #quit() QUIT} because other clients may alter the DB data between
     * the two commands.
     * 
     * @return Status code reply on error. On success nothing is returned since
     *         the server quits and the connection is closed.
     */
    String shutdown();

    /**
     * Return the members of a set resulting from the intersection of all the
     * sets hold at the specified keys.
     * <p>
     * If just a single key is specified, then this command produces the same
     * result as {@link #smembers(String)}. Actually smembers is just syntax
     * sugar for sinter.
     * <p>
     * Non existing keys are considered like empty sets, so if one of the keys
     * is missing an empty set is returned (since the intersection with an empty
     * set always is an empty set).
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the
     * smallest set and M the number of sets
     * 
     * @param key1
     *            the first set
     * @param keyN
     *            all other sets
     * 
     * @return The intersecion of all given sets.
     * 
     * @throws JedisException
     *             if data type of any key is other then set.
     */
    Set<byte[]> sinter(byte[] key1, final byte[]... keyN);

    /**
     * Store the members of a set resulting from the intersection of all the
     * sets hold at the specified keys.
     * <p>
     * This commnad works exactly like {@link #sinter(String, String...)} but
     * instead of being returned the resulting set is stored as dstkey.
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the
     * smallest set and M the number of sets
     * 
     * @param dstKey
     *            the set to store the result
     * @param srcKey1
     *            the first set
     * @param scrKeyN
     *            all other sets
     * @return The cardinality of the resulting set
     * @throws JedisException
     *             if data type of any key is not set
     */
    Long sinterstore(final byte[] dstKey, byte[] key1, final byte[]... keyN);

    /**
     * Return 1 if member is a member of the set stored at key, otherwise 0 is
     * returned.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @param member
     * @return Integer reply, specifically: 1 if the element is a member of the
     *         set 0 if the element is not a member of the set OR if the key
     *         does not exist
     */
    Boolean sismember(final byte[] key, final byte[] member);

    /**
     * Change the replication settings.
     * <p>
     * The SLAVEOF command can change the replication settings of a slave on the
     * fly. If a Redis server is arleady acting as slave, the command SLAVEOF NO
     * ONE will turn off the replicaiton turning the Redis server into a MASTER.
     * In the proper form SLAVEOF hostname port will make the server a slave of
     * the specific server listening at the specified hostname and port.
     * <p>
     * If a server is already a slave of some master, SLAVEOF hostname port will
     * stop the replication against the old server and start the
     * synchrnonization against the new one discarding the old dataset.
     * <p>
     * The form SLAVEOF no one will stop replication turning the server into a
     * MASTER but will not discard the replication. So if the old master stop
     * working it is possible to turn the slave into a master and set the
     * application to use the new master in read/write. Later when the other
     * Redis server will be fixed it can be configured in order to work as
     * slave.
     * <p>
     * 
     * @param host
     * @param port
     * @return Status code reply
     */
    String slaveof(final String host, final int port);

    String slaveofNoOne();

    /**
     * Return all the members of the set at key.
     * <p>
     * This is just syntax glue for {@link #sinter(byte[]...) SINTER}.
     * <p>
     * Time complexity O(N)
     * 
     * @param key
     *            the key
     * @return all the members of the set
     * @throws JedisException
     *             if data type at key is not set
     */
    Set<byte[]> smembers(final byte[] key);

    /**
     * Move the specifided value from the set at source key to the set at
     * destination key.
     * <p>
     * This operation is atomic, in every given moment the element will appear
     * to be in the source or destination set for accessing clients.
     * <p>
     * If the source set does not exist or does not contain the specified
     * element no operation is performed and zero is returned, otherwise the
     * element is removed from the source set and added to the destination set.
     * On success one is returned, even if the element was already present in
     * the destination set.
     * <p>
     * An error is raised if the source or destination keys contain a non Set
     * value.
     * <p>
     * Time complexity O(1)
     * 
     * @param srcKey
     *            the key of the source set
     * @param dstKey
     *            the key of the destination set
     * @param value
     *            the set member to be moved from source to destination
     * @return true if value was member of source set or false if value is not a
     *         member of source set or false if source is no set
     * @throws JedisException
     *             if source or destination is other than data type set
     */
    Boolean smove(final byte[] srcKey, final byte[] dstKey, final byte[] value);

    /**
     * Sort a Set or a List.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key.
     * By default sorting is numeric with elements being compared as double
     * precision floating point numbers. This is the simplest form of SORT.
     * 
     * @see #sort(String, String)
     * @see #sort(String, SortParams)
     * @see #sort(String, SortParams, String)
     * 
     * 
     * @param key
     * @return Assuming the Set/List at key contains a list of numbers, the
     *         return value will be the list of numbers ordered from the
     *         smallest to the biggest number.
     */
    List<byte[]> sort(final byte[] key);

    /**
     * Sort a Set or a List and Store the Result at dstkey.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key
     * and store the result at dstkey. By default sorting is numeric with
     * elements being compared as double precision floating point numbers. This
     * is the simplest form of SORT.
     * 
     * @see #sort(String)
     * @see #sort(String, SortParams)
     * @see #sort(String, SortParams, String)
     * 
     * @param key
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     */
    Long sort(final byte[] key, final byte[] dstkey);

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
     * -> [1, 2, 3]
     * 
     * sort(x, sp.desc())
     * -> [3, 2, 1]
     * 
     * sort(y)
     * -> [c, a, b]
     * 
     * sort(y, sp.alpha())
     * -> [a, b, c]
     * 
     * sort(y, sp.alpha().desc())
     * -> [c, a, b]
     * </pre>
     * 
     * Limit (e.g. for Pagination):
     * 
     * <pre>
     * sort(x, sp.limit(0, 2))
     * -> [1, 2]
     * 
     * sort(y, sp.alpha().desc().limit(1, 2))
     * -> [b, a]
     * </pre>
     * 
     * Sorting by external keys:
     * 
     * <pre>
     * sort(x, sb.by(w*))
     * -> [3, 2, 1]
     * 
     * sort(x, sb.by(w*).desc())
     * -> [1, 2, 3]
     * </pre>
     * 
     * Getting external keys:
     * 
     * <pre>
     * sort(x, sp.by(w*).get(k*))
     * -> [x, y, z]
     * 
     * sort(x, sp.by(w*).get(#).get(k*))
     * -> [3, x, 2, y, 1, z]
     * </pre>
     * 
     * @see #sort(String)
     * @see #sort(String, SortParams, String)
     * 
     * @param key
     * @param sortingParameters
     * @return a list of sorted elements.
     */
    List<byte[]> sort(final byte[] key, final SortParams sortingParameters);

    /**
     * Sort a Set or a List accordingly to the specified parameters and store
     * the result at dstkey.
     * 
     * @see #sort(String, SortParams)
     * @see #sort(String)
     * @see #sort(String, String)
     * 
     * @param key
     * @param sortingParameters
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     */
    Long sort(final byte[] key, final SortParams sortingParameters,
	    final byte[] dstkey);

    /**
     * Get and remove a random member from a set.
     * <p>
     * The {@link #srandmember(byte[])} command does a similar work but the
     * returned element is not removed from the Set.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     *            the key of the set
     * @return a random member of the set or null if set is empty or null if no
     *         set at key
     * @throws JedisException
     *             if other data type then set at key
     */
    byte[] spop(final byte[] key);

    /**
     * Get a random member from a set.
     * <p>
     * The {@link #spop(byte[]))} command does a similar work but the returned
     * element is removed from the Set.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     *            the key of the set
     * @return a random member of the set or null if set is empty or null if no
     *         set at key
     * @throws JedisException
     *             if other data type then set at key
     * @throws NullPointerException
     *             if key is null
     */
    byte[] srandmember(final byte[] key);

    /**
     * Remove the specified value from the set at key.
     * <p>
     * If value was not a member of the set no operation is performed.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     *            the key
     * @param value
     *            the member to remove
     * @return true if element was removed or false if no value is not member of
     *         set or false if no set at key
     * @throws JedisException
     *             if data type at key is not set
     */
    Boolean srem(final byte[] key, final byte[] value);

    /**
     * Returns the length of the string stored at the specified key.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     *            the key
     * @return the length of the string.
     */
    Long strlen(final byte[] key);

    void subscribe(final JedisPubSub jedisPubSub, final String... channels);

    /**
     * Return a subset of the string from offset start to offset end (both
     * offsets are inclusive). Negative offsets can be used in order to provide
     * an offset starting from the end of the string. So -1 means the last char,
     * -2 the penultimate and so forth.
     * <p>
     * The function handles out of range requests without raising an error, but
     * just limiting the resulting range to the actual length of the string.
     * <p>
     * Time complexity: O(start+n) (with start being the start index and n the
     * total length of the requested range). Note that the lookup part of this
     * command is O(1) so for small strings this is actually an O(1) command.
     * 
     * @param key
     * @param start
     * @param end
     * @return Bulk reply
     */
    byte[] substr(final byte[] key, final int start, final int end);

    /**
     * Return the result from the union of all the sets hold at the given keys.
     * <p>
     * Non existing keys are considered like empty sets.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the
     * provided sets
     * 
     * @param key1
     *            the first key, If just a single key is specified, then this
     *            command produces the same result as {@link #smembers(String)}
     * 
     * @param keyN
     *            all other keys
     * 
     * @return the union of all the sets hold at the given keys
     * @throws JedisException
     *             if data type at key is not set
     */
    Set<byte[]> sunion(final byte[] key1, final byte[]... keyN);

    /**
     * Store a uniun of keys at destination key.
     * <p>
     * This command works exactly like {@link #sunion(byte[], byte[]...)} but
     * instead of being returned the resulting set is stored as dstkey. Any
     * existing value in dstkey will be over-written.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the
     * provided sets
     * 
     * @param dstKey
     *            the key to store the result to
     * @param key1
     *            the first set, with only one set given this would act as a set
     *            copy
     * @param keyN
     *            the following sets
     * @return The cardinality of the list at dstKey
     * 
     * @throws JedisException
     *             if any key is not of data type set
     * @throws NullPointerException
     *             if dstKey or key1 is null
     */
    Long sunionstore(final byte[] dstKey, final byte[] key1,
	    final byte[]... keyN);

    void sync();

    /**
     * The TTL command returns the remaining time to live in seconds of a key
     * that has an {@link #expire(String, int) EXPIRE} set. This introspection
     * capability allows a Redis client to check how many seconds a given key
     * will continue to be part of the dataset.
     * 
     * @param key
     * @return Integer reply, returns the remaining time to live in seconds of a
     *         key that has an EXPIRE. If the Key does not exists or does not
     *         have an associated expire, -1 is returned.
     */
    Long ttl(final byte[] key);

    /**
     * Return the type of the value stored at key in form of a string. The type
     * can be one of "none", "string", "list", "set". "none" is returned if the
     * key does not exist.
     * 
     * Time complexity: O(1)
     * 
     * @param key
     * @return Status code reply, specifically: "none" if the key does not exist
     *         "string" if the key contains a String value "list" if the key
     *         contains a List value "set" if the key contains a Set value
     *         "zset" if the key contains a Sorted Set value "hash" if the key
     *         contains a Hash value
     */
    String type(final byte[] key);

    String unwatch();

    String watch(final byte[] key);

    /**
     * Add the specified member having the specifeid score to the sorted set
     * stored at key.
     * <p>
     * If member is already a member of the sorted set the score is updated, and
     * the element reinserted in the right position to ensure sorting. If key
     * does not exist a new sorted set with the specified member as sole member
     * is crated.
     * <p>
     * Time complexity: O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * @param key
     *            the key of the sset
     * @param score
     *            the score of the value
     * @param value
     *            the value
     * @return true if value is new added to sset, false if value was already a
     *         member
     * @throws JedisException
     *             if other data type then sset at key
     * @throws NullPointerException
     *             if key or value is null
     */
    Boolean zadd(final byte[] key, final double score, final byte[] member);

    /**
     * Add the specified member having the specifeid score to the sorted set
     * stored at key.
     * <p>
     * If member is already a member of the sorted set the score is updated, and
     * the element reinserted in the right position to ensure sorting. If key
     * does not exist a new sorted set with the specified member as sole member
     * is crated.
     * <p>
     * Time complexity: O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * @param key
     *            the key of the sset
     * @param value
     *            pair of the value and score
     * @return true if value is new added to sset, false if value was already a
     *         member
     * @throws JedisException
     *             if other data type then sset at key
     * @throws NullPointerException
     *             if key or value is null
     */
    Boolean zadd(final byte[] key, final Pair<byte[], Double> value);

    /**
     * Return the sorted set cardinality (number of elements).
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     *            of the sset
     * @return the cardinality of the sset or if the key does not exist 0 is
     *         returned, like for empty sorted sets.
     * @throws JedisException
     *             if other data type then sset at key
     * @throws NullPointerException
     *             if key is null
     */
    Long zcard(final byte[] key);

    /**
     * Returns the number of elements in the sorted set at key with a score
     * between min and max.
     * <p>
     * The min and max arguments have the same semantic as described for
     * {@link #zrangeByScore(String, String, String)}.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M being the number of elements between min and max.
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @return the number of elements in the specified score range.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     */
    Long zcount(final byte[] key, final byte[] min, final byte[] max);

    /**
     * Add a value to the members score and updates the position of the member
     * in the sorted set accordingly.
     * <p>
     * If member does not already exist in the sorted set it is added with value
     * as score (that is, like if the previous score was virtually zero). If key
     * does not exist a new sorted set with the specified member as sole member
     * is crated.
     * <p>
     * It's possible to provide a negative value to perform a decrement.
     * <p>
     * Time complexity: O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * @param key
     *            of the sset
     * @param member
     *            to increment
     * @param value
     *            to incr/decrement by
     * @return the new score
     * @throws JedisException
     *             if key is other data type then sset
     * @throws NullPointerException
     *             if any parameter is null
     */
    Double zincrby(final byte[] key, final byte[] member, final double value);

    /**
     * Computes the intersection of numkeys sorted sets given by the specified
     * key / multiplicator {@link Pair pairs}, and stores the result in dstKey,
     * the resulting score of an element is the maximum of its scores in the
     * sorted sets where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N*K)+O(M*log(M)) worst case with N being the smallest
     * input sorted set, K being the number of input sorted sets and M being the
     * number of elements in the resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zinterstoreMax(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

    /**
     * Computes the intersection of numkeys sorted sets given by the specified
     * key / multiplicator {@link Pair pairs}, and stores the result in dstKey,
     * the resulting score of an element is the minimum of its scores in the
     * sorted sets where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N*K)+O(M*log(M)) worst case with N being the smallest
     * input sorted set, K being the number of input sorted sets and M being the
     * number of elements in the resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zinterstoreMin(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

    /**
     * Computes the intersection of numkeys sorted sets given by the specified
     * key / multiplicator {@link Pair pairs}, and stores the result in dstKey,
     * the resulting score of an element is the sum of its scores in the sorted
     * sets where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N*K)+O(M*log(M)) worst case with N being the smallest
     * input sorted set, K being the number of input sorted sets and M being the
     * number of elements in the resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zinterstoreSum(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

    /**
     * Returns the specified range of members in the sorted set stored at key.
     * The members are considered to be ordered from the lowest to the highest
     * score.
     * <p>
     * Both start and stop are zero-based indexes, where 0 is the first element,
     * 1 is the next element and so on. They can also be negative numbers
     * indicating offsets from the end of the sorted set, with -1 being the last
     * element of the sorted set, -2 the penultimate element and so on.
     * <p>
     * Out of range indexes will not produce an error. If start is larger than
     * the largest index in the sorted set, or start > stop, an empty list is
     * returned. If stop is larger than the end of the sorted set Redis will
     * treat it like it is the last element of the sorted set.
     * <p>
     * See {@link #zrangeWithScores(byte[], long, long)} when you need the
     * members and scores.
     * <p>
     * See {@link #zrevrange(byte[], long, long)} when you need the members
     * ordered from highest to lowest score.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements returned.
     * 
     * @param key
     *            of the sorted set
     * @param start
     *            of the range
     * @param end
     *            of the range
     * @return the specified range of members in the sorted set stored at key
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key is null
     */
    Set<byte[]> zrange(final byte[] key, final long start, final long end);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered
     * from high to low.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * The Method {@link #zrangeByScore(byte[], byte[], byte[], long, long)} can
     * be used to only get a range of the matching elements (similar to SELECT
     * LIMIT offset, count in SQL). Keep in mind that if offset is large, the
     * sorted set needs to be traversed for offset elements before getting to
     * the elements to return, which can add up to O(N) time complexity.
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method
     * {@link #zrangeByScoreWithScores(byte[], byte[], byte[], long, long)}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<byte[]> zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered
     * from high to low with limit set.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method
     * {@link #zrangeByScoreWithScores(byte[], byte[], byte[], long, long)}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * <p>
     * Keep in mind that if offset is large, the sorted set needs to be
     * traversed for offset elements before getting to the elements to return,
     * which can add up to O(N) time complexity.
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param offset
     *            of the limit
     * @param count
     *            of the limit
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<byte[]> zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max, final long offset, final long count);

    /**
     * Return a range of members and scores in a sorted set by score, with
     * scores ordered from high to low.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * The Method {@link #zrangeByScore(byte[], byte[], byte[], long, long)} can
     * be used to only get a range of the matching elements (similar to SELECT
     * LIMIT offset, count in SQL). Keep in mind that if offset is large, the
     * sorted set needs to be traversed for offset elements before getting to
     * the elements to return, which can add up to O(N) time complexity.
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method
     * {@link #zrangeByScoreWithScores(byte[], byte[], byte[], long, long)}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<Pair<byte[], Double>> zrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max);

    /**
     * Return a range of members and scores in a sorted set by score, with
     * scores ordered from high to low with limit set.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method
     * {@link #zrangeByScoreWithScores(byte[], byte[], byte[], long, long)}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * <p>
     * Keep in mind that if offset is large, the sorted set needs to be
     * traversed for offset elements before getting to the elements to return,
     * which can add up to O(N) time complexity.
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param offset
     *            of the limit
     * @param count
     *            of the limit
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<Pair<byte[], Double>> zrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max, final long offset,
	    final long count);

    /**
     * Returns the specified range of member/score pairs in the sorted set
     * stored at key. The members are considered to be ordered from the lowest
     * to the highest score.
     * <p>
     * Both start and stop are zero-based indexes, where 0 is the first element,
     * 1 is the next element and so on. They can also be negative numbers
     * indicating offsets from the end of the sorted set, with -1 being the last
     * element of the sorted set, -2 the penultimate element and so on.
     * <p>
     * Out of range indexes will not produce an error. If start is larger than
     * the largest index in the sorted set, or start > stop, an empty list is
     * returned. If stop is larger than the end of the sorted set Redis will
     * treat it like it is the last element of the sorted set.
     * <p>
     * See {@link #zrange(String, long, long)} when you need only the members.
     * <p>
     * See {@link #zrevrangeWithScores(String, long, long)} when you need the
     * members ordered from highest to lowest score.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements returned.
     * 
     * @param key
     *            of the sorted set
     * @param start
     *            of the range
     * @param end
     *            of the range
     * @return the specified range of member/score pairs in the sorted set
     *         stored at key
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key is null
     */
    Set<Pair<byte[], Double>> zrangeWithScores(final byte[] key,
	    final long start, final long end);

    /**
     * Returns the rank of member in the sorted set stored at key, with the
     * scores ordered from low to high.
     * <p>
     * The rank (or index) is 0-based, which means that the member with the
     * lowest score has rank 0.
     * <p>
     * Use {@link #zrevrange(String, long, long)} to get the rank of an element
     * with the scores ordered from high to low.
     * <p>
     * Time complexity: O(log(N))
     * 
     * @param key
     *            the key of the sorted set
     * @param member
     *            to get the rank for
     * @return the rank of member or null if member not in sorted set
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key or member is null
     */
    Long zrank(final byte[] key, final byte[] member);

    /**
     * Removes the member from the sorted set stored at key. If member is not a
     * member of the sorted set, no operation is performed.
     * <p>
     * Time complexity: O(log(N)) with N being the number of elements in the
     * sorted set.
     * 
     * @param key
     *            of the sorted set
     * @param member
     *            to remove
     * @return true if member was removed or false if member is not a member of
     *         the sorted set.
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key or member is null
     */
    Boolean zrem(final byte[] key, final byte[] member);

    /**
     * Removes all elements in the sorted set stored at key with rank between
     * start and stop.
     * <p>
     * Both start and stop are 0-based indexes with 0 being the element with the
     * lowest score. These indexes can be negative numbers, where they indicate
     * offsets starting at the element with the highest score. For example: -1
     * is the element with the highest score, -2 the element with the second
     * highest score and so forth.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements removed by the operation.
     * 
     * @param key
     *            of the sorted set
     * @param start
     *            rank
     * @param end
     *            rank
     * @return the number of elements removed.
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key or member is null
     */
    Long zremrangeByRank(final byte[] key, final long start, final long end);

    /**
     * Removes all elements in the sorted set stored at key with a score between
     * min and max.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements removed by the operation.
     * 
     * @param key
     *            of the sorted set
     * @param min
     *            score inclusive, add a '(' as prefix for exclusive
     * @param max
     *            score inclusive, add a '(' as prefix for exclusive
     * @return the number of elements removed.
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if any parameter is null
     */
    Long zremrangeByScore(final byte[] key, final byte[] min, final byte[] max);

    /**
     * Returns the specified range of members in the sorted set stored at key.
     * The members are considered to be ordered from the highest to the lowest
     * score.
     * <p>
     * See {@link #zrevrangeWithScores(String, long, long)} when you need the
     * members and scores.
     * <p>
     * See {@link #zrange(String, long, long)} when you need the members ordered
     * from lowest to highest score.
     * <p>
     * Both start and stop are zero-based indexes, where 0 is the first element,
     * 1 is the next element and so on. They can also be negative numbers
     * indicating offsets from the end of the sorted set, with -1 being the last
     * element of the sorted set, -2 the penultimate element and so on.
     * <p>
     * Out of range indexes will not produce an error. If start is larger than
     * the largest index in the sorted set, or start > stop, an empty list is
     * returned. If stop is larger than the end of the sorted set Redis will
     * treat it like it is the last element of the sorted set.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements returned.
     * 
     * @param key
     *            of the sorted set
     * @param start
     *            of the range
     * @param end
     *            of the range
     * @return the specified range of members in the sorted set stored at key
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key is null
     */
    Set<byte[]> zrevrange(final byte[] key, final long start, final long end);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered
     * from low to high.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * The Method {@link #zrevrangeByScore(byte[], byte[], byte[], long, long)}
     * can be used to only get a range of the matching elements (similar to
     * SELECT LIMIT offset, count in SQL). Keep in mind that if offset is large,
     * the sorted set needs to be traversed for offset elements before getting
     * to the elements to return, which can add up to O(N) time complexity.
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method {@link #zrevrangeByScoreWithScores(byte[], byte[], byte[])}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered
     * from low to high with limit set.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * The Method {@link #zrevrangeByScoreWithScores(byte[], byte[], byte[])}
     * return both the element and its score, instead of the element alone.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * <p>
     * Keep in mind that if offset is large, the sorted set needs to be
     * traversed for offset elements before getting to the elements to return,
     * which can add up to O(N) time complexity.
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param offset
     *            of the limit
     * @param count
     *            of the limit
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max, final long offset, final long count);

    /**
     * Return a range of members and scores in a sorted set by score, with
     * scores ordered from low to high.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * The Method
     * {@link #zrangeByScoreWithScores(byte[], byte[], byte[], long, long)} can
     * be used to only get a range of the matching elements (similar to SELECT
     * LIMIT offset, count in SQL). Keep in mind that if offset is large, the
     * sorted set needs to be traversed for offset elements before getting to
     * the elements to return, which can add up to O(N) time complexity.
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max);

    /**
     * Return a range of members and scores in a sorted set by score, with
     * scores ordered from high to low with limit set.
     * <p>
     * Returns all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max). The
     * elements are considered to be ordered from low to high scores.
     * <p>
     * The elements having the same score are returned in lexicographical order
     * (this follows from a property of the sorted set implementation in Redis
     * and does not involve further computation).
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * the highest or lowest score in the sorted set to get all elements from or
     * up to a certain score.
     * <p>
     * By default, the interval specified by min and max is closed (inclusive).
     * It is possible to specify an open interval (exclusive) by prefixing the
     * score with the character '('. For example: max=(1 and min=5 will return
     * all elements with 1 < score <= 5 while.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements being returned. If M is constant
     * (e.g. always asking for the first 10 elements with LIMIT), you can
     * consider it O(log(N)).
     * <p>
     * Keep in mind that if offset is large, the sorted set needs to be
     * traversed for offset elements before getting to the elements to return,
     * which can add up to O(N) time complexity.
     * 
     * @param key
     *            of the sset.
     * @param min
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param max
     *            score as String representation of a double, optional with a
     *            '(' prefix or +/-inf
     * @param offset
     *            of the limit
     * @param count
     *            of the limit
     * @return all the elements in the sorted set at key with a score between
     *         min and max.
     * @throws JedisException
     *             if key is other data type then sset or min/max is malformed
     * @throws NullPointerException
     *             if any parameter is null
     * 
     */
    Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max, final long offset,
	    final long count);

    /**
     * Returns the specified range of member/score pairs in the sorted set
     * stored at key. The members are considered to be ordered from the highest
     * to the lowest score.
     * <p>
     * See {@link #zrevrange(String, long, long)} when you need only the
     * members.
     * <p>
     * See {@link #zrangeWithScores(String, long, long)} when you need the
     * members ordered from lowest to highest score.
     * <p>
     * Both start and stop are zero-based indexes, where 0 is the first element,
     * 1 is the next element and so on. They can also be negative numbers
     * indicating offsets from the end of the sorted set, with -1 being the last
     * element of the sorted set, -2 the penultimate element and so on.
     * <p>
     * Out of range indexes will not produce an error. If start is larger than
     * the largest index in the sorted set, or start > stop, an empty list is
     * returned. If stop is larger than the end of the sorted set Redis will
     * treat it like it is the last element of the sorted set.
     * <p>
     * Time complexity: O(log(N)+M) with N being the number of elements in the
     * sorted set and M the number of elements returned.
     * 
     * @param key
     *            of the sorted set
     * @param start
     *            of the range
     * @param end
     *            of the range
     * @return the specified range of member/score pairs in the sorted set
     *         stored at key
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key is null
     */
    Set<Pair<byte[], Double>> zrevrangeWithScores(final byte[] key,
	    final long start, final long end);

    /**
     * Returns the rank of member in the sorted set stored at key, with the
     * scores ordered from high to low.
     * <p>
     * The rank (or index) is 0-based, which means that the member with the
     * highest score has rank 0.
     * <p>
     * Use {@link #zrange(String, long, long)} to get the rank of an element
     * with the scores ordered from low to hight.
     * <p>
     * Time complexity: O(log(N))
     * 
     * @param key
     *            the key of the sorted set
     * @param member
     *            to get the rank for
     * @return the rank of member or null if member not in sorted set
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key or member is null
     */
    Long zrevrank(final byte[] key, final byte[] member);

    /**
     * Returns the score of member in the sorted set at key.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     *            of the sorted set
     * @param member
     *            to get the score from
     * @return the score of member or null if member does not exist in the
     *         sorted set, or key does not exist.
     * @throws JedisException
     *             if key is not of data type sorted set
     * @throws NullPointerException
     *             if key or member is null
     */
    Double zscore(final byte[] key, final byte[] member);

    /**
     * Computes the union of numkeys sorted sets given by the specified key /
     * multiplicator {@link Pair pairs}, and stores the result in dstKey, the
     * resulting score of an element is the maximum of its scores in the sorted
     * sets where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N)+O(M log(M)) with N being the sum of the sizes of
     * the input sorted sets, and M being the number of elements in the
     * resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zunionstoreMax(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

    /**
     * Computes the union of numkeys sorted sets given by the specified key /
     * multiplicator {@link Pair pairs}, and stores the result in dstKey, the
     * resulting score of an element is the minimum of its scores in the sorted
     * sets where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N)+O(M log(M)) with N being the sum of the sizes of
     * the input sorted sets, and M being the number of elements in the
     * resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zunionstoreMin(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

    /**
     * Computes the union of numkeys sorted sets given by the specified key /
     * multiplicator {@link Pair pairs}, and stores the result in dstKey, the
     * resulting score of an element is the sum of its scores in the sorted sets
     * where it exists.
     * <p>
     * It is needed to specify a multiplication factor for each input sorted
     * set. This means that the score of every element in every input sorted set
     * is multiplied by this factor before being passed to the aggregation
     * function.
     * <p>
     * Because of the java bug/feature varargs + generics = no fun, the compiler
     * will tell you:
     * "Type safety : A generic array of Pair<String,Double> is created for a varargs parameter"
     * to supress this just add "@SuppressWarnings("unchecked")" before the
     * call.
     * <p>
     * Time complexity: O(N)+O(M log(M)) with N being the sum of the sizes of
     * the input sorted sets, and M being the number of elements in the
     * resulting sorted set.
     * 
     * @param dstKey
     *            to the resulting set. If dstKey already exists, it is
     *            overwritten.
     * @param ssetAndWeight1
     *            the first Pair of sorted set key and multiplication factor.
     * @param ssetAndWeightN
     *            all other Pairs of sorted set key and multiplication factor.
     * @return the number of elements in the resulting sorted set at dstKey.
     * 
     * @throws JedisException
     *             if any key is other then sorted set, except the destination
     *             key.
     * @throws NullPointerException
     *             if dstKey or ssetAndWeight1 is null
     * 
     */
    Long zunionstoreSum(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN);

}