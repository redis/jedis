# Jedis 4 Breaking Changes

- The `BinaryJedis` and `BinaryJedisCluster` classes have been removed.

  The methods from these classes are available in the `Jedis` and `JedisCluster` classes
  respectively.

- The following cases now throws an `IllegalStateException` instead of a
`JedisDataException`.
  - `Cannot use Jedis when in Multi. Please use Transaction or reset Jedis state.`
  - `Cannot use Jedis when in Pipeline. Please use Pipeline or reset Jedis state.`

- The Redis transaction methods `multi()`, `exec()` and `discard()` have
  been removed from `Pipeline`.

- The `execGetResponse()` method has been removed from `Transaction`.

- The `watch()` and `unwatch()` methods from the `Transaction` class are unsupported
within MULTI (i.e., after the `multi()` method). However, `watch()` and `unwatch
()` can still be used before calling MULTI.

- The `JedisCluster` constructors with `GenericObjectPoolConfig<Jedis>` now accept
  `GenericObjectPoolConfig<Connection>`.

- All `JedisCluster` constructors now throw a `JedisClusterOperationException` if
unable to connect to any of the provided `HostAndPort(s)`. Previously, the
connection would go into an unusable state.

- `JedisCluster.getClusterNodes()` returns `Map<String, ConnectionPool>` instead of
  `Map<String, JedisPool>`.

- `JedisCluster.getConnectionFromSlot(int)` returns `Connection` instead of `Jedis`.

- `JedisNoReachableClusterNodeException` has been removed.
`JedisClusterOperationException`, with a similar message, is thrown instead.

- `JedisClusterMaxAttemptsException` has been removed.
`JedisClusterOperationException`, with a similar message, is thrown instead.

- `JedisExhaustedPoolException` has been removed. A `JedisException` with a similar message is thrown
  instead.

- [Many sorted set methods](v3-to-v4-zset-list.md) return a Java `List` instead of a
`Set`. [See the complete list](v3-to-v4-zset-list.md).

- [Many methods return primitive values](v3-to-v4-primitives.md)) (`long`/`boolean`/`double` instead of
`Long`/`Boolean`/
  `Double`). [See the complete list](v3-to-v4-primitives.md).

- `scriptExists(byte[])` method now returns `Boolean` instead of `Long`.

- `scriptExists(byte[]...)` method now returns `List<Boolean>` instead of `List<Long>`.

- In the [`xadd`](https://redis.io/commands/XADD) method with StreamEntryID
parameter, sending untyped `null` raises an exception.

  Casting the `null` to StreamEntryID (`(StreamEntryID) null`) resolves this issue.

- In the [`xrange`](https://redis.io/commands/XRANGE) and
  [`xrevrange`](https://redis.io/commands/xrevrange) methods with StreamEntryID parameters, sending
  untyped `null`s for both start and end parameters raises an exception.

  Casting the `null`s to StreamEntryID (`(StreamEntryID) null`) resolves this issue.

- The return type of `Jedis.shutdown()` is now `void`. Previously, it would return null.

- The `eval` and `evalsha` methods are now non-blocking. These methods were
blocking in Jedis 3.x.

- The `HostAndPort.localhost` constant has been removed.

- The following methods have been removed from HostAndPort class:
  - `extractParts()`
  - `parseString()`
  - `convertHost()`
  - `setLocalhost()`
  - `getLocalhost()`
  - `getLocalHostQuietly()`

- The following classes have been moved to the `redis.clients.jedis.args` package.
  - `BitOP`
  - `GeoUnit`
  - `ListPosition`

- The following classes have been moved to the `redis.clients.jedis.params` package.
  - `BitPosParams`
  - `ScanParams`
  - `SortingParams`
  - `ZParams`

- The following classes have been moved to the `redis.clients.jedis.resps` package.
  - `AccessControlLogEntry`
  - `AccessControlUser`
  - `GeoRadiusResponse`
  - `ScanResult`
  - `Slowlog`
  - `StreamConsumersInfo`
  - `StreamEntry`
  - `StreamGroupInfo`
  - `StreamInfo`
  - `StreamPendingEntry`
  - `StreamPendingSummary`
  - `Tuple`

- Jedis and JedisPool constructors with a `String` parameter, and no `int`
parameter, only support a URL or URI string.
  - Jedis(String)
  - JedisPool(String)
  - JedisPool(String, SSLSocketFactory, SSLParameters, HostnameVerifier)
  - JedisPool(GenericObjectPoolConfig<Jedis>, String)

- The `Client` and `BinaryClient` classes have been removed.

- `redis.clients.jedis.commands` package has been reimplemented, meaning that the
`Commands` interfaces have been restructured.

- The `ShardedJedisPool`, `Sharded`, `ShardedJedis`, `BinaryShardedJedis`, `ShardInfo`,
`JedisShardInfo` classes have been removed.
  - Introduced `JedisSharding` class to replace `ShardedJedisPool`.

    Earlier code without the use of "name" and "weight" (in ShardInfo/JedisShardInfo) are
    transferable to the new class.

- `ShardedJedisPipeline` class has been removed.
  - Introduced `ShardedPipeline` class to replace `ShardedJedisPipeline`.

- The type of `Protocol.CHARSET` has been changed to `java.nio.charset.Charset`.

- `Jedis.debug(DebugParams)` method has been removed.

- The `DebugParams` class has been removed.

- The `Jedis.sync()` method has been removed.

- The `Jedis.pubsubNumSub(String...)` method now returns `Map<String, Long>`
instead of `Map<String, String>`.

- `setDataSource` method in Jedis class now has `protected` access.

- `JedisPoolAbstract` class has been removed. Use `Pool<Jedis>`.

- The `Pool.initPool()` method has been removed.

- The `Pool.getNumActive()` method now returns `0` (via GenericObjectPool) when the
pool is closed.

- The `Connection.getRawObjectMultiBulkReply()` method has been removed. Use
  `Connection.getUnflushedObjectMultiBulkReply()` method.

- The `Queable.getResponse(Builder<T> builder)` method has been renamed to
  `Queable.enqueResponse(Builder<T> builder)`.

- All methods in Queable are now `final`:
  - `clean()`
  - `generateResponse(Object data)`
  - `enqueResponse(Builder<T> builder)`
  - `getPipelinedResponseLength()`

- These BuilderFactory implementations have been removed:
  - `OBJECT` (use `RAW_OBJECT`)
  - `BYTE_ARRAY_ZSET` (use `BINARY_LIST` or `BINARY_SET`)
  - `BYTE_ARRAY_MAP` (use `BINARY_MAP`)
  - `STRING_ZSET` (use `STRING_LIST` or `STRING_SET`)
  - `EVAL_RESULT` (use `ENCODED_OBJECT`)
  - `EVAL_BINARY_RESULT` (use `RAW_OBJECT`)

- All String variables representing Cluster, Sentinel and PubSub subcommands in Protocol class
  have been removed.

- `ClientKillParams.Type` has been removed. Use `ClientType`.

- `ClusterReset` has been removed. Use `ClusterResetType`.

- The `JedisClusterHostAndPortMap` interface has been removed. Use the
`HostAndPortMapper` interface.

- `JedisClusterHashTagUtil` class has been renamed to `JedisClusterHashTag`.

- The `KeyMergeUtil` class has been removed.
