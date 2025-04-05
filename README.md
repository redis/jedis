# Jedis

[![Release](https://img.shields.io/github/release/redis/jedis.svg?sort=semver)](https://github.com/redis/jedis/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/redis.clients/jedis.svg)](https://search.maven.org/artifact/redis.clients/jedis)
[![Javadocs](https://www.javadoc.io/badge/redis.clients/jedis.svg)](https://www.javadoc.io/doc/redis.clients/jedis)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE.txt)
[![Integration](https://github.com/redis/jedis/actions/workflows/integration.yml/badge.svg?branch=master)](https://github.com/redis/jedis/actions/workflows/integration.yml)
[![codecov](https://codecov.io/gh/redis/jedis/branch/master/graph/badge.svg?token=pAstxAAjYo)](https://codecov.io/gh/redis/jedis)
[![Discord](https://img.shields.io/discord/697882427875393627?style=flat-square)](https://discord.gg/redis)

## What is Jedis?

Jedis is a Java client for [Redis](https://github.com/redis/redis "Redis") designed for performance and ease of use.

Are you looking for a high-level library to handle object mapping? See [redis-om-spring](https://github.com/redis/redis-om-spring)!

## How do I Redis?

[Learn for free at Redis University](https://university.redis.io/academy/)

[Try the Redis Cloud](https://redis.io/try-free/)

[Dive in developer tutorials](https://redis.io/learn/)

[Join the Redis community](https://redis.io/community/)

[Work at Redis](https://redis.io/careers/jobs/)

## Supported Redis versions

The most recent version of this library supports redis version 
[5.0](https://github.com/redis/redis/blob/5.0/00-RELEASENOTES), 
[6.0](https://github.com/redis/redis/blob/6.0/00-RELEASENOTES), 
[6.2](https://github.com/redis/redis/blob/6.2/00-RELEASENOTES), 
[7.0](https://github.com/redis/redis/blob/7.0/00-RELEASENOTES), 
[7.2](https://github.com/redis/redis/blob/7.2/00-RELEASENOTES) and 
[7.4](https://github.com/redis/redis/blob/7.4/00-RELEASENOTES).

The table below highlights version compatibility of the most-recent library versions and Redis versions. Compatibility means communication features, and Redis command capabilities.


| Jedis version | Supported Redis versions              | JDK Compatibility |
|---------------|---------------------------------------|-------------------|
| 3.9+          | 5.0 to 6.2 Family of releases         | 8, 11             |
| >= 4.0        | Version 5.0 to 7.2 Family of releases | 8, 11, 17         |
| >= 5.0        | Version 6.0 to current                | 8, 11, 17, 21     |
| >= 5.2        | Version 7.2 to current                | 8, 11, 17, 21     |

## Getting started

To get started with Jedis, first add it as a dependency in your Java project. If you're using Maven, that looks like this:

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.2.0</version>
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

Using a *try-with-resources* block for each command may be cumbersome, so you may consider using JedisPooled.

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

## Using Redis modules

Jedis includes support for [Redis modules](https://redis.io/docs/modules/) such as
[RedisJSON](https://redis.io/json/) and [RediSearch](https://redis.io/search/).

See the [RedisJSON Jedis](docs/redisjson.md) or [RediSearch Jedis](docs/redisearch.md) for details.

## Failover

Jedis supports retry and failover for your Redis deployments. This is useful when:

1. You have more than one Redis deployment. This might include two independent Redis servers or two or more Redis databases replicated across multiple [active-active Redis Enterprise](https://redis.io/docs/latest/operate/rs/databases/active-active/) clusters.
2. You want your application to connect to one deployment at a time and to fail over to the next available deployment if the first deployment becomes unavailable.

For the complete failover configuration options and examples, see the [Jedis failover docs](docs/failover.md).

## Token-Based Authentication

Jedis supports Token-Based authentication (TBA) starting with 5.3.0 GA release. This feature is complemented by an extension library that enhances the developer experience and provides most of the components required for TBA functionality.

Notably, the extension library includes built-in support for **Microsoft EntraID**, offering a seamless integration as part of the generic solution.

For more details and examples, please refer to the [Advanced Usage](docs/advanced-usage.md) documentation.

## Documentation

The [Jedis wiki](http://github.com/redis/jedis/wiki) contains several useful articles for using Jedis.

You can also check the [latest Jedis Javadocs](https://www.javadoc.io/doc/redis.clients/jedis/latest/index.html).

Some specific use-case examples can be found in [`redis.clients.jedis.examples`
package](src/test/java/redis/clients/jedis/examples/) of the test source codes.

## Troubleshooting

If you run into trouble or have any questions, we're here to help!

Hit us up on the [Redis Discord Server](http://discord.gg/redis) or 
[Jedis GitHub Discussions](https://github.com/redis/jedis/discussions) or 
[Jedis mailing list](http://groups.google.com/group/jedis_redis).

## Contributing

We'd love your contributions!

Bug reports are always welcome! [You can open a bug report on GitHub](https://github.com/redis/jedis/issues/new).

You can also contribute documentation -- or anything to improve Jedis. Please see
[contribution guideline](https://github.com/redis/jedis/blob/master/.github/CONTRIBUTING.md) for more details.

## License

Jedis is licensed under the [MIT license](https://github.com/redis/jedis/blob/master/LICENSE).

## Sponsorship

[![Redis Logo](redis-logo-full-color-rgb.png)](https://redis.io/)
