# Jedis 7.0.0 Migration Guide

This guide helps you migrate from Jedis 6.2.0 to Jedis 7.0.0. Version 7.0.0 includes several breaking changes focused on removing deprecated features and improving the API design.

## Table of Contents

- [Overview](#overview)
- [Breaking Changes](#breaking-changes)
  - [Removed Deprecated Sharding/Sharded Features](#removed-deprecated-shardingsharded-features)
  - [Base Class Changes](#base-class-changes)
  - [UnifiedJedis Constructor Changes](#unifiedjedis-constructor-changes) 
  - [Return Type Changes](#return-type-changes)
- [New Features](#new-features)
  - [Automatic Failover and Failback](#automatic-failover-and-failback)
  - [Builder Pattern for Client Creation](#builder-pattern-for-client-creation)

## Overview

Jedis 7.0.0 is a major release that removes previously deprecated features and modernizes the API. The main focus areas are:

1. **Removal of deprecated sharding features** - JedisSharding and related classes have been removed
2. **Base class consolidation** - Pipeline and Transaction base classes have been renamed
3. **Builder pattern introduction** - New fluent builders for JedisPooled, JedisCluster, and JedisSentineled
4. **API cleanup** - Removal of deprecated constructors and methods

## Breaking Changes

### Removed Deprecated Sharding Features

The following classes and features have been **completely removed** in Jedis 7.0.0:

#### Removed Classes

- `JedisSharding` - Use `JedisCluster` for distributed Redis deployments instead
- `ShardedPipeline` - Use `Pipeline` with `JedisCluster` instead
- `ShardedConnectionProvider` - No replacement needed
- `ShardedCommandArguments` - Internal class, no replacement needed
- `ShardedCommandObjects` - Internal class, no replacement needed

#### Migration Path

If you were using `JedisSharding`:

**Before (v6.2.0):**
```java
List<HostAndPort> shards = Arrays.asList(
    new HostAndPort("localhost", 6379),
    new HostAndPort("localhost", 6380)
);
JedisSharding jedisSharding = new JedisSharding(shards);
jedisSharding.set("key", "value");
```

**After (v7.0.0):**
```java
// Option 1: Use JedisCluster for distributed deployments
Set<HostAndPort> nodes = new HashSet<>(Arrays.asList(
    new HostAndPort("localhost", 6379),
    new HostAndPort("localhost", 6380)
));
JedisCluster jedisCluster = new JedisCluster(nodes);
jedisCluster.set("key", "value");

// Option 2: Use JedisPooled for single-node deployments
JedisPooled jedis = new JedisPooled("localhost", 6379);
jedis.set("key", "value");
```

### Base Class Changes

Several base classes have been renamed to better reflect their purpose:

#### Pipeline Base Class

- `PipelineBase` has been **removed**
- `Pipeline` now extends `AbstractPipeline` instead

**Impact:** If you were using `PipelineBase` as a type reference, change it to `AbstractPipeline`.

**Before (v6.2.0):**
```java
PipelineBase pipeline = jedis.pipelined();
```

**After (v7.0.0):**
```java
AbstractPipeline pipeline = jedis.pipelined();
// Or use the concrete type:
Pipeline pipeline = (Pipeline) jedis.pipelined();
```

#### Transaction Base Class

- `TransactionBase` has been **removed**
- `Transaction` now extends `AbstractTransaction` instead

**Impact:** If you were using `TransactionBase` as a type reference, change it to `AbstractTransaction`.

**Before (v6.2.0):**
```java
TransactionBase transaction = jedis.multi();
```

**After (v7.0.0):**
```java
AbstractTransaction transaction = jedis.multi();
// Or use the concrete type:
Transaction transaction = (Transaction) jedis.multi();
```

### UnifiedJedis Constructor Changes

Several deprecated constructors have been removed from `UnifiedJedis`:

#### Removed Constructors

1. **Cluster constructors with maxAttempts:**
   ```java
   // REMOVED in v7.0.0
   UnifiedJedis(Set<HostAndPort> nodes, JedisClientConfig config, int maxAttempts)
   UnifiedJedis(Set<HostAndPort> nodes, JedisClientConfig config, int maxAttempts, Duration maxTotalRetriesDuration)
   UnifiedJedis(Set<HostAndPort> nodes, JedisClientConfig config, GenericObjectPoolConfig<Connection> poolConfig, int maxAttempts, Duration maxTotalRetriesDuration)
   ```

2. **Sharding constructors:**
   ```java
   // REMOVED in v7.0.0
   UnifiedJedis(ShardedConnectionProvider provider)
   UnifiedJedis(ShardedConnectionProvider provider, Pattern tagPattern)
   ```

#### Migration Path

**Before (v6.2.0):**
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("localhost", 6379));
JedisClientConfig config = DefaultJedisClientConfig.builder().build();

UnifiedJedis jedis = new UnifiedJedis(nodes, config, 3);
```

**After (v7.0.0):**
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("localhost", 6379));
JedisClientConfig config = DefaultJedisClientConfig.builder().build();

// Use JedisCluster instead
JedisCluster jedis = new JedisCluster(nodes, config);

// Or use the new builder pattern
JedisCluster jedis = JedisCluster.builder()
    .nodes(nodes)
    .clientConfig(config)
    .build();
```

### Return Type Changes

#### UnifiedJedis.pipelined()

The return type has been changed to be more generic:

**Before (v6.2.0):**
```java
PipelineBase pipelined()
```

**After (v7.0.0):**
```java
AbstractPipeline pipelined()
```

**Impact:** Minimal - `AbstractPipeline` is the parent class, so existing code should continue to work.

## New Features

### Automatic Failover and Failback

Jedis 7.0.0 significantly refactors the automatic failover and failback API. If you were using the failover features in v6.2.0 with `MultiClusterClientConfig` and `MultiClusterPooledConnectionProvider`, these have been renamed and improved in v7.0.0.

**For detailed migration guidance on automatic failover and failback** please refer to the **[Automatic Failover and Failback Migration Guide](https://redis.github.io/jedis/failover/#migration-from-6x-to-7x)**.

### Builder Pattern for Client Creation

Jedis 7.0.0 introduces a fluent builder pattern for creating client instances, making configuration more intuitive and discoverable.

#### JedisPooled Builder

**New in v7.0.0:**
```java
JedisPooled jedis = JedisPooled.builder()
    .hostAndPort("localhost", 6379)
    .clientConfig(DefaultJedisClientConfig.builder()
        .user("myuser")
        .password("mypassword")
        .database(0)
        .build())
    .poolConfig(new GenericObjectPoolConfig<>())
    .build();
```

#### JedisCluster Builder

**New in v7.0.0:**
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("localhost", 7000));
nodes.add(new HostAndPort("localhost", 7001));

JedisCluster cluster = JedisCluster.builder()
    .nodes(nodes)
    .clientConfig(DefaultJedisClientConfig.builder()
        .password("mypassword")
        .build())
    .maxAttempts(5)
    .maxTotalRetriesDuration(Duration.ofSeconds(10))
    .build();
```

#### JedisSentineled Builder

**New in v7.0.0:**
```java
Set<HostAndPort> sentinels = new HashSet<>();
sentinels.add(new HostAndPort("localhost", 26379));
sentinels.add(new HostAndPort("localhost", 26380));

JedisSentineled jedis = JedisSentineled.builder()
    .masterName("mymaster")
    .sentinels(sentinels)
    .clientConfig(DefaultJedisClientConfig.builder()
        .password("mypassword")
        .build())
    .build();
```

## Getting Help

If you encounter issues during migration create an issue or [start a discussion](https://github.com/redis/jedis/discussions/new?category=q-a).

