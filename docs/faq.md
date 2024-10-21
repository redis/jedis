# Frequently Asked Questions

## If you get `java.net.SocketTimeoutException: Read timed out` exception

Try setting own `timeout` value when constructing `JedisPool` using the following constructor:
```java
JedisPool(GenericObjectPoolConfig poolConfig, String host, int port, int timeout)
```
where `timeout` is given as milliseconds.

Default `timeout` value is **2 seconds**.

## JedisPool blocks after getting 8 connections

JedisPool defaults to 8 connections, you can change this in the PoolConfig:

```java
JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxTotal(maxTotal); // maximum active connections
poolConfig.setMaxIdle(maxIdle);  // maximum idle connections
```

Take into account that `JedisPool` inherits commons-pool [BaseObjectPoolConfig](https://commons.apache.org/proper/commons-pool/api-2.3/org/apache/commons/pool2/impl/BaseObjectPoolConfig.html) which has a lot of configuration parameters. 
We've set some defined ones which suit most of the cases. In case, you experience [issues](https://github.com/xetorthio/jedis/issues?utf8=%E2%9C%93&q=is%3Aissue+is%3Aopen+JedisPool) tuning these parameters may help.

## How to configure the buffer size of socket(s)

The buffer size of all Jedis sockets in an application can be configured through system property.

Buffer size of input stream can be configured by setting `jedis.bufferSize.input` or `jedis.bufferSize` system property.  
Buffer size of output stream can be configured by setting `jedis.bufferSize.output` or `jedis.bufferSize` system property.  
If you want to set the buffer size of both input and output stream to same value, you can just set `jedis.bufferSize`.

Note: This feature is available since Jedis 4.2.0.

## How to avoid cluster initialization error

As of Jedis 4.0.0, a `JedisClusterOperationException` is raised with the message `Could not initialize cluster slots cache.` when the cluster initialization process fails. 

Should you would want to avoid this error (for example, creating `JedisConnectionFactory` to an unavailable cluster for a spring-data-redis `Bean`), set the system property `jedis.cluster.initNoError` to any value.  
In the console, add the option `-Djedis.cluster.initNoError`.
In an application, `System.setProperty("jedis.cluster.initNoError", "");` can be set before creating any cluster object.

Note: This feature is available since Jedis 4.4.2.