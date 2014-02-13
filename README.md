[![Build Status](https://travis-ci.org/xetorthio/jedis.png?branch=master)](https://travis-ci.org/xetorthio/jedis)

# Jedis

Jedis is a blazingly small and sane [Redis](http://github.com/antirez/redis "Redis") java client.

Jedis was conceived to be EASY to use.

Jedis is fully compatible with redis 2.8.5.

## Community

Meet us on IRC: ##jedis on freenode.net

Join the mailing-list at [http://groups.google.com/group/jedis_redis](http://groups.google.com/group/jedis_redis)

## So what can I do with Jedis?
All of the following redis features are supported:

- Sorting
- Connection handling
- Commands operating on any kind of values
- Commands operating on string values
- Commands operating on hashes
- Commands operating on lists
- Commands operating on sets
- Commands operating on sorted sets
- Transactions
- Pipelining
- Publish/Subscribe
- Persistence control commands
- Remote server control commands
- Connection pooling
- Sharding (MD5, MurmurHash)
- Key-tags for sharding
- Sharding with pipelining
- Scripting with pipelining
- Redis Cluster

## How do I use it?

You can download the latest build at: 
    http://github.com/xetorthio/jedis/downloads

Or use it as a maven dependency:

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>2.2.1</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
```

To use it just:
    
```java
Jedis jedis = new Jedis("localhost");
jedis.set("foo", "bar");
String value = jedis.get("foo");
```

For more usage examples check the tests.

Please check the [wiki](http://github.com/xetorthio/jedis/wiki "wiki"). There are lots of cool things you should know, including information about connection pooling.

And you are done!

## Jedis Cluster

Redis cluster [specification](http://redis.io/topics/cluster-spec) (still under development) is implemented

```java
Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
//Jedis Cluster will attempt to discover cluster nodes automatically
jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7379));
JedisCluster jc = new JedisCluster(jedisClusterNode);
jc.set("foo", "bar");
String value = jc.get("foo");
```

## I want to contribute!

That is great! Just fork the project in github. Create a topic branch, write some code, and add some tests for your new code.

To run the tests:

- Use the latest redis master branch.

- Run ```make test```. This will run 2 instances of redis. We use 2 redis
	servers, one on default port (6379) and the other one on (6380). Both have
	authentication enabled with default password (foobared). This way we can
	test both sharding and auth command. For the Sentinel tests to we use a
	default Sentinel configuration that is configured to properly authenticate
	using the same password with a master called mymaster running on 6379.

Thanks for helping!

## License

Copyright (c) 2011 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

