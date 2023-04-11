# Jedis

[![Release](https://img.shields.io/github/release/redis/jedis.svg?sort=semver)](https://github.com/redis/jedis/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/redis.clients/jedis.svg)](https://search.maven.org/artifact/redis.clients/jedis)
[![Javadocs](https://www.javadoc.io/badge/redis.clients/jedis.svg)](https://www.javadoc.io/doc/redis.clients/jedis)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE.txt)
[![Integration](https://github.com/redis/jedis/actions/workflows/integration.yml/badge.svg?branch=master)](https://github.com/redis/jedis/actions/workflows/integration.yml)
[![codecov](https://codecov.io/gh/redis/jedis/branch/master/graph/badge.svg?token=pAstxAAjYo)](https://codecov.io/gh/redis/jedis)
[![Discord](https://img.shields.io/discord/697882427875393627?style=flat-square)](https://discord.gg/qRhBuY8Z)

## What is Jedis?

Jedis is a Java client for [Redis](https://github.com/redis/redis "Redis") designed for performance and ease of use.

Are you looking for a high-level library to handle object mapping? See [redis-om-spring](https://github.com/redis/redis-om-spring)!

## Contributing

We'd love your contributions!

**Bug reports** are always welcome! [You can open a bug report on GitHub](https://github.com/redis/jedis/issues/new).

You can also **contribute documentation** -- or anything to improve Jedis. Please see
[contribution guideline](https://github.com/redis/jedis/blob/master/.github/CONTRIBUTING.md) for more details.

## Getting started

To get started with Jedis, first add it as a dependency in your Java project. If you're using Maven, that looks like this:

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.3.0</version>
</dependency>
```

To use the cutting-edge Jedis, check [here](/docs/jedis-maven.md).

Next, you'll need to connect to Redis. Consider installing a redis-stack docker:

```bash
docker run -p 6379:6379 -it redis/redis-stack:latest
```

For many applications, it's best to use a connection pool. You can instantiate a Jedis connection pool like so:

```java
JedisPool pool = new JedisPool("localhost", 6379);
```

With a `JedisPool` instance, you can use a
[try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
block to get a connection and run Redis commands.

Here's how to run a single [SET](https://redis.io/commands/set) command within a *try-with-resources* block:

```java
try (Jedis jedis = pool.getResource()) {
  jedis.set("clientName", "Jedis");
}
```

`Jedis` instances implement most Redis commands. See the
[Jedis Javadocs](https://www.javadoc.io/doc/redis.clients/jedis/latest/redis/clients/jedis/Jedis.html)
for the complete list of supported commands.

### Easier way of using connection pool

Using a *try-with-resources* block for each command may be cumbursome, so you may consider using JedisPooled.

```java
JedisPooled jedis = new JedisPooled("localhost", 6379);
```

Now you can send commands like sending from Jedis.

```java
jedis.sadd("planets", "Venus");
```

## Connecting to a Redis cluster

Jedis lets you connect to Redis Clusters, supporting the [Redis Cluster Specification](https://redis.io/topics/cluster-spec).
To do this, you'll need to connect using `JedisCluster`. See the example below:

```java
Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7379));
jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7380));
JedisCluster jedis = new JedisCluster(jedisClusterNodes);
```

Now you can use the `JedisCluster` instance and send commands like you would with a standard pooled connection:

```java
jedis.sadd("planets", "Mars");
```

## Failover

Jedis supports retry and failover for your Redis deployments. This is useful when:
1. You have more than one Redis deployment. This might include two independent Redis servers or two or more Redis databases replicated across multiple [active-active Redis Enterprise](https://docs.redis.com/latest/rs/databases/active-active/) clusters.
2. You want your application to connect to one deployment at a time and to fail over to the next available deployment if the first deployment becomes unavailable.

### Configuring failover

To configure Jedis for failover, you specify an ordered list of Redis databases to connect to.
By default, Jedis will connect to the first Redis database in the list. If the first database becomes unavailable,
Jedis will attempt to connect to the next database in the list, and so on.

What follows here is a simple configuration example. For more details, see the [complete Jedis failover docs](docs/failover.md).

First create an array of `ClusterJedisClientConfig` objects, one for each Redis database.

```java
JedisClientConfig config = DefaultJedisClientConfig.builder().user("cache").password("secret").build();

ClusterJedisClientConfig[] clientConfigs = new ClusterJedisClientConfig[2];
clientConfigs[0] = new ClusterJedisClientConfig(new HostAndPort("redis-east.example.com", 14000), config);
clientConfigs[1] = new ClusterJedisClientConfig(new HostAndPort("redis-west.example.com", 14000), config);
```

The configuration above represents two example Redis databases: `redis-east.example.com` and `redis-west.example.com`.
(Note: If you're running Redis Enterprise, it's important that these two databases reside in separate clusters.)

You can use these configuration objects to create and configure a connection provider that supports failover:

```java
MultiClusterJedisClientConfig.Builder builder = new MultiClusterJedisClientConfig.Builder(clientConfigs);
builder.circuitBreakerSlidingWindowSize(10);
builder.circuitBreakerSlidingWindowMinCalls(1);
builder.circuitBreakerFailureRateThreshold(50.0f);

MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(builder.build());
```

Internally, the connection provider uses a [configurable circuit breaker and retry implementation](https://resilience4j.readme.io/docs/circuitbreaker) to determine when to fail over.
In this configuration, we've set a sliding window size of 10 and a failure rate threshold of 50%. This means that a failover will be triggered if 5 out of any 10 calls to Redis fail.

Once you've configured and created a `MultiClusterPooledConnectionProvider`, instantiate a `UnifiedJedis` instance for your application, passing in the provider you just created:

```java
UnifiedJedis jedis = new UnifiedJedis(provider);
```

When you use this `UnifiedJedis` instance to communicate with Redis, your application will be able to fail over automatically, when necessary.
In this example, if `redis-east.example.com` becomes unavailable, the application will then connect to and use `redis-west.example.com`.

We recommend testing your specific failover scenarios to ensure that this behavior meets your application's requirements.
For more configuration options and examples, see the complete [Jedis failover docs](docs/failover.md).

## Documentation

The [Jedis wiki](http://github.com/redis/jedis/wiki) contains several useful articles for using Jedis.

You can also check the [latest Jedis Javadocs](https://www.javadoc.io/doc/redis.clients/jedis/latest/index.html).

Some specific use-case examples can be found in [`redis.clients.jedis.examples` 
package](src/test/java/redis/clients/jedis/examples/) of the test source codes.

## Using Redis modules

Jedis includes support for [Redis modules](https://redis.io/docs/modules/) such as
[RedisJSON](https://oss.redis.com/redisjson/) and [RediSearch](https://oss.redis.com/redisearch/).

See the [RedisJSON Jedis](docs/redisjson.md) or [RediSearch Jedis](docs/redisearch.md) for details.

## Troubleshooting

If you run into trouble or have any questions, we're here to help!

Hit us up on the [Redis Discord Server](http://discord.gg/redis) or [open an issue on GitHub](https://github.com/redis/jedis).

You can also find help on the [Jedis mailing list](http://groups.google.com/group/jedis_redis) or the
[GitHub Discussions](https://github.com/redis/jedis/discussions).

## License

Jedis is licensed under the [MIT license](https://github.com/redis/jedis/blob/master/LICENSE.txt).

## Sponsorship

[![Redis Logo](redis-logo-full-color-rgb.png)](https://redis.com/)
