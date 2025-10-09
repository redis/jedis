# Jedis 6.0.0 Migration Guide

This guide helps you migrate from Jedis 5.x to Jedis 6.0.0. Version 6.0.0 includes breaking changes focused on Redis 8.0 support, removal of deprecated modules, and improvements to the Search API.

## Table of Contents

- [Overview](#overview)
- [Breaking Changes](#breaking-changes)
  - [Removed RedisGraph Support](#removed-redisgraph-support)
  - [Removed Triggers and Functions (RedisGears v2)](#removed-triggers-and-functions-redisgears-v2)
  - [Search Dialect Default Change](#search-dialect-default-change)
  - [FT.PROFILE Return Type Change](#ftprofile-return-type-change)
  - [COMMAND INFO Response Changes](#command-info-response-changes)
- [New Features](#new-features)
  - [Redis 8.0 Support](#redis-80-support)
  - [SslOptions for Advanced SSL Configuration](#ssloptions-for-advanced-ssl-configuration)
  - [Token-Based Authentication](#token-based-authentication)
  - [New Hash Commands](#new-hash-commands)
  - [Search Warning Messages](#search-warning-messages)
- [Additional Resources](#additional-resources)

## Overview

Jedis 6.0.0 is a major release that adds Redis 8.0 support and removes deprecated features. The main focus areas are:

1. **Redis 8.0 compatibility** - Full support for Redis 8.0 features including built-in JSON, Search, and TimeSeries
2. **Module deprecations** - Removal of RedisGraph and Triggers & Functions (RedisGears v2) support
3. **Search API improvements** - Default dialect change and enhanced profiling responses
4. **Security enhancements** - New SSL options and token-based authentication support

## Breaking Changes

### Removed RedisGraph Support

RedisGraph module support has been completely removed from Jedis 6.0.0 as the module has been deprecated by Redis.

#### Removed Classes and Interfaces

All classes in the `redis.clients.jedis.graph` package have been removed:

- `RedisGraphCommands` interface
- `RedisGraphPipelineCommands` interface
- `GraphCommandObjects` class
- `GraphCache`, `GraphProtocol`, `GraphQueryParams` classes
- `ResultSet`, `ResultSetBuilder`, `Record`, `Header`, `Statistics` classes
- All entity classes: `Edge`, `Node`, `Path`, `Point`, `Property`, `GraphEntity`

### Removed Triggers and Functions (RedisGears v2)

Support for Triggers and Functions (RedisGears v2) has been removed from Jedis 6.0.0.

#### Removed Classes and Interfaces

All classes in the `redis.clients.jedis.gears` package have been removed:

- `RedisGearsCommands` interface
- `RedisGearsProtocol` class
- `TFunctionListParams`, `TFunctionLoadParams` classes
- Response classes: `FunctionInfo`, `FunctionStreamInfo`, `GearsLibraryInfo`, `StreamTriggerInfo`, `TriggerInfo`

### Search Dialect Default Change

**BREAKING:** The default search dialect has changed from server-side default to **DIALECT 2** (client-side override).

#### Impact

Starting with Jedis 6.0.0, all `FT.SEARCH` and `FT.AGGREGATE` commands automatically append `DIALECT 2` unless explicitly configured otherwise. This may affect query results if you were relying on DIALECT 1 behavior.

#### Migration Path

**Option 1: Accept DIALECT 2 (Recommended)**

Review your search queries to ensure they work correctly with DIALECT 2. Most queries should work without changes.

**Option 2: Revert to DIALECT 1**

If you need to maintain DIALECT 1 behavior:

```java
JedisPooled jedis = new JedisPooled("redis://localhost:6379");

// Set default dialect to 1
jedis.setDefaultSearchDialect(1);

// Now all search commands will use DIALECT 1
SearchResult result = jedis.ftSearch("idx:products", "@category:electronics");
```

### FT.PROFILE Return Type Change

The return type of `FT.PROFILE` commands has changed from `Map<String, Object>` to a structured `ProfilingInfo` object.

#### Changed Methods

**Before (v5.x):**
```java
Map.Entry<SearchResult, Map<String, Object>> ftProfileSearch(
    String indexName, FTProfileParams profileParams, Query query);

Map.Entry<AggregationResult, Map<String, Object>> ftProfileAggregate(
    String indexName, FTProfileParams profileParams, AggregationBuilder aggr);
```

**After (v6.0.0):**
```java
Map.Entry<SearchResult, ProfilingInfo> ftProfileSearch(
    String indexName, FTProfileParams profileParams, Query query);

Map.Entry<AggregationResult, ProfilingInfo> ftProfileAggregate(
    String indexName, FTProfileParams profileParams, AggregationBuilder aggr);
```

### COMMAND INFO Response Changes

The response format for `COMMAND INFO` has been updated to include subcommand details, making it compatible with Redis 7.0+ and Redis 8.0.

#### Impact

If you were parsing the `COMMAND INFO` response, you may need to update your code to handle the new structure that includes subcommand information.

**Before (v5.x):**
```java
List<Object> commandInfo = jedis.commandInfo("SET");
// Returns basic command information
```

**After (v6.0.0):**
```java
List<Object> commandInfo = jedis.commandInfo("SET");
// Returns command information including subcommand details
// Compatible with Redis 7.0+ format
```

## New Features

### Redis 8.0 Support

Jedis 6.0.0 adds full support for Redis 8.0, which includes built-in support for:

- **JSON** - Native JSON data type (previously RedisJSON module)
- **Search and Query** - Full-text search capabilities (previously RediSearch module)
- **TimeSeries** - Time-series data support (previously RedisTimeSeries module)

**Example:**
```java
JedisPooled jedis = new JedisPooled("redis://localhost:6379");

// JSON operations (built-in in Redis 8.0)
jedis.jsonSet("user:1", Path2.of("$"), "{\"name\":\"John\",\"age\":30}");
String json = jedis.jsonGet("user:1");

// Search operations (built-in in Redis 8.0)
jedis.ftCreate("idx:users",
    FTCreateParams.createParams()
        .on(IndexDataType.JSON)
        .addPrefix("user:"),
    TextField.of("$.name").as("name"),
    NumericField.of("$.age").as("age"));

SearchResult result = jedis.ftSearch("idx:users", "@name:John");
```

### SslOptions for Advanced SSL Configuration

A new `SslOptions` class provides advanced SSL/TLS configuration options for secure connections.

**Features:**
- Custom keystore and truststore configuration
- SSL protocol selection
- SSL verification mode control
- SSL parameters customization

**Example:**
```java
SslOptions sslOptions = SslOptions.builder()
    .keystore(new File("/path/to/keystore.jks"))
    .keystorePassword("keystorePassword".toCharArray())
    .truststore(new File("/path/to/truststore.jks"))
    .truststorePassword("truststorePassword".toCharArray())
    .sslVerifyMode(SslVerifyMode.FULL)
    .build();

JedisClientConfig config = DefaultJedisClientConfig.builder()
    .ssl(true)
    .sslOptions(sslOptions)
    .build();

JedisPooled jedis = new JedisPooled("localhost", 6379, config);
```

### Token-Based Authentication

Jedis 6.0.0 introduces support for token-based authentication, useful for cloud environments and managed Redis services.

**Example:**
```java
// Token-based authentication with automatic token refresh
TokenCredentials tokenCredentials = new TokenCredentials("initial-token");

JedisClientConfig config = DefaultJedisClientConfig.builder()
    .credentials(tokenCredentials)
    .build();

JedisPooled jedis = new JedisPooled("localhost", 6379, config);

// Token can be updated dynamically
tokenCredentials.updateToken("new-token");
```

### New Hash Commands

Support for new hash field expiration commands introduced in Redis 7.4:

- `HGETDEL` - Get and delete a hash field
- `HGETEX` - Get a hash field with expiration options
- `HSETEX` - Set a hash field with expiration

**Example:**
```java
// HSETEX - Set field with expiration
jedis.hsetex("user:1", 3600, "session", "abc123");

// HGETEX - Get field and update expiration
String session = jedis.hgetex("user:1", "session", 
    HGetExParams.hgetExParams().ex(7200));

// HGETDEL - Get and delete field
String oldSession = jedis.hgetdel("user:1", "session");
```

### Search Warning Messages

Search and aggregation queries now support warning messages in results, helping identify potential issues with queries.

**Example:**
```java
SearchResult result = jedis.ftSearch("idx:products", "@name:laptop");

// Check for warnings
if (result.hasWarnings()) {
    List<String> warnings = result.getWarnings();
    warnings.forEach(warning -> 
        System.out.println("Search warning: " + warning));
}
```

## Additional Resources

- [Redis 8.0 Release Notes](https://github.com/redis/redis/blob/8.0/00-RELEASENOTES)
- [Redis Search Documentation](https://redis.io/docs/interact/search-and-query/)
- [Redis Query Dialects](https://redis.io/docs/interact/search-and-query/advanced-concepts/dialects/)

## Getting Help

If you encounter issues during migration:

1. Check the [Jedis GitHub Issues](https://github.com/redis/jedis/issues)
2. Join the [Redis Discord](https://discord.gg/redis)
3. Review the [Jedis Javadocs](https://www.javadoc.io/doc/redis.clients/jedis/latest/)
4. Start a [Discussion](https://github.com/redis/jedis/discussions)

