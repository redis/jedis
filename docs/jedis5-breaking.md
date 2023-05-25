# Jedis 5 Breaking Changes

- `StreamConsumersInfo` has been renamed to `StreamConsumerInfo`.

- `bzpopmax(double timeout, byte[]... keys)` now returns `List<Object>` (instead of `List<byte[]>`).
  - This is a three element list where the last element is a `Double`.

- `bzpopmin(double timeout, byte[]... keys)` now returns `List<Object>` (instead of `List<byte[]>`).
  - This is a three element list where the last element is a `Double`.

- `getAgeSeconds()` in `AccessControlLogEntry` now returns `Double` instead of `String`.

- `graphSlowlog(String graphName)` now returns `List<List<Object>>` (instead of `List<List<String>>`).

- `List<Object> getUnflushedObjectMultiBulkReply()` method has been removed from `Connection`.
  - `Object getUnflushedObject()` method has been added instead.

- `STREAM_INFO_FULL` in BuilderFactory has been renamed to `STREAM_FULL_INFO`.
- `STREAM_CONSUMERS_INFO_LIST` in BuilderFactory has been renamed to `STREAM_CONSUMER_INFO_LIST`.
- `STREAM_AUTO_CLAIM_ID_RESPONSE` in BuilderFactory has been renamed to `STREAM_AUTO_CLAIM_JUSTID_RESPONSE`.

- Following BuilderFactory implementations have been removed:
  - `BYTE_ARRAY` (use `BINARY`)
  - `BYTE_ARRAY_LIST` (use `BINARY_LIST`)
  - `BINARY_MAP_FROM_PAIRS`

<!--- Deprecated in Jedis 4 --->

- `quit()` method has been removed from `Connection` and `ServerCommands` interface and implementations.

- `select(int index)` method has been removed from `Connection`.

- `updatePassword(String password)` method has been removed from `JedisClientConfig` and implementations.

- `setPassword(String password)` method has been removed from `ConnectionFactory`.

- `setPassword(String password)` method has been removed from `JedisFactory`.

- `get()` option has been removed from `SetParams`.  Following methods have been added in Jedis/UnifiedJedis for convenience:
  - `setGet(String key, String value)` method has been added in `` interface.
  - `setGet(byte[] key, byte[] value)` method has been added in `` interface.

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
