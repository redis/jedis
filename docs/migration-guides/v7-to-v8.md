# Jedis 8.0.0 Migration Guide

This guide helps you migrate from Jedis 7.5.0 to Jedis 8.0.0. Version 8.0.0 introduces several breaking changes focused on completing the client class consolidation, modernizing protocol defaults, and tightening security defaults.

## Table of Contents

- [Overview](#overview)
- [Breaking Changes](#breaking-changes)
  - [RESP3 Negotiated by Default](#resp3-negotiated-by-default)
  - [Removed Client Classes (`JedisPooled`, `JedisSentineled`)](#removed-client-classes-jedispooled-jedissentineled)
  - [Removed `UnifiedJedis` Public Constructors](#removed-unifiedjedis-public-constructors)
  - [`Transaction(Jedis)` and `ClusterPipeline(ClusterConnectionProvider)` Constructors Removed](#transactionjedis-and-clusterpipelineclusterconnectionprovider-constructors-removed)
  - [`CommandObjects` Protocol is Now Immutable](#commandobjects-protocol-is-now-immutable)
  - [`broadcastCommand` and `JedisBroadcastAndRoundRobinConfig` Removed](#broadcastcommand-and-jedisbroadcastandroundrobinconfig-removed)
  - [Removed Sharding Utility Classes](#removed-sharding-utility-classes)
  - [`commons-pool2` Upgraded to 2.13.1](#commons-pool2-upgraded-to-2131)
- [Deprecations](#deprecations)
  - [`JedisCluster` Deprecation](#jediscluster-deprecation)
  - [RediSearch Cursor APIs Deprecated](#redisearch-cursor-apis-deprecated)
- [New Features](#new-features)
  - [Request/Response Policy Support for Cluster Commands](#requestresponse-policy-support-for-cluster-commands)
  - [Out-of-Band Push Notification Handling](#out-of-band-push-notification-handling)
  - [Cursor-Based `FT.AGGREGATE` Iterator](#cursor-based-ftaggregate-iterator)
  - [`CONFIG GET`/`SET` on `UnifiedJedis`](#config-getset-on-unifiedjedis)
  - [Shared Executor for `ClusterPipeline`](#shared-executor-for-clusterpipeline)
  - [Pool Access on `RedisSentinelClient`](#pool-access-on-redissentinelclient)

## Overview

Jedis 8.0.0 is a major release that finishes the client-class consolidation started in 7.x and modernizes a few long-standing defaults. The main focus areas are:

1. **RESP3 auto-negotiation on by default** for all `UnifiedJedis`-based clients (with graceful RESP2 fallback)
2. **Final removal of legacy `JedisPooled` / `JedisSentineled` classes** in favor of the `RedisClient` family introduced in 7.0.0
3. **Removal of deprecated `UnifiedJedis` public constructors** — clients are now created exclusively through the dedicated client classes or their builders
4. **Internal refactoring** that affects users of low-level extension points (custom `CommandExecutor`, `Transaction` subclasses, `CommandObjects`)

## Breaking Changes

### RESP3 Negotiated by Default

`UnifiedJedis`-based clients (`RedisClient`, `RedisClusterClient`, `RedisSentinelClient`, `MultiDbClient`, and the deprecated `JedisCluster`) now **auto-negotiate the RESP3 protocol by default**.

The negotiation logic:

1. When `JedisClientConfig.getRedisProtocol() == null` and `isAutoNegotiateProtocol() == true` (the new default), the client sends `HELLO 3` on connect and gracefully falls back to RESP2 if the server rejects it.
2. When `getRedisProtocol()` is `RESP2` or `RESP3`, that version is enforced strictly — the connection fails if the server rejects it.
3. When `getRedisProtocol() == null` and `isAutoNegotiateProtocol() == false`, the legacy "no HELLO" behavior is preserved (RESP2 assumed on the wire).

The legacy `Jedis` class doesn't fully support RESP3 protocol format and therefore ignores the auto-negotiation flag; it emits a warning if it is left enabled. To suppress the warning in `Jedis`-based code paths, use the new `serverDefaultProtocol()` shortcut:

```java
JedisClientConfig config = DefaultJedisClientConfig.builder()
    .serverDefaultProtocol()   // equivalent to .protocol(null).autoNegotiateProtocol(false)
    .build();
Jedis jedis = new Jedis(host, port, config);
```

#### New `JedisClientConfig` API

```java
public interface JedisClientConfig {
    // ...existing...
    default boolean isAutoNegotiateProtocol() { return true; }  // NEW, default = true
}
```

```java
// New builder methods on DefaultJedisClientConfig.Builder
Builder resp2();                              // shortcut for protocol(RESP2)
Builder serverDefaultProtocol();              // legacy "no HELLO" mode
Builder autoNegotiateProtocol(boolean value);
```

If your application relied on RESP2 reply shapes for commands whose RESP3 shape differs (e.g. `HRANDFIELD WITHVALUES`, `JSON.TYPE`, `XREAD`/`XREADGROUP`, certain RediSearch shapes), pin the protocol explicitly:

```java
JedisClientConfig config = DefaultJedisClientConfig.builder().resp2().build();
RedisClient client = RedisClient.builder().clientConfig(config).build();
```

#### Other RESP3-related fixes folded into this change

- `JSON.NUMINCRBY` now correctly returns the original numeric type (long or double) on RESP3 instead of always returning a double.
- RediSearch `NOCONTENT` responses now return an empty map on RESP3 instead of throwing on field access; missing scores default to `0.0` and missing warnings default to an empty list on both protocols.
- The deprecated `setProtocol(RedisProtocol)` mutator on `Jedis` was removed as part of the same change set.

### Removed Client Classes (`JedisPooled`, `JedisSentineled`)

`JedisPooled` and `JedisSentineled` were deprecated in 7.0.0 and have now been **completely removed**. Use the corresponding classes from the `RedisClient` family introduced in 7.0.0.

| v7.5.0                                | v8.0.0                                 |
| ------------------------------------- | -------------------------------------- |
| `redis.clients.jedis.JedisPooled`     | `redis.clients.jedis.RedisClient`      |
| `redis.clients.jedis.JedisSentineled` | `redis.clients.jedis.RedisSentinelClient` |

#### Migration Path

**Before (v7.5.0):**
```java
// Standalone
JedisPooled jedis = new JedisPooled("localhost", 6379);
jedis.set("key", "value");

// Sentinel
JedisSentineled sentinel = new JedisSentineled(
    "mymaster", masterConfig, sentinels, sentinelConfig);
```

**After (v8.0.0):**
```java
// Standalone — simple factory
RedisClient jedis = RedisClient.create("localhost", 6379);
jedis.set("key", "value");

// Standalone — builder for advanced configuration
RedisClient jedis = RedisClient.builder()
    .hostAndPort("localhost", 6379)
    .clientConfig(DefaultJedisClientConfig.builder()
        .user("myuser")
        .password("mypassword")
        .build())
    .build();

// Sentinel — builder
RedisSentinelClient sentinel = RedisSentinelClient.builder()
    .masterName("mymaster")
    .sentinels(sentinels)
    .clientConfig(masterConfig)
    .sentinelClientConfig(sentinelConfig)
    .build();
```

### Removed `UnifiedJedis` Public Constructors

All previously deprecated public constructors on `UnifiedJedis` have been removed. The class is no longer intended to be instantiated directly by users — use `RedisClient`, `RedisClusterClient`, `RedisSentinelClient`, or `MultiDbClient` (and their builders) instead.

#### Removed constructors

```java
// REMOVED in v8.0.0
UnifiedJedis()
UnifiedJedis(HostAndPort hostAndPort)
UnifiedJedis(String url)
UnifiedJedis(URI uri)
UnifiedJedis(URI uri, JedisClientConfig config)
UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig)
UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig, CacheConfig cacheConfig)
UnifiedJedis(HostAndPort hostAndPort, JedisClientConfig clientConfig, Cache cache)
UnifiedJedis(ConnectionProvider provider)
UnifiedJedis(JedisSocketFactory socketFactory)
UnifiedJedis(JedisSocketFactory socketFactory, JedisClientConfig clientConfig)
UnifiedJedis(ClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration)
UnifiedJedis(CommandExecutor executor)
UnifiedJedis(CommandExecutor executor, ConnectionProvider provider, CommandObjects commandObjects)
```

The following two constructors changed visibility from `public` to `protected`:

```java
// Now protected (still callable from subclasses)
UnifiedJedis(Connection connection)
UnifiedJedis(CommandExecutor executor, ConnectionProvider provider, CommandObjects commandObjects)
```

#### Migration Path

**Before (v7.5.0):**
```java
UnifiedJedis jedis = new UnifiedJedis(new HostAndPort("localhost", 6379));
UnifiedJedis jedis2 = new UnifiedJedis(URI.create("redis://localhost:6379"));
UnifiedJedis custom = new UnifiedJedis(myConnectionProvider);
```

**After (v8.0.0):**
```java
RedisClient jedis = RedisClient.create(new HostAndPort("localhost", 6379));
RedisClient jedis2 = RedisClient.create("redis://localhost:6379");
RedisClient custom = RedisClient.builder()
    .connectionProvider(myConnectionProvider)
    .build();
```

### `Transaction(Jedis)` and `ClusterPipeline(ClusterConnectionProvider)` Constructors Removed

#### `Transaction(Jedis)`

The legacy `public Transaction(Jedis jedis)` constructor — which Jedis used internally to wire `resetState()` callbacks — has been removed. The coupling is now handled inside `Jedis.multi()` via the new `onAfterExec()` / `onAfterDiscard()` hook methods.

If you subclassed `Transaction` or constructed it manually with a `Jedis` reference, switch to the `Connection`-based constructor and override the hooks if you need exec/discard notifications.

**Before (v7.5.0):**
```java
public Transaction(Jedis jedis); // public, took a Jedis
```

**After (v8.0.0):**
```java
public Transaction(Connection connection);            // existing
public Transaction(Connection connection, boolean doMulti);
public Transaction(Connection connection, boolean doMulti, boolean closeConnection);

// New protected hooks to override:
protected void onAfterExec();
protected void onAfterDiscard();
```

Additionally, the deprecated `protected AbstractTransaction()` no-arg constructor has been removed — subclasses must call `AbstractTransaction(CommandObjects commandObjects)`.

#### `ClusterPipeline(ClusterConnectionProvider)`

The single-argument `public ClusterPipeline(ClusterConnectionProvider provider)` constructor has been removed. It silently constructed a `ClusterCommandObjects` with no protocol information, which is no longer valid now that `CommandObjects` requires an explicit protocol (see [`CommandObjects` Protocol is Now Immutable](#commandobjects-protocol-is-now-immutable)).

A new overload accepts the protocol explicitly:

**Before (v7.5.0):**
```java
new ClusterPipeline(provider);
```

**After (v8.0.0):**
```java
new ClusterPipeline(provider, RedisProtocol.RESP3);    // or RESP2
// or, if you already have a ClusterCommandObjects:
new ClusterPipeline(provider, clusterCommandObjects);  // unchanged
```

### `CommandObjects` Protocol is Now Immutable

The internal `CommandObjects` class no longer allows mutating the protocol after construction:

- The no-arg `public CommandObjects()` constructor has been removed.
- The `public final void setProtocol(RedisProtocol)` setter has been removed.
- `public CommandObjects(RedisProtocol protocol)` now **throws `IllegalArgumentException` if `protocol` is `null`** — use `RedisProtocol.orServerDefault(protocol)` (which falls back to `RESP2`) when you do not know the negotiated protocol up front.

This only affects code that integrates with Jedis internals (custom `UnifiedJedis` subclasses, custom `CommandExecutor`, instrumentation). Application code that uses the `RedisClient*` builders is unaffected.

**Before (v7.5.0):**
```java
CommandObjects commandObjects = new CommandObjects();
// ... later
commandObjects.setProtocol(RedisProtocol.RESP3);
```

**After (v8.0.0):**
```java
CommandObjects commandObjects = new CommandObjects(RedisProtocol.orServerDefault(protocol));
```

A new constant `RedisProtocol.REDIS_SERVER_DEFAULT_PROTO` (= `RESP2`) and a helper `RedisProtocol.orServerDefault(RedisProtocol)` are provided for this purpose.

### `broadcastCommand` and `JedisBroadcastAndRoundRobinConfig` Removed

Broadcast routing is no longer a separate API surface — it is now driven entirely by the new request/response policy registry (see [Request/Response Policy Support for Cluster Commands](#requestresponse-policy-support-for-cluster-commands)). The following has been removed:

- The `JedisBroadcastAndRoundRobinConfig` interface (including the `RediSearchMode.DEFAULT` / `RediSearchMode.LIGHT` enum) is **deleted**.
- `UnifiedJedis.setBroadcastAndRoundRobinConfig(JedisBroadcastAndRoundRobinConfig)` is **removed**.
- The default `broadcastCommand(CommandObject<T>)` method on the `CommandExecutor` interface is **removed**.
- `UnifiedJedis.broadcastCommand(...)` is **removed** — broadcast routing now flows through `executeCommand(...)` and is resolved by the registry.

#### Migration Path

In v7.x, users running RediSearch against Redis 7 (where `FT.CREATE` only created the index on the contacted shard) had to opt into broadcasting indexes manually:

```java
// v7.5.0 — manual broadcast opt-in for cluster + RediSearch
JedisBroadcastAndRoundRobinConfig cfg = () -> RediSearchMode.DEFAULT;
jedis.setBroadcastAndRoundRobinConfig(cfg);
jedis.ftCreate("idx", IndexOptions.defaultOptions(), schema);
```

This is no longer necessary in v8.0.0. **Redis 8.0+ broadcasts `FT.CREATE` automatically on the server side** as well, so the index is created cluster-wide regardless. Simply drop the configuration:

```java
// v8.0.0 — no special configuration required
jedis.ftCreate("idx", IndexOptions.defaultOptions(), schema);
```

Custom `CommandExecutor` implementations that overrode `broadcastCommand` should remove the override.

### Removed Sharding Utility Classes

The following utility classes — leftovers from the `JedisSharding` feature removed in 7.0.0 — have been deleted:

- `redis.clients.jedis.util.Hashing`
- `redis.clients.jedis.util.MurmurHash`

If you happened to use these utility classes for application-level hashing, copy them into your own codebase or switch to an equivalent library (Guava's `Hashing.murmur3_*`, etc.).

### `commons-pool2` Upgraded to 2.13.1

The `org.apache.commons:commons-pool2` dependency has been bumped from 2.12.1 to **2.13.1**. The connection-close exception handling inside `MultiDbCommandExecutor` was adjusted accordingly. If you pin `commons-pool2` directly in your `pom.xml` / `build.gradle`, update it to 2.13.1 to stay consistent with what Jedis is tested against.

## Deprecations

### `JedisCluster` Deprecation

`JedisCluster` is now `@Deprecated` in favor of `RedisClusterClient`. The class still works, but new code should use `RedisClusterClient`.

**Before (v7.5.0):**
```java
JedisCluster cluster = new JedisCluster(nodes, clientConfig);
```

**After (v8.0.0):**
```java
RedisClusterClient cluster = RedisClusterClient.builder()
    .nodes(nodes)
    .clientConfig(clientConfig)
    .build();
```

### RediSearch Cursor APIs Deprecated

The low-level cursor methods on `RediSearchCommands` are deprecated because they don't work reliably on cluster deployments (the cursor lives on a specific shard, but the next command may be routed elsewhere):

```java
// Deprecated in v8.0.0
AggregationResult ftCursorRead(String indexName, long cursorId, int count);
String           ftCursorDel(String indexName, long cursorId);
```

The companion search-side iterator class is deprecated for the same reasons:

```java
// Deprecated in v8.0.0
redis.clients.jedis.search.FtSearchIteration
```

For `FT.SEARCH`, call `UnifiedJedis#ftSearch(String, String, FTSearchParams)` directly and paginate via `FTSearchParams.limit(offset, count)`. For `FT.AGGREGATE`, use the new `ftAggregateIterator(...)` method, which encapsulates cursor lifecycle and connection affinity:

**Before (v7.5.0):**
```java
AggregationBuilder b = new AggregationBuilder("*").cursor(100);
AggregationResult page = jedis.ftAggregate(idx, b);
while (page.getCursorId() != 0) {
    page = jedis.ftCursorRead(idx, page.getCursorId(), 100);
}
```

**After (v8.0.0):**
```java
AggregationBuilder b = new AggregationBuilder("*").cursor(100);
try (AggregateIterator it = jedis.ftAggregateIterator(idx, b)) {
    while (it.hasNext()) {
        AggregationResult page = it.next();
        // process page
    }
}
```

`AggregationResult` also gains an `isEmpty()` convenience method.

## New Features

### Request/Response Policy Support for Cluster Commands

Jedis 8.0.0 introduces a comprehensive request/response policy framework that aligns with the Redis [command tips](https://redis.io/docs/latest/develop/reference/key-specs/) — every command now declares how it should be routed (single shard, all shards, all masters, keyless) and how multi-shard responses should be aggregated (logical-AND, sum, set-union, etc.).

Highlights:
- New `CommandFlagsRegistry` / `StaticCommandFlagsRegistry` infrastructure with auto-generated metadata.
- New `Aggregator` family (`SumAggregator`, `SetAggregator`, `LogicalAndAggregator`, `MapAggregator`, `MultiNodeResultAggregator`, …) under `redis.clients.jedis.executors.aggregators`.
- New `ConnectionResolver` strategies (`SlotBasedConnectionResolver`, `RoundRobinConnectionResolver`, `ReplicaOnlyConnectionResolver`, …) under `redis.clients.jedis.executors`.
- `CommandArguments.isKeyless()` to inspect routing intent.
- New exceptions: `ClusterAggregationException`, `UnsupportedAggregationException`.

For most users this is transparent — cluster commands that previously needed manual fan-out (`INFO`, `CONFIG SET`, `DBSIZE`, etc.) now work correctly out of the box on `RedisClusterClient`.

### Out-of-Band Push Notification Handling

`Connection` and `Protocol` now understand out-of-band RESP3 push messages and route them through a configurable `PushConsumerChain`. Unsupported push types are silently dropped instead of failing the connection. This makes it safe to enable features such as `CLIENT TRACKING` on regular connections — invalidation pushes are no longer treated as protocol errors.

New (experimental) APIs:

```java
@Experimental public interface PushConsumer {
    PushConsumerContext handle(PushConsumerContext context);
}
@Experimental public interface PushConsumerChain { /* ... */ }
@Experimental public final class PushMessage { /* ... */ }
@Experimental public final class PushMessageTypes { /* ... */ }
```

### Cursor-Based `FT.AGGREGATE` Iterator

See [RediSearch Cursor APIs Deprecated](#redisearch-cursor-apis-deprecated). The new `AggregateIterator` (returned by `ftAggregateIterator(...)`) is cluster-safe and implements `AutoCloseable`, so it can be used in a try-with-resources block.

### `CONFIG GET`/`SET` on `UnifiedJedis`

`UnifiedJedis` (and therefore all `RedisClient*` clients) now exposes:

```java
public Map<String, String> configGet(String pattern);
public Map<String, String> configGet(String... patterns);
public String configSet(Map<String, String> parameterValues);
```

On `RedisClusterClient`, `configGet` is routed to a random node (configuration is assumed to be uniform across the cluster), while `configSet` fans out to all masters so the change is applied cluster-wide (see the [request/response policy framework](#requestresponse-policy-support-for-cluster-commands)).

### Shared Executor for `ClusterPipeline`

`JedisCluster.pipelined()` / `RedisClusterClient.pipelined()` previously created and tore down a private `ExecutorService` for every pipeline, which was wasteful for short pipelines under load. A new overload accepts a user-supplied executor:

```java
public ClusterPipeline pipelined(ExecutorService executorService);
```

Pass a shared executor when you create many short pipelines; reuse semantics are documented on the method.

### Pool Access on `RedisSentinelClient`

`RedisSentinelClient` now exposes the underlying primary-node connection pools (issue #4469):

```java
public Map<?, Pool<Connection>> getPrimaryNodesConnectionMap();
```

The returned map is keyed by master `HostAndPort` and yields the `Pool<Connection>` backing each primary, allowing instrumentation, draining, and pool-size monitoring without subclassing the provider.

## Getting Help

If you encounter issues during migration create an issue or [start a discussion](https://github.com/redis/jedis/discussions/new?category=q-a).
