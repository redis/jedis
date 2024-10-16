# Getting Started

## Installing Jedis  
In order to have Jedis as a dependency in your application you can:
### Use the jar files
Download the latest [Jedis](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22redis.clients%22%20AND%20a%3A%22jedis%22) and [Apache Commons Pool2](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.apache.commons%22%20AND%20a%3A%22commons-pool2%22) jars from [search.maven.org](http://search.maven.org) or any other maven repository.

### Build from source  
This gives you the most recent version. 
#### Clone the GitHub project. 
That is very easy, on the command line you just need to:
```git clone git://github.com/xetorthio/jedis.git```
#### Build from GitHub  
Before you package it using maven, you have to pass the tests.
To run the tests and package, run ```make package```.
 
### Configure a Maven dependency  
Jedis is also distributed as a Maven Dependency through Sonatype. To configure that just add the following XML snippet to your pom.xml file.

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>2.9.0</version>
    <type>jar</type>
    <scope>compile</scope>
</dependency>
```
## Basic usage example  
### using Jedis in a multithreaded environment  
You shouldn't use the same instance from different threads because you'll have strange errors. 
And sometimes creating lots of Jedis instances is not good enough because it means lots of sockets and connections, which leads to strange errors as well. 
```A single Jedis instance is not threadsafe!```
To avoid these problems, you should use JedisPool, which is a threadsafe pool of network connections. 
You can use the pool to reliably create several Jedis instances, given you return the Jedis instance to the pool when done. This way you can overcome those strange errors and achieve great performance.

To use it, init a pool:
```java
JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
```

You can store the pool somewhere statically, it is thread-safe. 

JedisPoolConfig includes a number of helpful Redis-specific connection pooling defaults. JedisPool is based on Commons Pool 2, so you may want to have a look at Commons Pool's configuration. Please see http://commons.apache.org/proper/commons-pool/apidocs/org/apache/commons/pool2/impl/GenericObjectPoolConfig.html for more details.

You use it by:

```java
/// Jedis implements Closeable. Hence, the jedis instance will be auto-closed after the last statement.
try (Jedis jedis = pool.getResource()) {
  /// ... do stuff here ... for example
  jedis.set("foo", "bar");
  String foobar = jedis.get("foo");
  jedis.zadd("sose", 0, "car"); jedis.zadd("sose", 0, "bike"); 
  Set<String> sose = jedis.zrange("sose", 0, -1);
}
/// ... when closing your application:
pool.close();
```

If you can't use try-with-resource, you can still enjoy with Jedis.close().

```java
Jedis jedis = null;
try {
  jedis = pool.getResource();
  /// ... do stuff here ... for example
  jedis.set("foo", "bar");
  String foobar = jedis.get("foo");
  jedis.zadd("sose", 0, "car"); jedis.zadd("sose", 0, "bike"); 
  Set<String> sose = jedis.zrange("sose", 0, -1);
} finally {
  // You have to close jedis object. If you don't close then
  // it doesn't release back to pool and you can't get a new
  // resource from pool.
  if (jedis != null) {
    jedis.close();
  }
}
/// ... when closing your application:
pool.close();
```

If Jedis was borrowed from pool, it will be returned to pool with proper method since it already determines there was JedisConnectionException occurred.
If Jedis wasn't borrowed from pool, it will be disconnected and closed.

### Setting up master/slave distribution  
#### enable replication  
Redis is primarily built for master/slave distribution. This means that write requests have to be explicitly addressed to the master (a redis server), which replicates changes to slaves (which are also redis servers). Read requests then can be (but must not necessarily) addressed to the slaves, which alleviates the master.

You use the master as shown above. In order to enable replication, there are two ways to tell a slave it will be "slaveOf" a given master: 

* Specify it in the respective section in the Redis Config file of the redis server

* on a given jedis instance (see above), call the slaveOf method and pass IP (or "localhost") and port as argument:

```java
jedis.slaveof("localhost", 6379);  //  if the master is on the same PC which runs your code
jedis.slaveof("192.168.1.35", 6379); 
```

Note: since Redis 2.6 slaves are read only by default, so write requests to them will result in an error.

If you change that setting they will behave like normal redis servers and accept write requests without errors, but the changes won't be replicated, and hence those changes are at risk to be silently overwritten, if you mix up your jedis instances.

#### disable replication / upon failing master, promote a slave  

In case your master goes down, you may want to promote a slave to be the new master. You should first (try to) disable replication of the offline master first, then, in case you have several slaves, enable replication of the remaining slaves to the new master:

```java
slave1jedis.slaveofNoOne();
slave2jedis.slaveof("192.168.1.36", 6379); 
```