# Jedis 5 Breaking Changes

- Both `bzpopmax(double timeout, String... keys)` and `bzpopmin(double timeout, String... keys)` now return `KeyValue<String, Tuple>` (instead of `KeyedZSetElement`).

- Both `bzpopmax(double timeout, byte[]... keys)` and `bzpopmin(double timeout, byte[]... keys)` now return `KeyValue<byte[], Tuple>` (instead of `List<byte[]>`).

- Following methods now return `KeyValue<String, String>` instead of `KeyedListElement`:
  - `blpop(double timeout, String key)`
  - `blpop(double timeout, String... keys)`
  - `brpop(double timeout, String key)`
  - `brpop(double timeout, String... keys)`

- Following methods now return `KeyValue<byte[], byte[]>` instead of `List<byte[]>`:
  - `blpop(double timeout, byte[]... keys)`
  - `brpop(double timeout, byte[]... keys)`

- `zdiff(String... keys)` method now returns `List<String>` (instead of `Set<String>`).
- `zdiff(byte[]... keys)` method now returns `List<byte[]>` (instead of `Set<byte[]>`).
- Both `zdiffWithScores(String... keys)` and `zdiffWithScores(byte[]... keys)` methods now return `List<Tuple>` (instead of `Set<Tuple>`).

- `zinter(ZParams params, String... keys)` method now returns `List<String>` (instead of `Set<String>`).
- `zinter(ZParams params, byte[]... keys)` method now returns `List<byte[]>` (instead of `Set<byte[]>`).
- Both `zinterWithScores(ZParams params, String... keys)` and `zinterWithScores(ZParams params, byte[]... keys)` methods now return `List<Tuple>` (instead of `Set<Tuple>`).

- `zunion(ZParams params, String... keys)` method now returns `List<String>` (instead of `Set<String>`).
- `zunion(ZParams params, byte[]... keys)` method now returns `List<byte[]>` (instead of `Set<byte[]>`).
- Both `zunionWithScores(ZParams params, String... keys)` and `zunionWithScores(ZParams params, byte[]... keys)` methods now return `List<Tuple>` (instead of `Set<Tuple>`).

- `getAgeSeconds()` in `AccessControlLogEntry` now returns `Double` instead of `String`.

- `graphSlowlog(String graphName)` now returns `List<List<Object>>` (instead of `List<List<String>>`).

- All _payload_ related parameters are removed from _search_ related classes; namely `Document`, `IndexDefinition`, `Query`.

- `KeyedZSetElement` class is removed.

- `KeyedListElement` class is removed.

- `STREAM_AUTO_CLAIM_ID_RESPONSE` in BuilderFactory has been renamed to `STREAM_AUTO_CLAIM_JUSTID_RESPONSE`.

- Following BuilderFactory implementations have been removed:
  - `BYTE_ARRAY` (use `BINARY`)
  - `BYTE_ARRAY_LIST` (use `BINARY_LIST`)
  - `BINARY_MAP_FROM_PAIRS`
  - `STRING_ORDERED_SET`

- `Queable` class is removed.

- `Params` abstract class is removed.
  - `toString()` support used by its sub-classes is now unavailable.

- `CommandListFilterByParams` now throws `IllegalArgumentException` (instead of `JedisDataException`) in case of unfulfilling filter.

- `FailoverParams` now throws `IllegalArgumentException` (instead of `IllegalStateException`) in case of unfulfilling optional arguments.

- `XPendingParams` now throws `IllegalArgumentException` (instead of `IllegalStateException`) in case of unfulfilling optional arguments.

- `getParams()` method is removed from `SortingParams` class.

<!--- Deprecated in Jedis 4 --->

- `quit()` method has been removed from `Connection` and `ServerCommands` interface and implementations.

- `select(int index)` method has been removed from `Connection`.

- `updatePassword(String password)` method has been removed from `JedisClientConfig` and implementations.

- `setPassword(String password)` method has been removed from `ConnectionFactory`.

- `setPassword(String password)` method has been removed from `JedisFactory`.

- `get()` option has been removed from `SetParams`.  Following methods have been added in Jedis/UnifiedJedis for convenience:
  - `setGet(String key, String value)` method has been added in `StringCommands` interface.
  - `setGet(byte[] key, byte[] value)` method has been added in `StringBinaryCommands` interface.

- `xpending(String key, String groupName, StreamEntryID start, StreamEntryID end, int count, String consumerName)` method has been removed from everywhere.
  - Use `xpending(java.lang.String, java.lang.String, redis.clients.jedis.params.XPendingParams)` instead.

- `xpending(byte[] key, byte[] groupName, byte[] start, byte[] end, int count, byte[] consumerName)` method has been removed from everywhere.
  - Use `xpending(byte[], byte[], redis.clients.jedis.params.XPendingParams)` instead.

- `topkCount(String key, String... items)` method has been removed from everywhere.

- Following methods have been removed:
  - `strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params)`
  - `strAlgoLCSStrings(byte[] strA, byte[] strB, StrAlgoLCSParams params)`
  - `strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params)`
  - `strAlgoLCSKeys(byte[] keyA, byte[] keyB, StrAlgoLCSParams params)`

- `StrAlgoLCSParams` has been removed.

- Following methods have been removed from all Pipeline classes:
  - `ftCursorRead(String indexName, long cursorId, int count)`
  - `ftCursorDel(String indexName, long cursorId)`
  - `ftDropIndex(String indexName)`
  - `ftDropIndexDD(String indexName)`
  - `ftAliasAdd(String aliasName, String indexName)`
  - `ftAliasUpdate(String aliasName, String indexName)`
  - `ftAliasDel(String aliasName)`

- `addObjects(int[] ints)` method has been removed from `CommandArguments`.

- `getArgsString()` and `serializeRedisArgs(List<byte[]> redisArgs)` methods have been removed from `AggregationBuilder`.

- `totalResults` variable in `AggregationResult` has been made private. Use `getTotalResults()` method instead.

- `retentionTime(long retentionTime)` method in `TSAlterParams` has been removed. Use `retention(long)` method instead.

- `JedisSentineled(String masterName, Set<HostAndPort> sentinels, JedisClientConfig masterClientConfig, JedisClientConfig sentinelClientConfig)` and
`JedisSentineled(String masterName, Set<HostAndPort> sentinels, GenericObjectPoolConfig<Connection> poolConfig, JedisClientConfig masterClientConfig, JedisClientConfig sentinelClientConfig)`
constructors have been removed.

- `JedisClusterInfoCache(JedisClientConfig clientConfig)` and `JedisClusterInfoCache(JedisClientConfig clientConfig, GenericObjectPoolConfig<Connection> poolConfig)`
constructors have been removed.
