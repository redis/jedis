# Jedis

[![Release](https://img.shields.io/github/release/redis/jedis.svg?sort=semver)](https://github.com/redis/jedis/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/redis.clients/jedis.svg)](https://search.maven.org/artifact/redis.clients/jedis)
[![Javadocs](https://www.javadoc.io/badge/redis.clients/jedis.svg)](https://www.javadoc.io/doc/redis.clients/jedis)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE.txt)
[![Integration](https://github.com/redis/jedis/actions/workflows/integration.yml/badge.svg?branch=master)](https://github.com/redis/jedis/actions/workflows/integration.yml)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/redis/jedis.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/redis/jedis/context:java)
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

Next, you'll need to connect to Redis. For many applications, it's best to use a connection pool. You can instantiate a Jedis connection pool like so:

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

## Using Redis modules

Jedis includes support for [Redis modules](https://redis.io/docs/modules/) such as
[RedisJSON](https://oss.redis.com/redisjson/) and [RediSearch](https://oss.redis.com/redisearch/).

See the [RedisJSON Jedis](docs/redisjson.md) or [RediSearch Jedis](docs/redisearch.md) for details.

## Documentation

The [Jedis wiki](http://github.com/redis/jedis/wiki) contains several useful articles for using Jedis.

You can also check the [latest Jedis Javadocs](https://www.javadoc.io/doc/redis.clients/jedis/latest/index.html).

## Troubleshooting

If you run into trouble or have any questions, we're here to help!

Hit us up on the [Redis Discord Server](http://discord.gg/redis) or [open an issue on GitHub](https://github.com/redis/jedis).

You can also find help on the [Jedis mailing list](http://groups.google.com/group/jedis_redis) or the
[GitHub Discussions](https://github.com/redis/jedis/discussions).

## License

Jedis is licensed under the [MIT license](https://github.com/redis/jedis/blob/master/LICENSE.txt).

## Sponsorship

[![Redis Logo](redis-logo-full-color-rgb.png)](https://redis.com/)
