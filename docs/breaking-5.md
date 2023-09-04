# Jedis 5 Breaking Changes

- All variants of `blmpop` and `bzmpop` methods now take `double timeout` parameter instead of `long timeout` parameter.
  This is breaking ONLY IF you are using `Long` for timeout.

- `Reducer` abstract class is refactored:
  - **`Reducer(String field)` constructor is removed; `Reducer(String name, String field)` constructor is added.**
  - **`Reducer(String name)` constructor is added; it will cause runtime error with older `Reducer(String field)` constructor.**
  - `getName` method is removed.
  - `getAlias` method is removed.
  - `setAlias` method is removed; use `as` method.
  - `setAliasAsField` method is removed.
  - `getOwnArgs` method is now abstract.
  - `getArgs` method is removed.

- `quit()` method has been removed from `Connection` and `ServerCommands` interface and implementations.

- `updatePassword(String password)` method has been removed from `JedisClientConfig` and implementations.

- `setPassword(String password)` method has been removed from both `JedisFactory` and `ConnectionFactory` classes.

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

- Both `configGet(String pattern)` and `configGet(String... patterns)` methods now return `Map<String, String>` instead of `List<String>`.
- Both `configGet(byte[] pattern)` and `configGet(byte[]... patterns)` methods now return `Map<byte[], byte[]>` instead of `List<byte[]>`.

- New `aclDelUser(String... names)` method replaces `aclDelUser(String name)` and `aclDelUser(String name, String... names)` methods.
- New `aclDelUser(byte[]... names)` method replaces `aclDelUser(byte[] name)` and `aclDelUser(byte[] name, byte[]... names)` methods.

- `tsMGet(TSMGetParams multiGetParams, String... filters)` method now returns `Map<String, TSMGetElement>` instead of `List<TSKeyValue<TSElement>>`.

- Following methods now return `Map<String, TSMRangeElements>` instead of `List<TSKeyedElements>`:
  - `tsMRange(long fromTimestamp, long toTimestamp, String... filters)`
  - `tsMRange(TSMRangeParams multiRangeParams)`
  - `tsMRevRange(long fromTimestamp, long toTimestamp, String... filters)`
  - `tsMRevRange(TSMRangeParams multiRangeParams)`

- `jsonNumIncrBy(String key, Path2 path, double value)` method now returns `Object` instead of `JSONArray`.
  - The returning object would still be JSONArray for all previous cases. So simple type casting is enough to handle this change.
  - The returning object will be `List<Double>` when running under RESP3 protocol.

- `getAgeSeconds()` in `AccessControlLogEntry` now returns `Double` instead of `String`.

- Both `ftConfigGet(String option)` and `ftConfigGet(String indexName, String option)` methods now return `Map<String, Object>` instead of `Map<String, String>`.

- `ftList()` method now returns `Set<String>` instead of `List<String>`.

- `graphSlowlog(String graphName)` now returns `List<List<Object>>` (instead of `List<List<String>>`).

- `CommandListFilterByParams` now throws `IllegalArgumentException` (instead of `JedisDataException`) in case of unfulfilling filter.

- `FailoverParams` now throws `IllegalArgumentException` (instead of `IllegalStateException`) in case of unfulfilling optional arguments.

- `XPendingParams` now throws `IllegalArgumentException` (instead of `IllegalStateException`) in case of unfulfilling optional arguments.

- `get()` option has been removed from `SetParams`.  Following methods have been added in Jedis/UnifiedJedis for convenience:
  - `setGet(String key, String value)` method has been added in `StringCommands` interface.
  - `setGet(byte[] key, byte[] value)` method has been added in `StringBinaryCommands` interface.

- `xpending(String key, String groupName, StreamEntryID start, StreamEntryID end, int count, String consumerName)` method has been removed from everywhere.
  - Use `xpending(java.lang.String, java.lang.String, redis.clients.jedis.params.XPendingParams)` instead.

- `xpending(byte[] key, byte[] groupName, byte[] start, byte[] end, int count, byte[] consumerName)` method has been removed from everywhere.
  - Use `xpending(byte[], byte[], redis.clients.jedis.params.XPendingParams)` instead.

- `retentionTime(long retentionTime)` method in `TSAlterParams` has been removed. Use `retention(long)` method instead.

- Following classes have been removed:
  - `KeyedZSetElement`
  - `KeyedListElement`
  - `TSKeyValue`
  - `TSKeyedElements`
  - `Limit`

- Following BuilderFactory implementations have been removed:
  - `BYTE_ARRAY` (use `BINARY`)
  - `BYTE_ARRAY_LIST` (use `BINARY_LIST`)
  - `BINARY_MAP_FROM_PAIRS`
  - `STRING_ORDERED_SET`

- All _payload_ related parameters are removed from _search_ related classes; namely `Document`, `IndexDefinition`, `Query`.

- `topkCount(String key, String... items)` method has been removed from everywhere.

- Following methods supporting JSON.RESP command have been removed:
  - `jsonResp(String key)`
  - `jsonResp(String key, Path path)`
  - `jsonResp(String key, Path2 path)`

- `RedisJsonCommands` and `RedisJsonPipelineCommands` interfaces have been moved into `redis.clients.jedis.json.commands` package.

- `AbortedTransactionException` is removed.

- `Queable` class is removed.

- `Params` abstract class is removed.
  - `toString()` support used by its sub-classes is now unavailable.

- `getParams()` method is removed from `SortingParams` class.

- Both `SEARCH_AGGREGATION_RESULT` and `SEARCH_AGGREGATION_RESULT_WITH_CURSOR` implementations from `SearchBuilderFactory` class have been moved to `AggregationResult` class.

- All `AggregationResult` constructors have been made `private`.

- `getArgs()`, `getArgsString()` and `serializeRedisArgs(List<byte[]> redisArgs)` methods have been removed from `AggregationBuilder`.

- `totalResults` variable in `AggregationResult` has been made private. Use `getTotalResults()` method instead.

- `getArgs()` and `limit(Limit limit)` methods have been removed from `Group` class.

- `addCommandEncodedArguments` and `addCommandBinaryArguments` methods have been removed from `FieldName` class.

- `addObjects(int[] ints)` method has been removed from `CommandArguments`.

- Following methods have been removed:
  - `strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params)`
  - `strAlgoLCSStrings(byte[] strA, byte[] strB, StrAlgoLCSParams params)`
  - `strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params)`
  - `strAlgoLCSKeys(byte[] keyA, byte[] keyB, StrAlgoLCSParams params)`

- `StrAlgoLCSParams` class has been removed.

- Following methods have been removed from all Pipeline classes:
  - `ftCursorRead(String indexName, long cursorId, int count)`
  - `ftCursorDel(String indexName, long cursorId)`
  - `ftDropIndex(String indexName)`
  - `ftDropIndexDD(String indexName)`
  - `ftAliasAdd(String aliasName, String indexName)`
  - `ftAliasUpdate(String aliasName, String indexName)`
  - `ftAliasDel(String aliasName)`

- `JedisSentineled(String masterName, Set<HostAndPort> sentinels, JedisClientConfig masterClientConfig, JedisClientConfig sentinelClientConfig)` and
`JedisSentineled(String masterName, Set<HostAndPort> sentinels, GenericObjectPoolConfig<Connection> poolConfig, JedisClientConfig masterClientConfig, JedisClientConfig sentinelClientConfig)`
constructors have been removed.

- `JedisClusterInfoCache(JedisClientConfig clientConfig)` and `JedisClusterInfoCache(JedisClientConfig clientConfig, GenericObjectPoolConfig<Connection> poolConfig)`
constructors have been removed.
